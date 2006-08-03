package com.spaceprogram.db4o.sql;

import com.db4o.ObjectContainer;
import com.db4o.ObjectServer;
import com.db4o.query.Query;
import com.spaceprogram.db4o.Contact;
import com.spaceprogram.db4o.TestUtils;
import com.spaceprogram.db4o.sql.parser.SQLParseException;
import com.spaceprogram.db4o.util.DbUtil;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.Assert;

import java.sql.SQLException;
import java.util.List;

/**
 * User: treeder
 * Date: Jul 10, 2006
 * Time: 2:12:45 PM
 */
public class SqlTest {

    static ObjectServer server;

    @BeforeClass
    public static void setupDb() {
        System.out.println("Setup");
        server = DbUtil.getObjectServer("sqltest");
        ObjectContainer oc = server.openClient();
        TestUtils.makeContacts(oc, 10);
        //TestUtils.dump(oc);
        oc.close();
    }

    @AfterClass
    public static void tearDown() {
        // remove contacts
        System.out.println("Removing contacts");
        ObjectContainer oc = server.openClient();
        TestUtils.clearObjects(oc, Contact.class);
        oc.close();

        // shutdown server
        server.close();
    }

    /**
     * - test query time vs normal soda query
     * - test that correct number of results are returned
     * - maybe correct value too
     *
     * @throws SQLException
     */
    @Test
    public void testQueryResults() throws SQLException {

        // todo: assert that soda results equal sql results
        int sodaCount = 0;
        // lets time a sode query vs the jdbc
        {
            System.out.println("Soda query...");
            ObjectContainer oc = server.openClient();
            Query q = oc.query();
            q.constrain(Contact.class);
            q.descend("name").constrain("contact 2");
            q.descend("category").constrain("friends");
            long startTime = System.currentTimeMillis();
            List results = q.execute();
            for (Object o : results) {
                Contact c = (Contact) o;
                System.out.println("got: " + c);
                sodaCount++;
            }
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            System.out.println("soda duration: " + duration);
            oc.close();
        }

        int sqlCount = 0;
        {
            // now same query with sql
            System.out.println("SQL query");
            // get JDBC connection

             ObjectContainer oc = server.openClient();
            try {
                long startTime = System.currentTimeMillis();

                // execute query
                String query = "select * from com.spaceprogram.db4o.Contact c where " +
                        "name = 'contact 2' and " + //  and email = 'email@2.com'
                        " category = 'friends'";

                SqlQuery sqlQuery = (SqlQuery) SqlParser.parse(query);
                List results = SQLToSoda.execute(oc, sqlQuery);

                for (Object result : results) {
                    System.out.println("Got: " + result);
                    sqlCount++;
                }
                long endTime = System.currentTimeMillis();
                long duration = endTime - startTime;
                System.out.println("SQL duration: " + duration);


            } catch (SQLParseException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } finally {
                oc.close();
            }
        }
        Assert.assertEquals(sodaCount, sqlCount);
    }


}
