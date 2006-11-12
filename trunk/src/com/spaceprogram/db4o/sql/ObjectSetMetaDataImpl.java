package com.spaceprogram.db4o.sql;

import com.db4o.ObjectSet;
import com.db4o.ObjectContainer;
import com.db4o.reflect.ReflectClass;
import com.db4o.reflect.ReflectField;
import com.db4o.reflect.generic.GenericReflector;

import java.util.List;
import java.util.ArrayList;

/**
 * User: treeder
 * Date: Aug 20, 2006
 * Time: 3:43:49 PM
 */
public class ObjectSetMetaDataImpl implements ObjectSetMetaData {
    private int columnCount;
    private ReflectClass reflectClass;
    private List<String> fields;


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
		GenericReflector reflector = oc.ext().reflector();
		reflectClass = reflector.forObject(lastResult);
        if (objectSetWrapper.hasSelectFields()) {
            fields = objectSetWrapper.getSelectFields();
            columnCount = fields.size();
        } else {
            fields = new ArrayList<String>();
            ReflectField[] reflectFields = getDeclaredFields();
            for (int i = 0; i < reflectFields.length; i++) {
                ReflectField reflectField = reflectFields[i];
                fields.add(reflectField.getName());
            }
            columnCount = fields.size();
        }
    }

    private ReflectField[] getDeclaredFields() {
        return ReflectHelper.getDeclaredFieldsInHeirarchy(reflectClass);
    }


    public int getColumnCount() {
        return columnCount;
    }

    public String getColumnName(int column) {
        if(column >= 0 && column < fields.size()){
            return fields.get(column);
        }
        return null;
    }


}
