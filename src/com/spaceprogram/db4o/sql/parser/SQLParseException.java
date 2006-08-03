package com.spaceprogram.db4o.sql.parser;

/**
 * User: treeder
 * Date: Aug 1, 2006
 * Time: 3:22:08 PM
 */
public class SQLParseException extends Throwable {
    public SQLParseException(String msg) {
        super(msg);
    }

    public SQLParseException() {
        super();
    }
}
