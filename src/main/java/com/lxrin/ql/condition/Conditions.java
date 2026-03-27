package com.lxrin.ql.condition;

/**
 * Static factory methods for building SQL {@link Condition}s.
 *
 * <h2>Usage (static import)</h2>
 * <pre>{@code
 * import static com.lxrin.ql.condition.Conditions.*;
 *
 * LxrinQL.createContribution(MyBean.class)
 *     .from("MY_TABLE t")
 *     .select("t.ID", "id")
 *     .where(eq("t.STATUS", ":status"), and(), gt("t.AGE", ":minAge"))
 *     .bind("status", "ACTIVE")
 *     .bind("minAge", 18)
 *     .multiple();
 * }</pre>
 */
public final class Conditions {

    private Conditions() {}

    // -------------------------------------------------------------------------
    // Comparison conditions
    // -------------------------------------------------------------------------

    /** {@code column = value} */
    public static Condition eq(String column, String value) {
        return new SimpleCondition(column, "=", value);
    }

    /** {@code column <> value} */
    public static Condition ne(String column, String value) {
        return new SimpleCondition(column, "<>", value);
    }

    /** {@code column > value} */
    public static Condition gt(String column, String value) {
        return new SimpleCondition(column, ">", value);
    }

    /** {@code column < value} */
    public static Condition lt(String column, String value) {
        return new SimpleCondition(column, "<", value);
    }

    /** {@code column >= value} */
    public static Condition ge(String column, String value) {
        return new SimpleCondition(column, ">=", value);
    }

    /** {@code column <= value} */
    public static Condition le(String column, String value) {
        return new SimpleCondition(column, "<=", value);
    }

    /** {@code column LIKE value} */
    public static Condition like(String column, String value) {
        return new SimpleCondition(column, "LIKE", value);
    }

    /** {@code column ILIKE value} (PostgreSQL case-insensitive LIKE) */
    public static Condition ilike(String column, String value) {
        return new SimpleCondition(column, "ILIKE", value);
    }

    // -------------------------------------------------------------------------
    // IN / BETWEEN / NULL
    // -------------------------------------------------------------------------

    /** {@code column IN (v1, v2, ...)} */
    public static Condition in(String column, String... values) {
        return new InCondition(column, values);
    }

    /** {@code column BETWEEN from AND to} */
    public static Condition between(String column, String from, String to) {
        return new BetweenCondition(column, from, to);
    }

    /** {@code column IS NULL} */
    public static Condition isNull(String column) {
        return new NullCondition(column, true);
    }

    /** {@code column IS NOT NULL} */
    public static Condition isNotNull(String column) {
        return new NullCondition(column, false);
    }

    // -------------------------------------------------------------------------
    // Logical operators
    // -------------------------------------------------------------------------

    /** {@code AND} */
    public static Condition and() {
        return LogicalOperator.AND;
    }

    /** {@code OR} */
    public static Condition or() {
        return LogicalOperator.OR;
    }

    /**
     * Wraps a condition with {@code NOT}: {@code NOT (condition)}.
     */
    public static Condition not(Condition condition) {
        return () -> "NOT (" + condition.toSql() + ")";
    }

    /**
     * Groups a set of conditions in parentheses: {@code (c1 AND c2)}.
     */
    public static Condition group(Condition... conditions) {
        return () -> {
            StringBuilder sb = new StringBuilder("(");
            for (int i = 0; i < conditions.length; i++) {
                if (i > 0) sb.append(" ");
                sb.append(conditions[i].toSql());
            }
            sb.append(")");
            return sb.toString();
        };
    }
}
