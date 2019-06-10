/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package simulatiesysteem.api;

/**
 *
 * @author Ken
 */
public final class TrackerBelgiumAPI extends TrackerBaseAPI {

    private static final String URL = "http://192.168.24.160:8080/api/eu/trackers?offset=0&limit=10";

    public TrackerBelgiumAPI() {
        super();
    }

    @Override
    protected String getUrl() {
        return URL;
    }

}
