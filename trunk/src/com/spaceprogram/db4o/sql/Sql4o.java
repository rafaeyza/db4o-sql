package com.spaceprogram.db4o.sql;

import com.spaceprogram.db4o.sql.parser.SqlParseException;
import com.db4o.ObjectContainer;

import java.util.List;

/**
 * Core convenience class to wrap up all the functionality.
 * <p/>
 * User: treeder
 * Date: Aug 15, 2006
 * Time: 1:57:20 PM
 */
public class Sql4o {
    public static List<Result> execute(ObjectContainer oc, String query) throws SqlParseException, Sql4oException {
        SqlQuery sqlQuery = (SqlQuery) SqlParser.parse(query);
        List<Result> results = null;
        try {
            results = SqlToSoda.execute(oc, sqlQuery);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            throw new Sql4oException(e.getMessage());
        }
        return results;
    }
}
