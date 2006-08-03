package com.spaceprogram.db4o.sql;

/**
 * User: treeder
 * Date: Jul 26, 2006
 * Time: 6:14:47 PM
 */
public class SqlQuery extends SqlStatement {
    private Select select;
    private From from;
    private Where where;

    public void setSelect(Select select) {
        this.select = select;
    }

    public void setFrom(From from) {
        this.from = from;
    }

    public String toString() {
        StringBuffer buff = new StringBuffer();
        if(select != null){
            buff.append(select);
        }
        if(from != null){
            if(select != null) buff.append(" ");
            buff.append(from);
        }
        if(where != null){
            buff.append(" ").append(where);
        }
        return buff.toString();
    }

    public void setWhere(Where where) {
        this.where = where;
    }

    public Select getSelect() {
        return select;
    }

    public From getFrom() {
        return from;
    }

    public Where getWhere() {
        return where;
    }
}
