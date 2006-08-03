package com.spaceprogram.db4o.sql.jdbc;

import com.db4o.ObjectServer;
import com.db4o.ObjectContainer;
import com.db4o.query.Query;
import com.spaceprogram.db4o.TestUtils;
import com.spaceprogram.db4o.Contact;
import com.spaceprogram.db4o.util.DbUtil;

import java.sql.*;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Test;


/**
 * User: treeder
 * Date: Jul 10, 2006
 * Time: 2:12:45 PM
 */
public class JdbcTest {

    static ObjectServer server;
    public static String username = "myUser";
    public static String password = "myPass";
    public static int port = 11987;

    @BeforeClass
    public static void setupDb() {
        System.out.println("Setup");
        server = DbUtil.getObjectServer("sqltest", port);
        server.grantAccess(username, password);

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


     private Connection getConnection() throws ClassNotFoundException, SQLException {
        // just like normal jdbc app
         Connection con = null;
             Class.forName("com.spaceprogram.db4o.sql.jdbc.Db4oDriver");
             con = DriverManager.getConnection("jdbc:db4o://localhost:" + port,
                     username, password);

         return con;
    }
    
    /**
     * - test query time vs normal soda query
     * - test that correct number of results are returned
     * - maybe correct value too
     *
     * @throws SQLException
     */
    @Test
    public void testPerformance() throws SQLException, ClassNotFoundException {

        // todo: assert that soda results equal sql results
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
                ResultSet rs = stmt.executeQuery("select * from com.spaceprogram.db4o.Contact c where " +
                         "name = 'contact 2' and " + //  and email = 'email@2.com'
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




}

