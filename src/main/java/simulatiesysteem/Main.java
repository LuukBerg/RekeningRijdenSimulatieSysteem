/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package simulatiesysteem;

import com.google.gson.Gson;
import com.rabbitmq.client.AMQP;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import simulatiesysteem.jms.gateway.MessageSenderGateway;
import simulatiesysteem.json.RootObject;

/**
 *
 * @author Ken
 */
public class Main {

    private static final String BASE_URL = "http://192.168.24.14:5000/route/v1/driving/%7.6f,%7.6f;%7.6f,%7.6f?overview=full&geometries=geojson";
    private static final double START_LAT = -0.5;
    private static final double START_LONG = 44;
    private static final double END_LAT = 3;
    private static final double END_LONG = 48;
    private static final int STEP_TIME = 10;
    
    private final String[] trackers;
    private final Gson gson;
    private final Random random;
    private final MessageSenderGateway sender;
    private final AMQP.BasicProperties props;
    
    private Set<Simulation> simulations;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        if (args.length <= 1) {
            System.out.println("Invalid arguments specified.");
            return;
        }

        String argument = args[0];
        if (argument.equals("-v") || argument.equals("--vehicles")) {
            argument = args[1];
            int amount = Integer.parseInt(argument);
            if (amount <= 0) {
                System.out.println("An invalid amount of vehicles was specified.");
            } else {
                new Main().run(amount);
            }
        } else {
            System.out.println("No vehicle amount specified.");
        }
    }

    private Main()
    {
        this.gson = new Gson();
        this.trackers = new String[500];

        // NOTE: Same buffer size and seed as in the administratie test data.
        byte[] buffer = new byte[10];
        Random random = new Random(665198248186247L);
        for (int i = 0; i < 500; i += 1) {
            random.nextBytes(buffer);
            String uuid = "FR_" + UUID.nameUUIDFromBytes(buffer).toString();
            this.trackers[i] = uuid;
        }

        this.random = new Random();
        this.sender = new MessageSenderGateway("StepChannel");
        this.props = new AMQP.BasicProperties.Builder()
                .correlationId("")
                .replyTo("")
                .build();
    }

    private void run(int vehicles) {
        if (vehicles > trackers.length) {
            vehicles = trackers.length;
        }

        simulations = new HashSet<>(vehicles);
        for (int i = 0; i < vehicles; i++) {
            String trackerId = trackers[i];
            Simulation simulation = new Simulation(trackerId, sender, props);
            simulations.add(simulation);
        }
        
        simulations = Collections.synchronizedSet(simulations);
        
        simulations.parallelStream().forEach((simulation) -> {
            double startLat = randomDouble(START_LAT, END_LAT);
            double startLong = randomDouble(START_LONG, END_LONG);
            double endLat = randomDouble(START_LAT, END_LAT);
            double endLong = randomDouble(START_LONG, END_LONG);
            RootObject obj = fetchRoutes(startLat, startLong, endLat, endLong);
            if (obj == null) {
                System.out.println("Failed to retrieve routes.");
            } else{
                String message = String.format("%s: Fetching routes.", simulation.getTrackerId());
                System.out.println(message);
            }
            simulation.initialize(obj);
        });

        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (simulations.isEmpty()) {
                    timer.cancel();
                    System.out.println("Simulation complete.");
                }

                simulations.forEach((simulation) -> {
                    if (!simulation.step()) {
                        simulations.remove(simulation);
                    }
                });
            }
        }, 0, STEP_TIME * 1000);
    }
    
    private double randomDouble(double min, double max){
        return min + (max - min) * random.nextDouble();
    }

    private RootObject fetchRoutes(double startLat, double startLong, double endLat, double endLong) {
        try {
            String urlStr = String.format(Locale.ROOT, BASE_URL, startLat, startLong, endLat, endLong);

            URL url = new URL(urlStr);
            HttpURLConnection urlCon = (HttpURLConnection) url.openConnection();
            urlCon.setRequestMethod("GET");
            urlCon.setDoOutput(true);
            urlCon.setReadTimeout(15 * 1000);

            String output = getResponse(urlCon);
            RootObject obj = gson.fromJson(output, RootObject.class);
            return obj;
        } catch (IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    private String getResponse(HttpURLConnection urlCon) throws IOException {
        urlCon.connect();

        try ( BufferedReader reader = new BufferedReader(new InputStreamReader(urlCon.getInputStream()))) {
            StringBuilder stringBuilder = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append('\n');
            }
            return stringBuilder.toString();
        }
    }

}
