package com.lxrin.ql;

import com.lxrin.ql.bind.BindMap;
import com.lxrin.ql.bind.Binds;
import com.lxrin.ql.condition.Condition;
import com.lxrin.ql.sql.ISqlExecutor;
import com.lxrin.ql.sql.ScoutSqlExecutor;
import com.lxrin.ql.table.Column;
import com.lxrin.ql.table.TableDef;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Fluent builder for {@code SQL.selectInto} operations that populate an
 * Eclipse Scout table page data object.
 *
 * <p>The generated SQL follows the Eclipse Scout convention:</p>
 * <pre>{@code
 * SELECT t.ID, t.NAME
 * FROM   MY_TABLE t
 * WHERE  t.STATUS = :status
 * INTO   :id, :name
 * }</pre>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * LxrinQL.selectInto(myTablePageData)
 *     .from("MY_TABLE t")
 *     .select("t.ID",   "id")
 *     .select("t.NAME", "name")
 *     .join("LEFT JOIN ADDRESS a ON a.PERSON_ID = t.ID")
 *     .where(eq("t.STATUS", ":status"))
 *     .bind("status", "ACTIVE")
 *     .execute();
 * }</pre>
 */
public class SelectIntoBuilder {

    private final Object tableData;
    private ISqlExecutor executor;

    private String table;
    private final List<String[]> selectColumns = new ArrayList<>();   // [sqlExpr, intoAlias]
    private final List<String> joins = new ArrayList<>();
    private final List<Condition> conditions = new ArrayList<>();
    private BindMap binds = new BindMap();

    // -------------------------------------------------------------------------
    // Constructor (package-private)
    // -------------------------------------------------------------------------

    SelectIntoBuilder(Object tableData) {
        if (tableData == null) throw new IllegalArgumentException("tableData must not be null");
        this.tableData = tableData;
        this.executor = new ScoutSqlExecutor();
    }

    // -------------------------------------------------------------------------
    // DSL
    // -------------------------------------------------------------------------

    /**
     * Sets the primary FROM table using a {@link TableDef}.
     *
     * @param tableDef table definition, e.g. {@code new ProductTable()}
     */
    public SelectIntoBuilder from(TableDef tableDef) {
        return from(tableDef.toFromSql());
    }

    /** Sets the primary FROM table / expression (e.g. {@code "MY_TABLE t"}). */
    public SelectIntoBuilder from(String table) {
        this.table = table;
        return this;
    }

    /**
     * Adds a {@link Column} to the SELECT / INTO list.
     * Uses {@link Column#toSql()} as the SELECT expression and
     * {@link Column#getAlias()} as the INTO target alias.
     *
     * @param column column reference from a {@link TableDef}
     */
    public SelectIntoBuilder select(Column column) {
        return select(column.toSql(), column.getAlias());
    }

    /**
     * Adds a SELECT column that maps to an INTO alias.
     *
     * @param sqlExpression source SQL expression, e.g. {@code "t.FIRST_NAME"}
     * @param intoAlias     target property name on the output data object
     *                      (will be rendered as {@code :intoAlias} in the
     *                      INTO clause)
     */
    public SelectIntoBuilder select(String sqlExpression, String intoAlias) {
        selectColumns.add(new String[]{sqlExpression, intoAlias});
        return this;
    }

    /**
     * Appends a JOIN clause verbatim.
     */
    public SelectIntoBuilder join(String joinClause) {
        joins.add(joinClause);
        return this;
    }

    /**
     * Adds WHERE conditions (explicit {@code and()}/{@code or()} between them).
     */
    public SelectIntoBuilder where(Condition... conditions) {
        this.conditions.addAll(Arrays.asList(conditions));
        return this;
    }

    /**
     * Merges all parameters from a {@link Binds} instance into this builder.
     *
     * @param binds typed bind parameters
     */
    public SelectIntoBuilder bind(Binds binds) {
        for (java.util.Map.Entry<String, Object> e : binds.asMap().entrySet()) {
            this.binds = this.binds.put(e.getKey(), e.getValue());
        }
        return this;
    }

    /**
     * Adds a named bind parameter.
     *
     * @param name  bind name (without leading {@code :})
     * @param value bind value
     */
    public SelectIntoBuilder bind(String name, Object value) {
        this.binds = binds.put(name, value);
        return this;
    }

    /** Overrides the SQL executor (useful for testing). */
    public SelectIntoBuilder executor(ISqlExecutor executor) {
        this.executor = executor;
        return this;
    }

    // -------------------------------------------------------------------------
    // Terminal operation
    // -------------------------------------------------------------------------

    /**
     * Executes the built {@code SELECT … INTO} statement.
     *
     * @throws IllegalStateException if the FROM clause has not been set
     */
    public void execute() {
        executor.selectInto(buildSql(), binds, tableData);
    }

    // -------------------------------------------------------------------------
    // SQL builder
    // -------------------------------------------------------------------------

    /**
     * Builds and returns the SQL string without executing it.
     *
     * @return the full {@code SELECT … INTO} statement
     */
    public String buildSql() {
        if (table == null || table.isBlank()) {
            throw new IllegalStateException("FROM clause is required – call .from(tableName) first");
        }

        StringBuilder sql = new StringBuilder("SELECT ");

        // SELECT expressions
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

        // INTO
        if (!selectColumns.isEmpty()) {
            sql.append(" INTO ");
            for (int i = 0; i < selectColumns.size(); i++) {
                if (i > 0) sql.append(", ");
                sql.append(":").append(selectColumns.get(i)[1]);
            }
        }

        return sql.toString();
    }

    // -------------------------------------------------------------------------
    // Package-private accessors for tests
    // -------------------------------------------------------------------------

    BindMap getBinds() { return binds; }
    List<String[]> getSelectColumns() { return selectColumns; }
}
