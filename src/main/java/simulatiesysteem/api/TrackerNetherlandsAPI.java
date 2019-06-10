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
public final class TrackerNetherlandsAPI extends TrackerBaseAPI {
    
    private static final String URL = "http://192.168.24.194:8085/registrationBackend/api/europe/cartrackers";
    
    public TrackerNetherlandsAPI(){
        super();
    }
    
    @Override
    protected String getUrl() {
        return URL;
    }
    
}
