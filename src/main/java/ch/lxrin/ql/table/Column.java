package ch.lxrin.ql.table;

/**
 * A strongly-typed reference to a database column, carrying both the
 * SQL expression (e.g. {@code "p.PRODUCT_NR"}) and a Java-style alias
 * (e.g. {@code "productNr"}).
 *
 * <p>Instances are created exclusively through {@link TableDef#column(String)}
 * or {@link TableDef#column(String, String)} to ensure the table alias is
 * always included in the SQL expression.</p>
 *
 * <h2>Direct use with conditions</h2>
 * <pre>{@code
 * ProductTable p = new ProductTable();
 *
 * // Conditions accept Column directly:
 * createContribution(ProductBean.class)
 *     .from(p)
 *     .select(p.productNr)
 *     .where(eq(p.productNr, ":productNr"))
 *     .bind("productNr", 42L)
 *     .single();
 * }</pre>
 */
public class Column {

    private final String sqlExpression;
    private final String alias;

    /**
     * Package-private constructor – use {@link TableDef#column} to create instances.
     */
    Column(String sqlExpression, String alias) {
        this.sqlExpression = sqlExpression;
        this.alias = alias;
    }

    /**
     * Returns the fully-qualified SQL column expression, e.g. {@code "p.PRODUCT_NR"}.
     * This is what appears in the {@code SELECT} list and in {@code WHERE} conditions.
     */
    public String toSql() {
        return sqlExpression;
    }

    /**
     * Returns the Java-style alias, e.g. {@code "productNr"}.
     * This is used as the {@code INTO :alias} target in Scout's {@code selectInto}.
     */
    public String getAlias() {
        return alias;
    }

    /**
     * Returns the SQL expression (same as {@link #toSql()}).
     * Enables {@code Column} to be used anywhere a {@code String} expression is expected
     * via {@code column.toString()}.
     */
    @Override
    public String toString() {
        return sqlExpression;
    }
}
