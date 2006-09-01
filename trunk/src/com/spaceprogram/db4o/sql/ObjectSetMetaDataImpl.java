package com.spaceprogram.db4o.sql;

import com.db4o.ObjectSet;
import com.db4o.ObjectContainer;
import com.db4o.reflect.ReflectClass;
import com.db4o.reflect.ReflectField;

/**
 * User: treeder
 * Date: Aug 20, 2006
 * Time: 3:43:49 PM
 */
public class ObjectSetMetaDataImpl implements ObjectSetMetaData {
    private int columnCount;
    private ReflectClass reflectClass;

    public ObjectSetMetaDataImpl(ObjectSet results, ObjectSetWrapper objectSetWrapper, ObjectContainer oc) {
        init(results, objectSetWrapper, oc);
    }

    private void init(ObjectSet results, ObjectSetWrapper objectSetWrapper, ObjectContainer oc) {
        // examine objects in result set
        // guess we'll have to get an object here first
        if (objectSetWrapper.getLastResult() != null) {
            init(objectSetWrapper.getLastResult(), objectSetWrapper, oc);
        } else if (results.hasNext()) {
            Object nextResult = results.next();
            objectSetWrapper.setNextResult(nextResult);
            init(nextResult, objectSetWrapper, oc);
        }
    }


    private void init(Object lastResult, ObjectSetWrapper objectSetWrapper, ObjectContainer oc) {
        reflectClass = oc.ext().reflector().forObject(lastResult);
        if (objectSetWrapper.hasSelectFields()) {
            columnCount = objectSetWrapper.getSelectFields().size();
        } else {
            columnCount = getDeclaredFields().length;
        }
    }

    private ReflectField[] getDeclaredFields() {
        return ReflectHelper.getDeclaredFields(reflectClass);
    }


    public int getColumnCount() {
        return columnCount;
    }

    public String getColumnName(int column) {
        return ReflectHelper.getDeclaredFields(reflectClass)[column].getName();
    }


}
