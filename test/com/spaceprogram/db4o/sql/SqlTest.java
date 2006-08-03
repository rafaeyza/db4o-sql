package com.spaceprogram.db4o.sql;

import com.db4o.ObjectContainer;
import com.db4o.ObjectServer;
import com.db4o.query.Query;
import com.spaceprogram.db4o.Contact;
import com.spaceprogram.db4o.TestUtils;
import com.spaceprogram.db4o.util.DbUtil;
import com.spaceprogram.db4o.sql.jdbc.Db4oConnection;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * User: treeder
 * Date: Jul 10, 2006
 * Time: 2:12:45 PM
 */
public class SqlTest {

    public static void main(String[] args) {
        // intellij not supporting junit4 yet
        SqlTest test = new SqlTest();
        test.setupDb();
        try {
            test.testQueryResults();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        test.tearDown();
    }

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


    String queriesToTest[] = new String[]{
            "select *" // should fail
            , "select * from Contact " //should work fine
            , "from Contact" // should work same as above
    };

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
        // lets time a sode query vs the jdbc
        {
            System.out.println("Soda query...");
            ObjectContainer oc = server.openClient();
            Query q = oc.query();
            q.constrain(Contact.class);
            //q.descend("name").constrain("contact 2");
            q.descend("category").constrain("friends");
            long startTime = System.currentTimeMillis();
            List results = q.execute();
            for (Object o : results) {
                Contact c = (Contact) o;
                System.out.println("got: " + c);
            }
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            System.out.println("soda duration: " + duration);
            oc.close();
        }

        {
            // now same query with jdbc
            System.out.println("SQL query");
            // get JDBC connection
            Connection conn = getConnection();
            try {
                // create JDBC statement
                Statement stmt = conn.createStatement();
                long startTime = System.currentTimeMillis();

                // execute query
                ResultSet rs = stmt.executeQuery("select * from db4o.Contact c where " +
                        // "name = 'contact 2' and " + //  and email = 'email@2.com'
                        " category = 'friends'"
                );
                int counter = 0;
                while (rs.next()) {
                    String name = rs.getString("name");
                    System.out.println("Got: " + name);
                    counter++;
                }
                long endTime = System.currentTimeMillis();
                long duration = endTime - startTime;
                System.out.println("SQL duration: " + duration);
                rs.close();
                stmt.close();
                //Assert.assertEquals(1, counter);

            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                if (conn != null) conn.close();
            }
        }
    }

    private Connection getConnection() {
        return new Db4oConnection(server.openClient());
    }


}
