/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package simulatiesysteem.json;

import java.io.Serializable;
import java.util.Date;

/**
 *
 * @author u
 */
public class Step implements Serializable {

    private long id;
    private double x;
    private double y;
    private Date timestamp;
    private String trackerId;

    public Step(long id, double x, double y, Date timestamp, String trackerId) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.timestamp = timestamp;
        this.trackerId = trackerId;
    }
}
