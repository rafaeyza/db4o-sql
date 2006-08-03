package com.spaceprogram.db4o.sql;

/**
 * User: treeder
 * Date: Jul 26, 2006
 * Time: 6:59:48 PM
 */
public class ClassRef {
    private String className;
    private String alias;

    public ClassRef(String className) {
        this.className = className;
    }

    public ClassRef(String className, String alias) {
        this.className = className;
        this.alias = alias;
    }

    public String toString() {
        String ret = className;
        if (alias != null) ret += " " + alias;
        return ret;
    }

    public String getClassName() {
        return className;
    }

    public String getAlias() {
        return alias;
    }
}
