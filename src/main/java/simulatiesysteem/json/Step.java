package simulatiesysteem.json;

import java.io.Serializable;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Data;

/**
 *
 * @author Rabie Bkhiti
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Step implements Serializable {

    private long id;
    private double x;
    private double y;
    private Date timestamp;
    private String trackerId;

    public Step(double x, double y, Date timestamp) {
        this.x = x;
        this.y = y;
        this.timestamp = timestamp;
    }

}
