package com.spaceprogram.db4o;

import com.db4o.ObjectContainer;
import com.db4o.query.Query;
import com.spaceprogram.db4o.util.ObjectContainerUtils;
import com.spaceprogram.db4o.sql.Result;

import java.util.List;

/**
 * User: treeder
 * Date: Jul 11, 2006
 * Time: 8:52:10 AM
 */
public class TestUtils {

    public static int contactNumber = 0;

    public static void makeContacts(ObjectContainer oc, int numberOfContacts) {
        System.out.println("Making " + numberOfContacts + " contacts");
        // a some contacts too
        for (int i = 0; i < numberOfContacts; i++) {
            Contact c = new Contact();
            c.setId(contactNumber);
            c.setName("contact " + i);
            c.setEmail("email@" + i + ".com");
            c.setCategory("friends");
            c.setAge(i * 10);
            oc.set(c);
            contactNumber++;
        }
        oc.commit();

    }


    public static int clearObjects(ObjectContainer oc, Class aClass) {
        return ObjectContainerUtils.clear(oc, aClass);
    }

    public static int clear(ObjectContainer oc) {
        return ObjectContainerUtils.clear(oc);
    }

    public static void displaySqlResults(List<Result> results) {
        for (Result result : results) {
            System.out.println("Got: ");
            displaySqlResult(result);
        }
    }

    public static void displaySqlResult(Result result) {
        for (int i = 0; i < result.getColumnCount(); i++) {
            Object o = result.getObject(i);
            System.out.println(i + " = " + o);
        }
    }
}
