package com.spaceprogram.db4o.sql;

import com.db4o.ObjectContainer;
import com.db4o.ObjectSet;
import com.db4o.query.Query;
import com.db4o.query.Constraint;

import java.util.List;

/**
 * User: treeder
 * Date: Aug 1, 2006
 * Time: 5:15:56 PM
 */
public class SqlToSoda {
    public static List<Result> execute(ObjectContainer oc, SqlQuery q) throws ClassNotFoundException {
        System.out.println("QUERY: " + q);
        Query dq = oc.query();
        for (int i = 0; i < q.getFrom().getClassRefs().size(); i++) {
            ClassRef classRef = q.getFrom().getClassRefs().get(i);
            String className = classRef.getClassName();
            // System.out.println("from: " + className);
            Class c = Class.forName(className);
            dq.constrain(c);
            // todo: where should we restrict to one class?  Or allow joins??
        }
        applyWhere(dq, q.getWhere().getRoot());
        ObjectSet results = dq.execute();
        ObjectSetWrapper resultWrapper = new ObjectSetWrapper();
        resultWrapper.setObjectSet(results);
        // todo: if select fields specified, pull them out and put into array - see spec
        if(q.getSelect() != null && q.getSelect().getFields() != null){
            for (int i = 0; i < q.getSelect().getFields().size(); i++) {
                String field = q.getSelect().getFields().get(i);
                //System.out.println("select: " + field);
            }
            resultWrapper.setSelectFields(q.getSelect().getFields());
        }
        return resultWrapper;
    }


    private static void applyWhere(Query dq, WhereExpression where) {
        List<WhereExpression> expressions = where.getExpressions();
        if(expressions.size() > 0){
            // then sub constraint, todo: make this happen somehow: AND's and OR's, etc
            /*
             Sample:
              Query query=db.query();
                query.constrain(Pilot.class);
                Constraint constr=query.descend("name")
                .constrain("Michael Schumacher");
                query.descend("points")
                .constrain(new Integer(99)).or(constr);
                that means: points == 99 or name = Michael S
             */
            if(!where.isRoot()){
                // start bracket (
            }
            for (int i = 0; i < expressions.size(); i++) {
                WhereExpression whereExpression = expressions.get(i);
                if(i > 0){
                    //buff.append(" ").append(whereExpression.getType());
                }
                makeConstraint(dq, whereExpression);
            }
            if(!where.isRoot()){
                // end bracket )
            }
        }
        else {
            Constraint constraint = makeConstraint(dq, where);
        }

    }

    private static Constraint makeConstraint(Query dq, WhereExpression where) {
        System.out.println("adding constraint: " + where);
        Constraint constraint = dq.descend(where.getField()).constrain(cleanValue(where.getValue()));
        applyOperator(constraint, where.getOperator());
        return constraint;
    }

    private static Object cleanValue(String value) {
        // strip quotes
        value = value.replace("'", "");
        return value;
    }

    private static void applyOperator(Constraint constraint, String operator) {
        // todo: implement, eg: if operator == '=', then constraint.equal();
    }
}
