
package simulatiesysteem.jms;

import com.owlike.genson.Genson;

/**
 *
 * @author Rabie Bkhiti & Kennard van Grinsven
 */
public class Serializer {

    private final static Genson genson;

    static {
        genson = new Genson();
    }

    public static <T> T deserialize(String str, Class<T> t) {
        return genson.deserialize(str, t);
    }

    public static String serialize(Object obj) {
        return genson.serialize(obj);
    }
}
