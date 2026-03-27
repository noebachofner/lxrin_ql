package com.lxrin.ql.condition;

/**
 * Logical join operators ({@code AND}, {@code OR}) used between conditions.
 */
public class LogicalOperator implements Condition {

    /** Singleton AND operator. */
    public static final LogicalOperator AND = new LogicalOperator("AND");

    /** Singleton OR operator. */
    public static final LogicalOperator OR = new LogicalOperator("OR");

    private final String keyword;

    private LogicalOperator(String keyword) {
        this.keyword = keyword;
    }

    @Override
    public String toSql() {
        return keyword;
    }
}
