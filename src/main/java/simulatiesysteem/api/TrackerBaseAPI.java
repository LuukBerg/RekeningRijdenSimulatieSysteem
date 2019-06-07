/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package simulatiesysteem.api;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Ken
 */
public abstract class TrackerBaseAPI {
    
    private final Gson gson;
    
    public TrackerBaseAPI(){
        gson = new Gson();
    }
    
    public Set<String> getTrackers(){
        String url = getUrl();
        String response = fetchTrackers(url);
        
        if("".equals(response))
            return null;

        Type setType = new TypeToken<Set<TrackerJson>>() {}.getType();
        Set<TrackerJson> rawTrackers = gson.fromJson(response, setType);
        Set<String> trackers = new HashSet<>();
        for(TrackerJson tracker : rawTrackers){
            String trackerId = tracker.getTrackerId();
            trackers.add(trackerId);
        }
        return Collections.unmodifiableSet(trackers);
    }

    protected String fetchTrackers(String urlStr) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection urlCon = (HttpURLConnection) url.openConnection();
            urlCon.setRequestMethod("GET");
            urlCon.setDoOutput(true);
            urlCon.setReadTimeout(15 * 1000);

            return getResponse(urlCon);
        } catch (IOException ex) {
            Logger.getLogger(TrackerBaseAPI.class.getName()).log(Level.SEVERE, null, ex);
            return "";
        }
    }

    private String getResponse(HttpURLConnection urlCon) {
        try ( BufferedReader reader = new BufferedReader(new InputStreamReader(urlCon.getInputStream()))) {
            StringBuilder stringBuilder = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append('\n');
            }
            return stringBuilder.toString();
        } catch (IOException ex) {
            Logger.getLogger(TrackerBaseAPI.class.getName()).log(Level.SEVERE, null, ex);
            return "";
        }
    }

    protected abstract String getUrl();

}
