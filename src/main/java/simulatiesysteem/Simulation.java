/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package simulatiesysteem;

import java.util.List;
import simulatiesysteem.json.RootObject;
import simulatiesysteem.json.Route;

/**
 *
 * @author Ken
 */
public class Simulation {

    private final String trackerId;
    private final List<List<Double>> coordinates;
    private int cursor;

    public Simulation(String trackerId, RootObject obj) {
        this.trackerId = trackerId;
        List<Route> routes = obj.getRoutes();
        
        //TODO - Error handling voor lege collecties en dergelijke.
        coordinates = routes.get(0).getGeometry().getCoordinates();
    }

    public boolean step() {
        if(cursor == coordinates.size())
            return false;
        
        List<Double> point = coordinates.get(cursor++);
        double lat = point.get(0);
        double lon = point.get(1);
        
        String output = String.format("%s: Stepped to [%7.6f, %7.6f].", trackerId, lat, lon);
        System.out.println(output);
        return true;
    }

}
