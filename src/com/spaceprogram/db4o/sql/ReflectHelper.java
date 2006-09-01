package com.spaceprogram.db4o.sql;

import com.db4o.reflect.ReflectClass;
import com.db4o.reflect.ReflectField;
import com.db4o.reflect.generic.GenericVirtualField;

import java.util.ArrayList;
import java.util.List;

/**
 * User: treeder
 * Date: Aug 20, 2006
 * Time: 9:41:34 PM
 */
public class ReflectHelper {
    public static ReflectField[] getDeclaredFields(ReflectClass aClass) {
        // need to filter here because some internal fields are coming through (v4oversion and v4ouuid)
        ReflectField[] fields = aClass.getDeclaredFields();
        List<ReflectField> ret = new ArrayList();
        int x = 0;
        for (int i = 0; i < fields.length; i++) {
            ReflectField field = fields[i];
            if (!(field instanceof GenericVirtualField)) {
                ret.add(field);
            }
        }
        return ret.toArray(new ReflectField[ret.size()]);
    }
}
