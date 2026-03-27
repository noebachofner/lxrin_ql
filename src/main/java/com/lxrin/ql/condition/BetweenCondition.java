package com.lxrin.ql.condition;

/**
 * SQL {@code BETWEEN} condition: {@code column BETWEEN from AND to}.
 */
public class BetweenCondition implements Condition {

    private final String column;
    private final String from;
    private final String to;

    public BetweenCondition(String column, String from, String to) {
        if (column == null || column.isBlank()) throw new IllegalArgumentException("column must not be blank");
        if (from == null) throw new IllegalArgumentException("from must not be null");
        if (to == null) throw new IllegalArgumentException("to must not be null");
        this.column = column;
        this.from = from;
        this.to = to;
    }

    @Override
    public String toSql() {
        return column + " BETWEEN " + from + " AND " + to;
    }
}
