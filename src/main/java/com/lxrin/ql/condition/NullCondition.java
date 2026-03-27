package com.lxrin.ql.condition;

/**
 * SQL {@code IS NULL} / {@code IS NOT NULL} condition.
 */
public class NullCondition implements Condition {

    private final String column;
    private final boolean isNull;

    public NullCondition(String column, boolean isNull) {
        if (column == null || column.isBlank()) throw new IllegalArgumentException("column must not be blank");
        this.column = column;
        this.isNull = isNull;
    }

    @Override
    public String toSql() {
        return column + (isNull ? " IS NULL" : " IS NOT NULL");
    }
}
