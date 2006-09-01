package com.spaceprogram.db4o.sql.parser;

import org.junit.Test;
import org.junit.Assert;
import com.spaceprogram.db4o.sql.SqlStatement;
import com.spaceprogram.db4o.sql.SqlParser;

/**
 * User: treeder
 * Date: Aug 1, 2006
 * Time: 11:30:34 AM
 */
public class ParserTest {
    @Test //(expected=SqlParseException.class)
    public void testQueryParsing() throws SqlParseException {
        QueryString queryStringsToTest[] = new QueryString[]{
                new QueryString("  \n  \tselect id,name,   \nemail \n FrOm Contact", "SELECT id, name, email FROM Contact")
                , new QueryString("    from   \n\n \t   Contact where name='contact 1'", "FROM Contact where name = 'contact 1'")
                , new QueryString("select * from Contact c where c.id = 123 AND c.name = 'contact 1'", "select * from Contact c where c.id = 123 AND c.name = 'contact 1'")
                , new QueryString("select c.id, c.name from Contact c where c.id = 123 and ( c.name = 'contact 1' or c.name = 'contact 2' )")
                , new QueryString("select c.id, c.name from Contact c where c.id = 123 and (c.name='contact 1' or c.name='contact 2')", "select c.id, c.name from Contact c where c.id = 123 and ( c.name = 'contact 1' or c.name = 'contact 2' )")
                , new QueryString("from com.spaceprogram.db4o.Contact c where " +
                "(age = 10 or age = 20) and " +
                "income = 50000.02", 
                "from com.spaceprogram.db4o.Contact c where " +
                "( age = 10 or age = 20 ) and " +
                "income = 50000.02")
               };
        for (int i = 0; i < queryStringsToTest.length; i++) {
            QueryString s = queryStringsToTest[i];
            System.out.println("Input: " + s.getInput());
            SqlStatement query = null;
            query = SqlParser.parse(s.getInput());
            System.out.println("Output: " + query);
            Assert.assertEquals(s.getOutput().toLowerCase(), query.toString().toLowerCase());

        }
    }
    @Test(expected= SqlParseException.class)
    public void testForBadQueries() throws SqlParseException {
        QueryString queryStringsToTest[] = new QueryString[]{
                new QueryString("select c.id, c.name from Contact c where c.id = 123 and (c.name = 'contact 1 OR c.name = 'contact 2')") // should fail
        };
        for (int i = 0; i < queryStringsToTest.length; i++) {
            QueryString s = queryStringsToTest[i];
            System.out.println("Input: " + s.getInput());
            SqlStatement query = null;
            query = SqlParser.parse(s.getInput());
            System.out.println("Output: " + query);
            Assert.assertNotSame(s.getOutput().toLowerCase(), query.toString().toLowerCase());

        }
    }
    @Test //(expected= SqlParseException.class)
    public void testGarbageStrings() throws SqlParseException {
        String[] queries = new String[]{
                "asdf",
                "select asdf",
                "select x from",
                "from",
                "from x where name'",
        };
        for (int i = 0; i < queries.length; i++) {
            String query = queries[i];
            System.out.println("testing: " + query);
            try {
                SqlParser.parse(query); // should throw SqlParseExceptions
                // it should not get here
                Assert.assertTrue("Didn't throw SqlParseException!", false);
            } catch (SqlParseException e) {
                // all good
                Assert.assertTrue(true);
            }
        }
    }

}
