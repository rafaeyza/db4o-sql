package com.spaceprogram.db4o.sql.parser;

/**
 * User: treeder
 * Date: Aug 1, 2006
 * Time: 3:22:08 PM
 */
public class SqlParseException extends Exception {
    public SqlParseException(String msg) {
        super(msg);
    }

    public SqlParseException() {
        super();
    }
}
