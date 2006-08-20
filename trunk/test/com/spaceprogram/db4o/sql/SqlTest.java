package com.spaceprogram.db4o.sql;

import com.db4o.ObjectContainer;
import com.db4o.ObjectServer;
import com.db4o.query.Query;
import com.spaceprogram.db4o.Contact;
import com.spaceprogram.db4o.TestUtils;
import com.spaceprogram.db4o.sql.parser.SqlParseException;
import com.spaceprogram.db4o.util.DbUtil;
import org.junit.*;

import java.sql.SQLException;
import java.util.List;

/**
 * User: treeder
 * Date: Jul 10, 2006
 * Time: 2:12:45 PM
 */
public class SqlTest {

    static ObjectServer server;
    private ObjectContainer oc;

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

    @Before
    public void beforeEach() {
        oc = server.openClient();
    }

    @After
    public void afterEach() {
        oc.close();
    }

    /**
     * - test query time vs normal soda query
     * - test that correct number of results are returned
     * - maybe correct value too
     *
     * @throws SQLException
     */
    @Test
    public void testQueryResults() throws SQLException, SqlParseException, ClassNotFoundException, Sql4oException {

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
            ObjectContainer oc = server.openClient();
            try {
                long startTime = System.currentTimeMillis();

                // execute query
                String query = "select * from com.spaceprogram.db4o.Contact c where " +
                        " name = 'contact 2' and " + //  and email = 'email@2.com'
                        " category = 'friends'";

                List<Result> results = Sql4o.execute(oc, query);

                for (Result result : results) {
                    System.out.println("Got: ");
                    TestUtils.displaySqlResult(result);
                    sqlCount++;
                }
                long endTime = System.currentTimeMillis();
                long duration = endTime - startTime;
                System.out.println("SQL duration: " + duration);
            } finally {
                oc.close();
            }
        }
        Assert.assertEquals(sodaCount, sqlCount);
    }

    @Test
    public void testSelectFieldsQuery() throws SqlParseException, ClassNotFoundException {
        String query = "select name, age from com.spaceprogram.db4o.Contact c where " +
                "name = 'contact 2' and " + //  and email = 'email@2.com'
                " category = 'friends'";

        SqlQuery sqlQuery = (SqlQuery) SqlParser.parse(query);
        List<Result> results = SqlToSoda.execute(oc, sqlQuery);
        TestUtils.displaySqlResults(results);

        Assert.assertEquals(1, results.size());
        // get by index
        Result result = results.get(0);
        Assert.assertEquals("contact 2", result.getObject(0));

        // get by name
        Assert.assertEquals(20, result.getObject("age"));
    }

    @Test(expected = Sql4oRuntimeException.class)
    public void testFieldExceptions() throws SqlParseException, ClassNotFoundException {
        String query = "select name, age from com.spaceprogram.db4o.Contact c where " +
                "name = 'contact 2' and " + //  and email = 'email@2.com'
                " category = 'friends'";

        SqlQuery sqlQuery = (SqlQuery) SqlParser.parse(query);
        List<Result> results = SqlToSoda.execute(oc, sqlQuery);
        TestUtils.displaySqlResults(results);

        Result result = results.get(0);
        Object somefield = result.getObject("somefield"); // this should throw, but it's expected
    }

    @Test
    public void testAsteriskQuery() throws SqlParseException, ClassNotFoundException {
        String query = "select * from com.spaceprogram.db4o.Contact c where " +
                "name = 'contact 2' and " + //  and email = 'email@2.com'
                " category = 'friends'";

        SqlQuery sqlQuery = (SqlQuery) SqlParser.parse(query);
        List<Result> results = SqlToSoda.execute(oc, sqlQuery);
        TestUtils.displaySqlResults(results);

        Assert.assertEquals(1, results.size());
        // get by index
        Result result = results.get(0);
        Assert.assertEquals("contact 2", result.getObject("name"));

        // get by name
        Assert.assertEquals(20, result.getObject("age"));
    }

    @Test
    public void testNoSelectQuery() throws SqlParseException, ClassNotFoundException {
        String query = "from com.spaceprogram.db4o.Contact c where " +
                "name = 'contact 2' and " + //  and email = 'email@2.com'
                " category = 'friends'";

        SqlQuery sqlQuery = (SqlQuery) SqlParser.parse(query);
        List<Result> results = SqlToSoda.execute(oc, sqlQuery);
        TestUtils.displaySqlResults(results);

        Assert.assertEquals(1, results.size());

        Result result = results.get(0);
        Assert.assertEquals("contact 2", result.getObject("name"));

        Assert.assertEquals(20, result.getObject("age"));
    }
}
