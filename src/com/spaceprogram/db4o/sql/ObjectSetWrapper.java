package com.spaceprogram.db4o.sql;

import com.db4o.ObjectSet;
import com.db4o.reflect.generic.GenericObject;
import com.db4o.ext.ExtObjectSet;

import java.util.List;
import java.util.Iterator;
import java.util.Collection;
import java.util.ListIterator;
import java.lang.reflect.Field;
import java.sql.SQLException;

/**
 * User: treeder
 * Date: Aug 3, 2006
 * Time: 3:58:32 PM
 */
public class ObjectSetWrapper implements ObjectSet {
    private ObjectSet results;
    private List<String> selectFields;
    private boolean initialized;
    private int index;
    private Object lastResult;
    private Object nextResult;
    private int columnCount;


    /**
     * todo: move this into an ObjectSetMetaData interface
     *
     * @return
     */
    public int getColumnCount() {
        init();
        return columnCount;
    }

    private void init() {
        if (!initialized) {
            // examine objects in result set
            // guess we'll have to get an object here first
            if (lastResult != null) {
                init(lastResult);
            } else if (results.hasNext()) {
                nextResult = results.next();
                init(nextResult);
            }
            initialized = true;
        }
    }

    private void init(Object lastResult) {
        if (hasSelectFields()) {
            columnCount = selectFields.size();
        } else {
            Class c = lastResult.getClass();
            columnCount = c.getDeclaredFields().length;
        }
    }

    /**
     * This will check the select fields if exists, otherwise it will use the fields on the object
     *
     * @param ob
     * @param columnIndex
     * @return
     */
    public Field getFieldForColumn(Object ob, int columnIndex) {
        if (hasSelectFields() && selectFields.size() > columnIndex) {
            try {
                return getField(ob.getClass(), selectFields.get(columnIndex));
            } catch (NoSuchFieldException e) {
                throw new Sql4oRuntimeException(e);
            }
        } else {
            return getField(ob.getClass(), columnIndex);
        }
    }

    public Field getFieldForColumn(Object ob, String fieldName) {
        if (hasSelectFields() && !selectFields.contains(fieldName)) {
            throw new Sql4oRuntimeException("Field not found in select list: " + fieldName);
        }
        try {
            return getField(ob.getClass(), fieldName);
        } catch (NoSuchFieldException e) {
            throw new Sql4oRuntimeException(e);
        }

    }

    private Field getField(Class<? extends Object> aClass, int columnIndex) {
        Field[] fields = aClass.getDeclaredFields();
        if (fields.length <= columnIndex || columnIndex < 0) {
            // then out of bounds, so throw
            throw new Sql4oRuntimeException("Field index out of bounds. received: " + columnIndex + " max: " + fields.length);
        } else {
            Field ret = fields[columnIndex];
            ret.setAccessible(true);
            return ret;
        }

    }

    private Field getField(Class<? extends Object> aClass, String fieldName) throws NoSuchFieldException {
        // todo: check for generic object here and try to use that GenericObject genericObject = 
        Field field = aClass.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field;
    }

    private boolean hasSelectFields() {
        return (selectFields != null && selectFields.size() > 0 && !selectFields.get(0).equals("*"));
    }

    /**
     * This method will do the conversion to the array
     *
     * @param next
     * @return
     * @deprecated 
     */
    private Object[] arrayStruct(Object next) throws SQLException {
        System.out.println("making array");
        Object[] ret;
        if (selectFields != null && selectFields.size() > 0) {
            ret = new Object[selectFields.size()];
            for (int i = 0; i < selectFields.size(); i++) {
                String fieldName = selectFields.get(i);
                if (i == 0 && fieldName.equals("*")) {
                    // then it's the full object
                    ret[i] = next;
                    break;
                }
                ret[i] = getFieldValue(next, fieldName);
            }
        } else {
            // else just return full object
            ret = new Object[1];
            ret[0] = next;
        }
        return ret;
    }

    private Object getFieldValue(Object o, String fieldName) throws SQLException {
        Class c = o.getClass();
        try {
            Field field = c.getDeclaredField(fieldName);
            // todo: use getters if exist
            field.setAccessible(true);
            Object value = field.get(o);
            return value;
        } catch (NoSuchFieldException e) {
            throw new SQLException("No Such Field: " + fieldName);
        } catch (IllegalAccessException e) {
            throw new SQLException("Cannot access field: " + fieldName);
        }
    }

    public ExtObjectSet ext() {
        return results.ext();
    }

    public boolean hasNext() {
        return results.hasNext();
    }

    public Object next() {
        Object next;
        if (nextResult != null) {
            next = nextResult;
            nextResult = null;
        } else {
            next = results.next();
        }
        lastResult = next;
        index++;
        // now replace with array structure based on fields chosen
        // TRYING ObjectWrapper instead
        //try {
        //return arrayStruct(next);
        return new ObjectWrapper(this, next);
        /*} catch (SQLException e) {
            throw new Sql4oRuntimeException(e);
        }*/
    }

    public Object get(int index) {
        Object ret = results.get(index);
        lastResult = ret;
        // now replace with array structure based on fields chosen
        //return arrayStruct(ret);
        return new ObjectWrapper(this, ret);

    }

    public void reset() {
        results.reset();
    }

    public int size() {
        return results.size();
    }

    public boolean isEmpty() {
        return results.isEmpty();
    }

    public boolean contains(Object o) {
        return results.contains(o);
    }

    public Iterator iterator() {
        return this; //results.iterator();
    }

    public Object[] toArray() {
        return results.toArray();
    }

    public Object[] toArray(Object[] a) {
        return results.toArray(a);
    }

    public boolean add(Object o) {
        return results.add(o);
    }

    public boolean remove(Object o) {
        return results.remove(o);
    }

    public boolean containsAll(Collection c) {
        return results.containsAll(c);
    }

    public boolean addAll(Collection c) {
        return results.addAll(c);
    }

    public boolean addAll(int index, Collection c) {
        return results.addAll(index, c);
    }

    public boolean removeAll(Collection c) {
        return results.removeAll(c);
    }

    public boolean retainAll(Collection c) {
        return results.retainAll(c);
    }

    public void clear() {
        results.clear();
    }

    public boolean equals(Object o) {
        return results.equals(o);
    }

    public int hashCode() {
        return results.hashCode();
    }


    public Object set(int index, Object element) {
        return results.set(index, element);
    }

    public void add(int index, Object element) {
        results.add(index, element);
    }

    public Object remove(int index) {
        return results.remove(index);
    }

    public int indexOf(Object o) {
        return results.indexOf(o);
    }

    public int lastIndexOf(Object o) {
        return results.lastIndexOf(o);
    }

    public ListIterator listIterator() {
        return results.listIterator();
    }

    public ListIterator listIterator(int index) {
        return results.listIterator(index);
    }

    public List subList(int fromIndex, int toIndex) {
        return results.subList(fromIndex, toIndex);
    }

    public void remove() {
        results.remove();
    }

    public void setObjectSet(ObjectSet objectSet) {
        this.results = objectSet;
    }

    public void setSelectFields(List<String> selectFields) {
        this.selectFields = selectFields;
    }

}
