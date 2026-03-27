package com.lxrin.ql;

import com.lxrin.ql.condition.Condition;
import com.lxrin.ql.condition.Conditions;

/**
 * Main entry point for the <strong>LxrinQL</strong> query-builder library.
 *
 * <p>This class provides static factory methods to create query builders and
 * exposes all {@link Conditions} helpers as convenience re-exports so that a
 * single static import is sufficient:</p>
 *
 * <pre>{@code
 * import static com.lxrin.ql.LxrinQL.*;
 *
 * List<MyBean> rows = createContribution(MyBean.class)
 *     .from("MY_TABLE t")
 *     .select("t.ID",   "id")
 *     .select("t.NAME", "name")
 *     .join("LEFT JOIN ADDRESS a ON a.PERSON_ID = t.ID")
 *     .where(eq("t.STATUS", ":status"), and(), gt("t.AGE", ":minAge"))
 *     .bind("status", "ACTIVE")
 *     .bind("minAge",  18)
 *     .mapWith(row -> new MyBean((Long)row[0], (String)row[1]))
 *     .multiple();
 * }</pre>
 *
 * <h2>selectInto – Eclipse Scout table page data</h2>
 * <pre>{@code
 * LxrinQL.selectInto(myTablePageData)
 *     .from("MY_TABLE t")
 *     .select("t.ID",   "id")
 *     .select("t.NAME", "name")
 *     .where(eq("t.STATUS", ":status"))
 *     .bind("status", "ACTIVE")
 *     .execute();
 * }</pre>
 *
 * <p><em>This project is AI-generated.</em></p>
 *
 * @see QueryBuilder
 * @see SelectIntoBuilder
 * @see Conditions
 */
public final class LxrinQL {

    private LxrinQL() {}

    // =========================================================================
    // Factory methods
    // =========================================================================

    /**
     * Creates a {@link QueryBuilder} for the given element type.
     *
     * <p>Call {@link QueryBuilder#single()} to fetch one row or
     * {@link QueryBuilder#multiple()} to fetch all rows.</p>
     *
     * @param elementType the class of the objects returned by the query
     * @param <T>         element type
     * @return a new, empty {@link QueryBuilder}
     */
    public static <T> QueryBuilder<T> createContribution(Class<T> elementType) {
        return new QueryBuilder<>(elementType);
    }

    /**
     * Convenience overload matching the {@code createContribution(List.class, MyBean.class)}
     * syntax described in the project requirements.
     *
     * <p>The {@code collectionType} parameter is accepted but ignored – the
     * returned builder's {@link QueryBuilder#multiple()} method always returns a
     * {@link java.util.List}.</p>
     *
     * @param collectionType ignored – kept for API symmetry (e.g. {@code List.class})
     * @param elementType    the element class
     * @param <T>            element type
     * @return a new, empty {@link QueryBuilder}
     */
    public static <T> QueryBuilder<T> createContribution(
            @SuppressWarnings("unused") Class<?> collectionType,
            Class<T> elementType) {
        return new QueryBuilder<>(elementType);
    }

    /**
     * Creates a {@link SelectIntoBuilder} that fills the given Eclipse Scout
     * table page data object.
     *
     * @param tableData an Eclipse Scout {@code AbstractTablePageData} (or any
     *                  compatible bean)
     * @return a new {@link SelectIntoBuilder}
     */
    public static SelectIntoBuilder selectInto(Object tableData) {
        return new SelectIntoBuilder(tableData);
    }

    // =========================================================================
    // Conditions re-exports (convenience – avoids a second static import)
    // =========================================================================

    /** @see Conditions#eq(String, String) */
    public static Condition eq(String column, String value)   { return Conditions.eq(column, value); }

    /** @see Conditions#ne(String, String) */
    public static Condition ne(String column, String value)   { return Conditions.ne(column, value); }

    /** @see Conditions#gt(String, String) */
    public static Condition gt(String column, String value)   { return Conditions.gt(column, value); }

    /** @see Conditions#lt(String, String) */
    public static Condition lt(String column, String value)   { return Conditions.lt(column, value); }

    /** @see Conditions#ge(String, String) */
    public static Condition ge(String column, String value)   { return Conditions.ge(column, value); }

    /** @see Conditions#le(String, String) */
    public static Condition le(String column, String value)   { return Conditions.le(column, value); }

    /** @see Conditions#like(String, String) */
    public static Condition like(String column, String value) { return Conditions.like(column, value); }

    /** @see Conditions#ilike(String, String) */
    public static Condition ilike(String column, String value){ return Conditions.ilike(column, value); }

    /** @see Conditions#in(String, String...) */
    public static Condition in(String column, String... values) { return Conditions.in(column, values); }

    /** @see Conditions#between(String, String, String) */
    public static Condition between(String column, String from, String to) { return Conditions.between(column, from, to); }

    /** @see Conditions#isNull(String) */
    public static Condition isNull(String column)    { return Conditions.isNull(column); }

    /** @see Conditions#isNotNull(String) */
    public static Condition isNotNull(String column) { return Conditions.isNotNull(column); }

    /** @see Conditions#and() */
    public static Condition and() { return Conditions.and(); }

    /** @see Conditions#or() */
    public static Condition or()  { return Conditions.or(); }

    /** @see Conditions#not(Condition) */
    public static Condition not(Condition c) { return Conditions.not(c); }

    /** @see Conditions#group(Condition...) */
    public static Condition group(Condition... c) { return Conditions.group(c); }
}
