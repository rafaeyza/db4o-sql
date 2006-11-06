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
import java.util.Date;
import java.lang.reflect.Constructor;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.text.ParseException;

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
	private static final String DATE_FORMAT_PATTERN = "yyyy-MM-dd";
	public static DateFormat df = new SimpleDateFormat(DATE_FORMAT_PATTERN);

	public static List<Result> execute(ObjectContainer oc, SqlQuery q) throws Sql4oException {
        //System.out.println("QUERY: " + q);
        Query query = oc.query();
        for (int i = 0; i < q.getFrom().getClassRefs().size(); i++) {
            ClassRef classRef = q.getFrom().getClassRefs().get(i);
            String className = classRef.getClassName();

            // if .NET query, might be surrouned by quotes, eg: FROM 'Quizlet.Question, Quizlet.Framework'
            className = className.replaceAll("'", "");

            // Class may not be on classpath, so lets use the generic reflector
            ReflectClass reflectClass = oc.ext().reflector().forName(className);
            if (reflectClass == null) {
                throw new Sql4oException("Class not stored: " + className);
            }
            query.constrain(reflectClass);
            // todo: where should we restrict to one class?  Or allow joins??

            verifySelectFields(reflectClass, q);

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
        if (q.getSelect() != null) {
            resultWrapper.setSelectFields(q.getSelect().getFields());
        }
        return resultWrapper;
    }

    private static void verifySelectFields(ReflectClass reflectClass, SqlQuery q) throws Sql4oException {
        if (q.getSelect() != null) {
            List<String> selFields = q.getSelect().getFields();
            if (selFields != null) {
                // check for asterisk
                if (selFields.size() == 1 && selFields.get(0).equals("*")) {
                    return;
                }
                ReflectField[] fields = ReflectHelper.getDeclaredFields(reflectClass);
                for (int i = 0; i < selFields.size(); i++) {
                    String field = selFields.get(i);
                    boolean fieldOk = false;
                    for (int j = 0; j < fields.length; j++) {
                        ReflectField reflectField = fields[j];
                        if (reflectField.getName().equals(field)) {
                            fieldOk = true;
                            break;
                        }
                    }
                    if (!fieldOk) {
                        throw new Sql4oException("Field not found: " + field);
                    }
                }
            }
        }
    }


    private static void applyWhere(ReflectClass reflectClass, Query dq, WhereExpression where) throws CloneNotSupportedException, Sql4oException{
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

    private static Constraint makeConstraint(ReflectClass reflectClass, Query dq, WhereExpression where) throws CloneNotSupportedException, Sql4oException{
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

    private static Object convertStringToObjectValue(Class c, WhereExpression where) throws Sql4oException {
        Object val = null;

		System.out.println("Class " + c);
		if (c.isPrimitive()) {
            //System.out.println("is primitive " + c);
            if (c.isAssignableFrom(Integer.TYPE)) {
                val = new Integer(where.getValue());
            } else if (c.isAssignableFrom(Long.TYPE)) {
                val = new Long(where.getValue());
            } else if (c.isAssignableFrom(Double.TYPE)) {
                val = new Double(where.getValue());
            }  else if (c.isAssignableFrom(Float.TYPE)) {
                val = new Float(where.getValue());
            } else if (c.isAssignableFrom(Short.TYPE)) {
                val = new Short(where.getValue());
            } else if (c.isAssignableFrom(Byte.TYPE)) {
                val = new Byte(where.getValue());
            }
            // todo: add the rest of the types
		} else if (Number.class.isAssignableFrom(c)) {
			if(c == Integer.class){
				val = new Integer(where.getValue());
			} else if(c == Long.class){
				val = new Long(where.getValue());
			} else if(c == Float.class){
				val = new Float(where.getValue());
			} else if(c == Double.class){
				val = new Double(where.getValue());
			} else if(c == Short.class){
				val = new Short(where.getValue());
			} else if(c == Byte.class){
				val = new Byte(where.getValue());
			}
		} else if(c == String.class){
			val = cleanValue(where.getValue());
		} else if(c == Date.class){
			String dateString = (String) cleanValue(where.getValue());
			try {
				val = df.parse(dateString);
			} catch (ParseException e) {
				throw new Sql4oException("Could not parse date: " + dateString + ". Format must be: " + DATE_FORMAT_PATTERN);
			}
		} else {
			throw new Sql4oException("Value type is not recognized! " + c + " : " + where.getValue());
		}
        return val;
    }

    private static String cleanValue(String value) {
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
        } else if (operator.equals(WhereExpression.OP_NOT_EQUAL) || operator.equals(WhereExpression.OP_NOT_EQUAL_2)) {
            constraint.not();
        } else {
            constraint.equal(); // default
        }

    }
}
