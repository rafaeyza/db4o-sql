package com.spaceprogram.db4o.sql.parser;

import com.spaceprogram.db4o.sql.query.*;
import com.spaceprogram.db4o.sql.SqlStatement;

import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.regex.MatchResult;

/**
 * This is a core class that will do the parsing of an SQL string and return that string in an object format.
 * There are no ties to db4o in in this class.
 * <p/>
 * User: treeder
 * Date: Jul 26, 2006
 * Time: 6:05:54 PM
 */
public class SqlParser {
    private static String REGEX_QUOTED_STRING = "'[^']*'"; //"'[^'.]*'"; // "'.*'"; // "'[\\w\\s]*'"; - 
    public static final String REGEX_OPERATORS = "<=|>=|<>|=|<|>";

    private Builder[] builders = {
            new SelectBuilder(),
            new FromBuilder(),
            new WhereBuilder()
    };
    private String query;
    private List<String> quotedStrings = new ArrayList<String>();


    public static SqlStatement parse(String query) throws SqlParseException {
        SqlParser parser = new SqlParser();
        parser.setQuery(query);
        return parser.doParse();

    }

    private SqlStatement doParse() throws SqlParseException {
        // what to do for quoted strings that might have spaces in them??? hmmm, how about taking those out right now and replacing them with tokens
        query = replaceQuotedStrings(query);
        //  System.out.println("query: " + query);

        String[] split = query.trim().split("\\s+"); // split by white space
        // preliminary checks
        if (split.length < 2) {
            throw new SqlParseException("Invalid query.");
        }
        // now build up query into different parts
        SqlQuery sq = new SqlQuery();
        buildQuery(split, sq);
        // ensure that the correct pieces are here
        if (sq.getFrom() == null) {
            throw new SqlParseException("No FROM part!");
        }
        return sq;
    }

    private String replaceQuotedStrings(String query) {
        StringBuffer buff = new StringBuffer(query);
        Pattern pattern = Pattern.compile(REGEX_QUOTED_STRING);
        Matcher matcher = pattern.matcher(buff);

        boolean found = false;
        int i = 0;
        while (matcher.find()) {
            /*System.out.println("found text \"" + matcher.group() +
                    "\" starting at index " + matcher.start() +
                    " and ending at index " + matcher.end() + ".");
            */
            //swap the string out with a token
            quotedStrings.add(matcher.group());
            buff.replace(matcher.start(), matcher.end(), "{" + i + "}");
            matcher.reset(); // todo: could optimize this
            found = true;
            i++;
        }
        if (!found) {
            //  System.out.println("No match found.");
        }
        String ret = buff.toString();
        //System.out.println("query changed to: " + ret);
        return ret;
    }

    private void buildQuery(String[] split, SqlQuery sq) throws SqlParseException {
        // look for keywords
        Builder curBuilder = null;
        List<String> expr = new ArrayList<String>();
        for (String s : split) {
            Builder builder = getBuilder(s);
            if (builder != null) {
                if (curBuilder != null) {
                    curBuilder.build(sq, expr);
                }
                curBuilder = builder;
                expr.clear();
            } else {
                expr.add(s);
            }
        }
        if (expr.size() > 0) {
            if (curBuilder != null)
                curBuilder.build(sq, expr);
            else throw new SqlParseException("Invalid Query. No FROM part.");
        }
    }

    private Builder getBuilder(String s) {
        for (Builder builder : builders) {
            if (s.equalsIgnoreCase(builder.getKeyword())) {
                return builder;
            }
        }
        return null;
    }

    public void setQuery(String query) {
        this.query = query;
    }


    private String replaceQuotedValue(String value) {
        if (value.startsWith("{") && value.endsWith("}")) {
            int replacementIndex = Integer.parseInt(value.substring(1, value.length() - 1));
            return quotedStrings.get(replacementIndex);
        }
        return value;
    }

    interface Builder {
        String getKeyword();

        void build(SqlQuery sq, List<String> expr) throws SqlParseException;
    }

    static class SelectBuilder implements Builder {
        final String keyword = "SELECT";

        public String getKeyword() {
            return keyword;
        }

        public void build(SqlQuery sq, List<String> expr) {
            Select select = new Select();
            // select is just a list of fields separated by commas, not necessarily by spaces, so lets split further by commas
            for (String s : expr) {
                if (s.equals(",")) continue; // skip it
                String[] s2 = s.split(",");
                for (String s1 : s2) {
                    select.addField(s1);
                }
            }
            sq.setSelect(select);
        }
    }

    class FromBuilder implements Builder {
        final String keyword = "FROM";

        public String getKeyword() {
            return keyword;
        }

        public void build(SqlQuery sq, List<String> expr) {
            // from is classname list with possible aliases, aliases separated by spaces, class names, separated by commas
            From from = new From();
            // add class after each comma found
            for (int i = 0; i < expr.size(); i++) {
                // since there can only be max of 3 strings in a set here (class, alias, and comma), i'll just look forward
                String s = expr.get(i); // must be classname
                if (s.equals(",")) {
                    continue;
                }
                if (s.endsWith(",")) { // then just classname
                    from.addClass(stripQuotes(replaceQuotedValue(s.substring(0, s.length() - 1))));
                    continue;
                }
                if (i == expr.size() - 1) {
                    from.addClass(stripQuotes(replaceQuotedValue(s)));
                    continue;
                }
                if (expr.size() > i + 1) {
                    String s2 = expr.get(i + 1);
                    if (s2.equals(",")) {
                        // then just classname as well
                        from.addClass(stripQuotes(replaceQuotedValue(s)));
                    } else {
                        // must be an alias
                        if (s2.endsWith(",")) {
                            s2 = s2.substring(0, s2.length());
                        } // else just ignore, commas alone are just thrown out on next loop
                        from.addClass(stripQuotes(replaceQuotedValue(s)), s2);
                    }
                    i++;
                }

            }
            sq.setFrom(from);

        }
    }

    private String stripQuotes(String s) {
        return s; //s.replaceAll("'", "");
    }

    class WhereBuilder implements Builder {
        final String keyword = "WHERE";

        public String getKeyword() {
            return keyword;
        }

        public void build(SqlQuery sq, List<String> expressionSplit) throws SqlParseException {
            // where clauses are tree like structures
            /*
            samples:
            where c.name = 'somename' and id = 123 or id < 300 and (name = 'scooby' OR name != 'something')

            */
            Where where = new Where();
            buildExpr(where.getRoot(), expressionSplit, 0);
            sq.setWhere(where);

        }

        private int buildExpr(WhereExpression root, List<String> expressionSplit, int index) throws SqlParseException {
            //System.out.println("slit.size= " + expressionSplit.size() + " " + expressionSplit);
            // todo: support BETWEEN x AND y
            Pattern pattern = Pattern.compile(REGEX_OPERATORS);
            WhereExpression current = new WhereExpression();
            int i = index;
            for (; i < expressionSplit.size(); i++) {
                String s = expressionSplit.get(i);
                //System.out.println("WHERE expr: " + s);
                if (s.startsWith("(")) {
                    // then start of a sub expression - recurse
                    String token;
                    if (s.equals("(")) {
                        token = s;
                    } else {
                        // then attached to next token
                        token = s.substring(1);
                        expressionSplit.add(i + 1, token);
                    }
                    i++;
                    WhereExpression sub = new WhereExpression();
                    root.add(sub);
                    i = buildExpr(sub, expressionSplit, i);
                } else if (s.equals(")")) {
                    return i;

                } else if (s.equalsIgnoreCase(WhereExpression.AND)) {
                    // then new expression
                    current = new WhereExpression(WhereExpression.AND);
                    //root.add(current);
                } else if (s.equalsIgnoreCase(WhereExpression.OR)) {
                    current = new WhereExpression(WhereExpression.OR);
                    //root.add(current);
                } else {
                    // otherwise, just normal expression body - ex: name = 'something' or name='something''
                    // first token is field, 2nd is operator, 3rd is value
                    Matcher matcher = pattern.matcher(s);
                    List<MatchResult> matches = findMatches(matcher);
                    int found = matches.size();
                    String operator = null;
                    String field = null;
                    String value = null;
                    int extraPiecesUsed = 0;
                    if (found > 1) {
                        throw new SqlParseException("Too many operators in where expression: " + s);
                    } else if (found == 1) {
                        // then it's on this first token
                        MatchResult matchResult = matches.get(0);
                        operator = matchResult.group();
                        field = s.substring(0, matchResult.start());
                        // check if second half is attached too
                        value = checkForNextPieceAttached(s, matchResult, operator);
                    } else {
                        field = s;
                        // check next piece
                        if(expressionSplit.size() <= i + 1){
                            throw new SqlParseException("Invalid where expression.");
                        }
                        String s2 = expressionSplit.get(i + 1);
                        //System.out.println("s2=" + s2);
                        Matcher matcher2 = pattern.matcher(s2);
                        List<MatchResult> matches2 = findMatches(matcher2);
                        int found2 = matches2.size();
                        if (found2 > 1) {
                            throw new SqlParseException("Too many operators in where expression: " + s);
                        } else if (found2 == 1) {
                            // then all good
                            MatchResult matchResult = matches2.get(0);
                            operator = matchResult.group();
                            //value = //checkForNextPieceAttached(s, matchResult, operator);
                            if (s2.length() > matchResult.end()) {
                                value = s2.substring(matchResult.end(), s2.length());
                            }

                        } else {
                            throw new SqlParseException("Operator not found in where expression.");
                        }
                        extraPiecesUsed++;
                    }
                    if (value == null) {
                        // must be next piece
                        // todo: check for list size before this and the same above
                        value = expressionSplit.get(i + extraPiecesUsed + 1);
                        extraPiecesUsed++;
                    }
                    //System.out.println("full expression found: " + field + "," + operator + "," + value);
                    if (value.endsWith(")")) {
                        value = value.substring(0, value.length() - 1);
                        expressionSplit.add(i + extraPiecesUsed + 1, ")");
                    }
                    // check if value was replaced
                    value = replaceQuotedValue(value);
                    // System.out.println("replaced with: " + value);
                    i += extraPiecesUsed;

                    if (field == null || field.length() < 1
                            || operator == null || operator.length() < 1
                            || value == null || value.length() < 1) {
                        throw new SqlParseException("Incomplete where expression.");
                    }

                    current.setField(field);
                    current.setOperator(operator);
                    current.setValue(value);
                    //      System.out.println("current: " + current);
                    root.add(current);
                }
            }
            return i;

        }


        private String checkForNextPieceAttached(String s, MatchResult matcher, String operator) {
            String value = null;
            if (s.length() > matcher.end() + operator.length()) {
                value = s.substring(matcher.end(), s.length());
            }
            return value;
        }


        private List<MatchResult> findMatches(Matcher matcher) {
            List<MatchResult> ret = new ArrayList<MatchResult>();
            while (matcher.find()) {
//                System.out.println("I found the text \"" + matcher.group() + 
//                        "\" starting at index " + matcher.start() +
//                        " and ending at index " + matcher.end() + ".");
                ret.add(matcher.toMatchResult());
            }
            return ret;
        }
    }

}
