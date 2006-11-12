package com.spaceprogram.db4o.sql;

import com.db4o.ObjectSet;
import com.db4o.ObjectContainer;
import com.db4o.reflect.generic.GenericObject;
import com.db4o.reflect.generic.GenericVirtualField;
import com.db4o.reflect.generic.GenericReflector;
import com.db4o.reflect.ReflectField;
import com.db4o.reflect.ReflectClass;
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
    private int index;
    private Object lastResult;
    private Object nextResult;
    private ObjectSetMetaData objectSetMetaData;
    private ObjectContainer oc;


    public ObjectSetWrapper(ObjectContainer oc) {
        this.oc = oc;
    }

    public ObjectSetMetaData getMetaData() {
        if (objectSetMetaData == null) {
            objectSetMetaData = new ObjectSetMetaDataImpl(results, this, oc);
        }
        return objectSetMetaData;
    }


    /**
     * This will check the select fields if exists, otherwise it will use the fields on the object
     * todo: could cache the fields for an object here for super fast return
     *
     * @param ob
     * @param columnIndex
     * @return
     */
    public ReflectField getFieldForColumn(Object ob, int columnIndex) throws Sql4oException {
        ReflectClass reflectClass = oc.ext().reflector().forObject(ob);
        if (hasSelectFields() && selectFields.size() > columnIndex) {
            return getField(reflectClass, selectFields.get(columnIndex));
        } else {
            return getField(reflectClass, columnIndex);
        }
    }

    public ReflectField getFieldForColumn(Object ob, String fieldName) throws Sql4oException {
        if (hasSelectFields() && !selectFields.contains(fieldName)) {
            throw new Sql4oRuntimeException("Field not found: " + fieldName);
        }
        ReflectClass reflectClass = oc.ext().reflector().forObject(ob);
        return getField(reflectClass, fieldName);
    }

    private ReflectField getField(ReflectClass aClass, int columnIndex) {
        ReflectField[] fields = ReflectHelper.getDeclaredFieldsInHeirarchy(aClass);
        if (fields.length <= columnIndex || columnIndex < 0) {
            // then out of bounds, so throw
            throw new Sql4oRuntimeException("Field index out of bounds. received: " + columnIndex + " max: " + fields.length);
        } else {
            ReflectField ret = fields[columnIndex];
            ret.setAccessible();
            return ret;
        }

    }



    private ReflectField getField(ReflectClass aClass, String fieldName) throws Sql4oException {
        ReflectField field = ReflectHelper.getDeclaredFieldInHeirarchy(aClass, fieldName);
        if(field == null){
            throw new Sql4oException("Field " + fieldName + " does not exist.");
        }
        field.setAccessible();

        return field;
    }

    public boolean hasSelectFields() {
        return (selectFields != null && selectFields.size() > 0 && !selectFields.get(0).equals("*"));
    }

    public ExtObjectSet ext() {
        return results.ext();
    }

    public boolean hasNext() {
        if(nextResult != null) return true;
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
        return new ObjectWrapper(this, next);
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

    public Object getLastResult() {
        return lastResult;
    }

    /**
     * This should only be used in very rare circumstances
     *
     * @param nextResult
     */
    public void setNextResult(Object nextResult) {
        this.nextResult = nextResult;
    }

    public List<String> getSelectFields() {
        return selectFields;
    }

    public GenericReflector getReflector() {
        return oc.ext().reflector();
    }
}
