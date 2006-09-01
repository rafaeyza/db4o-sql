package com.spaceprogram.db4o.sql;

import com.db4o.ObjectContainer;
import com.db4o.ObjectSet;
import com.db4o.reflect.ReflectClass;
import com.db4o.reflect.ReflectField;
import com.db4o.reflect.ReflectConstructor;
import com.db4o.reflect.jdk.JdkReflector;
import com.db4o.query.Query;
import com.db4o.query.Constraint;

import java.util.List;
import java.lang.reflect.Constructor;

/**
 * This is a core class that will take a parsed SQL string, convert it to a soda query, then execute it.
 * <p/>
 * User: treeder
 * Date: Aug 1, 2006
 * Time: 5:15:56 PM
 */
public class SqlToSoda {
    // this is here just in case we want this field to be configurable
    public static boolean allowClassNotFound = true;

    public static List<Result> execute(ObjectContainer oc, SqlQuery q) throws Sql4oException {
        //System.out.println("QUERY: " + q);
        Query query = oc.query();
        for (int i = 0; i < q.getFrom().getClassRefs().size(); i++) {
            ClassRef classRef = q.getFrom().getClassRefs().get(i);
            String className = classRef.getClassName();
            //Class c = Class.forName(className);
            // Class may not be on classpath, so lets use the generic reflector
            ReflectClass reflectClass = oc.ext().reflector().forName(className);
            if (reflectClass == null) {
                throw new Sql4oException("Class not stored.");
            }
            query.constrain(reflectClass);
            // todo: where should we restrict to one class?  Or allow joins??

            // todo: apply value based where conditions to specific classes, join conditions can then be done after
            if (q.getWhere() != null) {
                try {
                    applyWhere(reflectClass, query, q.getWhere().getRoot());
                } catch (CloneNotSupportedException e) {
                    e.printStackTrace();
                    throw new Sql4oException("Could not apply where conditions.  Exception: " + e.getMessage());
                }
            }
        }

        ObjectSet results = query.execute();
        ObjectSetWrapper resultWrapper = new ObjectSetWrapper(oc);
        resultWrapper.setObjectSet(results);
        if (q.getSelect() != null && q.getSelect().getFields() != null) {
            /*
            This isn't needed anymore
            for (int i = 0; i < q.getSelect().getFields().size(); i++) {
                String field = q.getSelect().getFields().get(i);
                //System.out.println("select: " + field);
            }*/
            resultWrapper.setSelectFields(q.getSelect().getFields());
        }
        return resultWrapper;
    }


    private static void applyWhere(ReflectClass reflectClass, Query dq, WhereExpression where) throws CloneNotSupportedException, Sql4oException {
        List<WhereExpression> expressions = where.getExpressions();
        Constraint previousConstraint = null;
        // then sub constraint, todo: make this happen somehow: AND's and OR's, etc
        // ok, first round: just or/and the expressions to the previous constraint
        if (!where.isRoot()) {
            // start bracket (
        }
        for (int i = 0; i < expressions.size(); i++) {
            WhereExpression whereExpression = expressions.get(i);
            if (whereExpression.getExpressions() != null && whereExpression.getExpressions().size() > 0) {
                applyWhere(reflectClass, dq, whereExpression);
            } else {
                Constraint constraint = makeConstraint(reflectClass, dq, whereExpression);
                if (previousConstraint != null) {
                    if (whereExpression.getType().equalsIgnoreCase(WhereExpression.OR)) {
                        //System.out.println("oring");
                        previousConstraint.or(constraint);
                    } else {
                        //System.out.println("anding");
                        //previousConstraint.and(constraint); // should be equivalent to not adding this
                    }
                }
                previousConstraint = constraint;
            }
        }
        if (!where.isRoot()) {
            // end bracket )
        }


    }

    private static Constraint makeConstraint(ReflectClass reflectClass, Query dq, WhereExpression where) throws CloneNotSupportedException, Sql4oException {
        //System.out.println("adding constraint: " + where);
        // convert to proper object type
        ReflectField field = reflectClass.getDeclaredField(where.getField());
        if (field == null) throw new Sql4oException("Field not found: " + where.getField());
        ReflectClass fieldClass = field.getFieldType();
        Class c = JdkReflector.toNative(fieldClass);
        Object val = convertStringToObjectValue(c, where);
        if (val == null) {
            throw new Sql4oException("Could not create where condition value object! " + where.getValue() + " for field type " + fieldClass);
        }
        Constraint constraint = dq.descend(where.getField()).constrain(val);
        applyOperator(reflectClass, constraint, where.getOperator(), dq, where);
        return constraint;
    }

    private static Object convertStringToObjectValue(Class c, WhereExpression where) {
        Object val = null;
        if (c.isPrimitive()) {
            //System.out.println("is primitive");
            if (c.isAssignableFrom(Integer.TYPE)) {
                //System.out.println("isassignable INT");
                val = new Integer(where.getValue());
            } else if (c.isAssignableFrom(Long.TYPE)) {
                val = new Long(where.getValue());
            } else if (c.isAssignableFrom(Double.TYPE)) {
                val = new Double(where.getValue());
            }
            // todo: add the rest of the types

        } else {
            val = cleanValue(where.getValue());
        }
        return val;
    }

    private static Object cleanValue(String value) {
        // strip quotes
        value = value.replace("'", "");
        return value;
    }

    private static void applyOperator(ReflectClass reflectClass, Constraint constraint, String operator, Query dq, WhereExpression where) throws CloneNotSupportedException, Sql4oException {
        //System.out.println("operator:" + operator);
        if (operator.equals(WhereExpression.OP_GREATER)) {
            constraint.greater();
        } else if (operator.equals(WhereExpression.OP_LESS)) {
            constraint.smaller();
        } else if (operator.equals(WhereExpression.OP_GREATER_OR_EQUAL)) {
            constraint.greater();
            // have to OR this with an equals query too since db4o doesn't support this yet
            WhereExpression where2 = (WhereExpression) where.clone();
            where2.setOperator(WhereExpression.OP_EQUALS);
            constraint.or(makeConstraint(reflectClass, dq, where2));
        } else if (operator.equals(WhereExpression.OP_LESS_OR_EQUAL)) {
            constraint.smaller();
            // have to OR this with an equals query too since db4o doesn't support this yet
            WhereExpression where2 = (WhereExpression) where.clone();
            where2.setOperator(WhereExpression.OP_EQUALS);
            constraint.or(makeConstraint(reflectClass, dq, where2));
        } else {
            constraint.equal(); // default
        }

    }
}
