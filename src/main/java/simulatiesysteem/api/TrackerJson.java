package simulatiesysteem.api;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class TrackerJson {

    @SerializedName("trackerId")
    @Expose
    private String trackerId;

    public String getTrackerId() {
        return trackerId;
    }

    public void setTrackerId(String trackerId) {
        this.trackerId = trackerId;
    }

}
