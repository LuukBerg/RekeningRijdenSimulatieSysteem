/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package simulatiesysteem.jms;

import com.owlike.genson.Genson;

/**
 *
 * @author u
 */
public class Serializer {
    public static <T> T deserialize(String str, Class<T> t){
        return new Genson().deserialize(str,t);
    }
    public static String serialize(Object obj){
        return new Genson().serialize(obj);
    }
}
