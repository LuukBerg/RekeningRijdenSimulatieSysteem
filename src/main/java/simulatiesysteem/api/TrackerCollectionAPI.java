/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package simulatiesysteem.api;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author Ken
 */
public final class TrackerCollectionAPI extends TrackerBaseAPI {

    private final Set<TrackerBaseAPI> apis;
    
    public TrackerCollectionAPI(){
        this.apis = new HashSet<>();
        this.apis.add(new TrackerBelgiumAPI());
        this.apis.add(new TrackerNetherlandsAPI());
    }
    
    @Override
    public Set<String> getTrackers() {
        Set<String> trackers = new HashSet<>();
        for(TrackerBaseAPI api : apis){
            Set<String> t = api.getTrackers();
            trackers.addAll(t);
        }
        return Collections.unmodifiableSet(trackers);
    }

    @Override
    protected String getUrl() {
        return "";
    }
    
}
