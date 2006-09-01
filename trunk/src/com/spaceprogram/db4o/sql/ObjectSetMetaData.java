package com.spaceprogram.db4o.sql;

/**
 * User: treeder
 * Date: Aug 20, 2006
 * Time: 3:42:32 PM
 */
public interface ObjectSetMetaData {
    int getColumnCount();
    String getColumnName(int column);
}
