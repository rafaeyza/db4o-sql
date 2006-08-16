package com.spaceprogram.db4o.sql;

import java.lang.reflect.Field;

/**
 * User: treeder
 * Date: Aug 14, 2006
 * Time: 2:51:16 PM
 */
public class ObjectWrapper implements Result{
    private Object ob;
    private ObjectSetWrapper objectSetWrapper;

    public ObjectWrapper(ObjectSetWrapper objectSetWrapper, Object ob) {
        this.objectSetWrapper = objectSetWrapper;
        this.ob = ob;
    }

    public int getColumnCount() {
        return objectSetWrapper.getColumnCount();
    }

    public Object getObject(int fieldIndex) {
        Field f = objectSetWrapper.getFieldForColumn(ob, fieldIndex);
        return getFieldValue(f, ob);
    }

    private Object getFieldValue(Field f, Object ob) {
        try {
            return f.get(ob);
        } catch (IllegalAccessException e) {
            throw new Sql4oRuntimeException(e);
        }
    }

    public Object getObject(String fieldName) {
        Field f = objectSetWrapper.getFieldForColumn(ob, fieldName);
        return getFieldValue(f, ob);
    }

}
