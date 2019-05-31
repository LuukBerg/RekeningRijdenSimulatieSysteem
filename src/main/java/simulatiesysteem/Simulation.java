/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package simulatiesysteem;

import com.rabbitmq.client.AMQP;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import simulatiesysteem.jms.Serializer;
import simulatiesysteem.jms.gateway.MessageSenderGateway;
import simulatiesysteem.json.RootObject;
import simulatiesysteem.json.Route;
import simulatiesysteem.json.Step;

/**
 *
 * @author Ken
 */
public class Simulation {

    private final MessageSenderGateway sender;
    private final AMQP.BasicProperties props;
    private final String trackerId;

    private List<List<Double>> coordinates;
    private boolean initialized;
    private int cursor;

    public Simulation(String trackerId, MessageSenderGateway sender, AMQP.BasicProperties props) {
        this.sender = sender;
        this.props = props;
        this.trackerId = trackerId;
    }

    public String getTrackerId() {
        return trackerId;
    }

    public void initialize(RootObject obj) {
        List<Route> routes = obj.getRoutes();

        //TODO - Error handling voor lege collecties en dergelijke.
        coordinates = routes.get(0).getGeometry().getCoordinates();
        initialized = true;
    }

    public boolean step() {
        if (!initialized || cursor == coordinates.size()) {
            return false;
        }

        List<Double> point = coordinates.get(cursor++);
        double lat = point.get(0);
        double lon = point.get(1);

        String output = String.format("%s: Stepped to [%7.6f, %7.6f].", trackerId, lat, lon);
        System.out.println(output);

        try {
            Date date = new Date();

            Step step = new Step();
            step.setId(0);
            step.setTimestamp(date);
            step.setX(lon);
            step.setY(lat);
            step.setTrackerId(trackerId);

            sender.SendMessage(Serializer.serialize(step), props);
        } catch (IOException ex) {
            Logger.getLogger(Simulation.class.getName()).log(Level.SEVERE, null, ex);
        }

        return true;
    }

}
