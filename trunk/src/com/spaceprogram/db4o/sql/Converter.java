package com.spaceprogram.db4o.sql;

/**
 * Will convert fields to different types
 * <p/>
 * User: treeder
 * Date: Aug 21, 2006
 * Time: 2:39:19 PM
 */
public class Converter {
    public static Boolean convertToBoolean(Object o) {
        if (o instanceof Boolean) {
            // todo: this should accept Numbers too
            return (Boolean) o;
        }
        return false;
    }
}
