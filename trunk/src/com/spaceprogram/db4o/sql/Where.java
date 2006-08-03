package com.spaceprogram.db4o.sql;

import java.util.List;

/**
 * User: treeder
 * Date: Jul 26, 2006
 * Time: 7:09:47 PM
 */
public class Where {
    private WhereExpression root;

    public WhereExpression getRoot() {
        if(root == null) root = new WhereExpression(null);
        return root;
    }

    public String toString() {
        if(root == null) return "";
        StringBuffer ret = new StringBuffer("WHERE");
        ret.append(root.toString());
        return ret.toString();
    }
}
