package ch.lxrin.ql.condition;

import ch.lxrin.ql.table.Column;

/**
 * Static factory methods for building SQL {@link Condition}s.
 *
 * <h2>Usage with raw strings (static import)</h2>
 * <pre>{@code
 * import static ch.lxrin.ql.condition.Conditions.*;
 *
 * LxrinQL.createContribution(MyBean.class)
 *     .from("MY_TABLE t")
 *     .select("t.ID", "id")
 *     .where(eq("t.STATUS", ":status"), and(), gt("t.AGE", ":minAge"))
 *     .bind("status", "ACTIVE")
 *     .bind("minAge", 18)
 *     .multiple();
 * }</pre>
 *
 * <h2>Usage with {@link Column} (recommended)</h2>
 * <pre>{@code
 * PersonTable p = new PersonTable();
 * createContribution(PersonBean.class)
 *     .from(p)
 *     .select(p.id)
 *     .where(eq(p.status, ":status"), and(), gt(p.age, ":minAge"))
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
    // Column-based overloads (use with TableDef columns)
    // -------------------------------------------------------------------------

    /** {@link Column} overload for {@link #eq(String, String)}. */
    public static Condition eq(Column column, String value)   { return eq(column.toSql(), value); }

    /** {@link Column} overload for {@link #ne(String, String)}. */
    public static Condition ne(Column column, String value)   { return ne(column.toSql(), value); }

    /** {@link Column} overload for {@link #gt(String, String)}. */
    public static Condition gt(Column column, String value)   { return gt(column.toSql(), value); }

    /** {@link Column} overload for {@link #lt(String, String)}. */
    public static Condition lt(Column column, String value)   { return lt(column.toSql(), value); }

    /** {@link Column} overload for {@link #ge(String, String)}. */
    public static Condition ge(Column column, String value)   { return ge(column.toSql(), value); }

    /** {@link Column} overload for {@link #le(String, String)}. */
    public static Condition le(Column column, String value)   { return le(column.toSql(), value); }

    /** {@link Column} overload for {@link #like(String, String)}. */
    public static Condition like(Column column, String value) { return like(column.toSql(), value); }

    /** {@link Column} overload for {@link #ilike(String, String)}. */
    public static Condition ilike(Column column, String value){ return ilike(column.toSql(), value); }

    /** {@link Column} overload for {@link #in(String, String...)}. */
    public static Condition in(Column column, String... values) { return in(column.toSql(), values); }

    /** {@link Column} overload for {@link #between(String, String, String)}. */
    public static Condition between(Column column, String from, String to) { return between(column.toSql(), from, to); }

    /** {@link Column} overload for {@link #isNull(String)}. */
    public static Condition isNull(Column column)    { return isNull(column.toSql()); }

    /** {@link Column} overload for {@link #isNotNull(String)}. */
    public static Condition isNotNull(Column column) { return isNotNull(column.toSql()); }

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
