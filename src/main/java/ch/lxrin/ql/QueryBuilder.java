package ch.lxrin.ql;

import ch.lxrin.ql.bind.BindMap;
import ch.lxrin.ql.bind.Binds;
import ch.lxrin.ql.condition.Condition;
import ch.lxrin.ql.sql.ISqlExecutor;
import ch.lxrin.ql.sql.ScoutSqlExecutor;
import ch.lxrin.ql.table.Column;
import ch.lxrin.ql.table.TableDef;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Fluent builder for SQL SELECT queries executed through Eclipse Scout's SQL
 * service.
 *
 * <h2>Typical usage</h2>
 * <pre>{@code
 * import static ch.lxrin.ql.condition.Conditions.*;
 *
 * List<MyBean> rows = LxrinQL.createContribution(MyBean.class)
 *     .from("MY_TABLE t")
 *     .select("t.ID",   "id")
 *     .select("t.NAME", "name")
 *     .join("LEFT JOIN OTHER_TABLE o ON o.ID = t.OTHER_ID")
 *     .where(eq("t.STATUS", ":status"), and(), gt("t.AGE", ":minAge"))
 *     .bind("status", "ACTIVE")
 *     .bind("minAge", 18)
 *     .mapWith(row -> {
 *         MyBean b = new MyBean();
 *         b.setId((Long)   row[0]);
 *         b.setName((String) row[1]);
 *         return b;
 *     })
 *     .multiple();
 * }</pre>
 *
 * @param <T> the element type returned by {@link #single()} / {@link #multiple()}
 */
public class QueryBuilder<T> {

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    private final Class<T> elementType;
    private ISqlExecutor executor;

    private String table;
    private final List<String[]> selectColumns = new ArrayList<>();   // [sqlExpr, alias]
    private final List<String> joins = new ArrayList<>();
    private final List<Condition> conditions = new ArrayList<>();
    private BindMap binds = new BindMap();
    private RowMapper<T> rowMapper;

    // -------------------------------------------------------------------------
    // Constructor (package-private – created via LxrinQL)
    // -------------------------------------------------------------------------

    QueryBuilder(Class<T> elementType) {
        this.elementType = elementType;
        this.executor = new ScoutSqlExecutor();
    }

    // -------------------------------------------------------------------------
    // DSL methods
    // -------------------------------------------------------------------------

    /**
     * Sets the primary FROM table using a {@link TableDef}.
     * The SQL fragment is derived from {@link TableDef#toFromSql()}.
     *
     * @param tableDef table definition, e.g. {@code new ProductTable()}
     */
    public QueryBuilder<T> from(TableDef tableDef) {
        return from(tableDef.toFromSql());
    }

    /**
     * Sets the primary FROM table / expression (e.g. {@code "MY_TABLE t"}).
     */
    public QueryBuilder<T> from(String table) {
        this.table = table;
        return this;
    }

    /**
     * Adds a {@link Column} to the SELECT list.
     * Uses {@link Column#toSql()} as the SQL expression and
     * {@link Column#getAlias()} as the result alias.
     *
     * @param column column reference from a {@link TableDef}
     */
    public QueryBuilder<T> select(Column column) {
        return select(column.toSql(), column.getAlias());
    }

    /**
     * Adds a column to the SELECT list.
     *
     * @param sqlExpression SQL expression, e.g. {@code "t.FIRST_NAME"}
     * @param alias         column alias used in the result mapping
     */
    public QueryBuilder<T> select(String sqlExpression, String alias) {
        selectColumns.add(new String[]{sqlExpression, alias});
        return this;
    }

    /**
     * Appends a JOIN clause verbatim (e.g.
     * {@code "INNER JOIN ADDRESS a ON a.PERSON_ID = t.ID"}).
     */
    public QueryBuilder<T> join(String joinClause) {
        joins.add(joinClause);
        return this;
    }

    /**
     * Adds WHERE conditions. Conditions are appended to any previously added
     * conditions separated by a single space, so logical operators
     * ({@code and()}, {@code or()}) must be explicitly included.
     *
     * @param conditions one or more {@link Condition} instances
     */
    public QueryBuilder<T> where(Condition... conditions) {
        this.conditions.addAll(Arrays.asList(conditions));
        return this;
    }

    /**
     * Merges all parameters from a {@link Binds} instance into this builder.
     * Existing parameters with the same name are overwritten.
     *
     * @param binds typed bind parameters
     */
    public QueryBuilder<T> bind(Binds binds) {
        for (java.util.Map.Entry<String, Object> e : binds.asMap().entrySet()) {
            this.binds = this.binds.put(e.getKey(), e.getValue());
        }
        return this;
    }

    /**
     * Adds a named bind parameter. The name must match a {@code :name}
     * placeholder used in the SQL or in a condition value.
     *
     * @param name  bind name (without leading {@code :})
     * @param value bind value
     */
    public QueryBuilder<T> bind(String name, Object value) {
        this.binds = binds.put(name, value);
        return this;
    }

    /**
     * Sets the {@link RowMapper} used to convert result rows to {@code T}.
     * <p>If no mapper is set:</p>
     * <ul>
     *   <li>{@link #single()} returns the first column of the first row cast
     *       to {@code T}.</li>
     *   <li>{@link #multiple()} returns a {@code List<Object[]>} cast unsafely
     *       to {@code List<T>}.</li>
     * </ul>
     *
     * @param rowMapper mapper implementation used for each result row
     * @return this builder instance
     */
    public QueryBuilder<T> mapWith(RowMapper<T> rowMapper) {
        this.rowMapper = rowMapper;
        return this;
    }

    /**
     * Overrides the default {@link ISqlExecutor} (useful for testing).
     */
    public QueryBuilder<T> executor(ISqlExecutor executor) {
        this.executor = executor;
        return this;
    }

    // -------------------------------------------------------------------------
    // Terminal operations
    // -------------------------------------------------------------------------

    /**
     * Executes the query and returns the first result, or {@code null} if no
     * rows are returned.
     *
     * @return first row mapped to {@code T}, or {@code null}
     */
    @SuppressWarnings("unchecked")
    public T single() {
        Object[][] rows = executor.select(buildSql(), binds);
        if (rows == null || rows.length == 0) {
            return null;
        }
        if (rowMapper != null) {
            return rowMapper.map(rows[0]);
        }
        // Default: return the first column of the first row
        return (T) rows[0][0];
    }

    /**
     * Executes the query and returns all results as a {@link List}.
     *
     * @return list of results (never {@code null})
     */
    @SuppressWarnings("unchecked")
    public List<T> multiple() {
        Object[][] rows = executor.select(buildSql(), binds);
        List<T> result = new ArrayList<>();
        if (rows == null) return result;
        for (Object[] row : rows) {
            if (rowMapper != null) {
                result.add(rowMapper.map(row));
            } else {
                result.add((T) row);
            }
        }
        return result;
    }

    // -------------------------------------------------------------------------
    // SQL builder
    // -------------------------------------------------------------------------

    /**
     * Builds and returns the SQL string without executing it.
     * Useful for debugging, logging, or unit-testing the generated SQL.
     *
     * @return the SQL SELECT statement
     * @throws IllegalStateException if the FROM clause has not been set
     */
    public String buildSql() {
        if (table == null || table.isBlank()) {
            throw new IllegalStateException("FROM clause is required – call .from(tableName) first");
        }

        StringBuilder sql = new StringBuilder("SELECT ");

        // SELECT columns
        if (selectColumns.isEmpty()) {
            sql.append("*");
        } else {
            for (int i = 0; i < selectColumns.size(); i++) {
                if (i > 0) sql.append(", ");
                sql.append(selectColumns.get(i)[0]);
            }
        }

        // FROM
        sql.append(" FROM ").append(table);

        // JOINs
        for (String join : joins) {
            sql.append(" ").append(join);
        }

        // WHERE
        if (!conditions.isEmpty()) {
            sql.append(" WHERE ");
            for (int i = 0; i < conditions.size(); i++) {
                if (i > 0) sql.append(" ");
                sql.append(conditions.get(i).toSql());
            }
        }

        return sql.toString();
    }

    // -------------------------------------------------------------------------
    // Accessors (package-private for tests)
    // -------------------------------------------------------------------------

    BindMap getBinds() {
        return binds;
    }

    List<String[]> getSelectColumns() {
        return selectColumns;
    }

    List<Condition> getConditions() {
        return conditions;
    }
}
