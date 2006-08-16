package com.spaceprogram.db4o.sql;

/**
 * User: treeder
 * Date: Aug 14, 2006
 * Time: 2:53:24 PM
 */
public interface Result {
    int getColumnCount();

    Object getObject(int fieldIndex);

    Object getObject(String fieldName);
}
