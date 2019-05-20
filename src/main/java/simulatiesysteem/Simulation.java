/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package simulatiesysteem;

import com.owlike.genson.Genson;
import com.rabbitmq.client.AMQP;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import simulatiesysteem.jms.gateway.MessageSenderGateway;
import simulatiesysteem.json.RootObject;
import simulatiesysteem.json.Route;
import simulatiesysteem.json.Step;

/**
 *
 * @author Ken
 */
public class Simulation {

    private MessageSenderGateway sender;
    private final String trackerId;
    private final List<List<Double>> coordinates;
    private int cursor;

    public Simulation(String trackerId, RootObject obj, MessageSenderGateway sender) {
        this.sender = sender;
        this.trackerId = trackerId;
        List<Route> routes = obj.getRoutes();

        //TODO - Error handling voor lege collecties en dergelijke.
        coordinates = routes.get(0).getGeometry().getCoordinates();
    }

    public boolean step() {
        if (cursor == coordinates.size()) {
            return false;
        }

        List<Double> point = coordinates.get(cursor++);
        double lat = point.get(0);
        double lon = point.get(1);

        String output = String.format("%s: Stepped to [%7.6f, %7.6f].", trackerId, lat, lon);
        System.out.println(output);

        try {
            AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
                    .correlationId("")
                    .replyTo("")
                    .build();

            Date date = new Date();

            Step step = new Step();
            step.setId(0);
            step.setTimestamp(date);
            step.setX(lon);
            step.setY(lat);
            step.setTrackerId(trackerId);

            sender.SendMessage(new Genson().serialize(step), props);
        } catch (IOException ex) {
            Logger.getLogger(Simulation.class.getName()).log(Level.SEVERE, null, ex);
        }

        return true;
    }

}
