package com.lxrin.ql.table;

/**
 * Base class for user-defined table definitions.
 *
 * <p>Extend this class to declare a database table and its columns as
 * strongly-typed Java fields. Those {@link Column} instances can then be
 * passed directly to {@link com.lxrin.ql.QueryBuilder} and
 * {@link com.lxrin.ql.SelectIntoBuilder} instead of raw SQL strings.</p>
 *
 * <h2>Defining a table</h2>
 * <pre>{@code
 * public class ProductTable extends TableDef {
 *
 *     public final Column productNr = column("PRODUCT_NR");  // → "p.PRODUCT_NR", alias "productNr"
 *     public final Column name      = column("NAME");         // → "p.NAME",       alias "name"
 *     public final Column price     = column("PRICE");        // → "p.PRICE",      alias "price"
 *
 *     public ProductTable() {
 *         super("PRODUCT", "p");
 *     }
 * }
 * }</pre>
 *
 * <h2>Using in a query</h2>
 * <pre>{@code
 * import static com.lxrin.ql.LxrinQL.*;
 *
 * ProductTable p = new ProductTable();
 *
 * List<ProductBean> products = createContribution(ProductBean.class)
 *     .from(p)
 *     .select(p.productNr)
 *     .select(p.name)
 *     .where(eq(p.productNr, ":productNr"))
 *     .bind("productNr", 42L)
 *     .mapWith(row -> new ProductBean((Long) row[0], (String) row[1]))
 *     .multiple();
 * }</pre>
 *
 * <h2>Using with selectInto</h2>
 * <pre>{@code
 * ProductTable p = new ProductTable();
 *
 * selectInto(myTablePageData)
 *     .from(p)
 *     .select(p.productNr)
 *     .select(p.name)
 *     .execute();
 * // → SELECT p.PRODUCT_NR, p.NAME FROM PRODUCT p INTO :productNr, :name
 * }</pre>
 */
public abstract class TableDef {

    private final String tableName;
    private final String alias;

    /**
     * Creates a table definition.
     *
     * @param tableName SQL table name, e.g. {@code "PRODUCT"}
     * @param alias     table alias used in queries, e.g. {@code "p"}
     */
    protected TableDef(String tableName, String alias) {
        if (tableName == null || tableName.isBlank()) {
            throw new IllegalArgumentException("tableName must not be blank");
        }
        if (alias == null || alias.isBlank()) {
            throw new IllegalArgumentException("alias must not be blank");
        }
        this.tableName = tableName;
        this.alias = alias;
    }

    // -------------------------------------------------------------------------
    // Column factory methods (for use inside subclass constructors / field init)
    // -------------------------------------------------------------------------

    /**
     * Creates a {@link Column} whose SQL expression is {@code alias.COLUMN_NAME}.
     * The Java alias is automatically derived by converting
     * {@code UPPER_SNAKE_CASE} to {@code lowerCamelCase}
     * (e.g. {@code "PRODUCT_NR"} → {@code "productNr"}, {@code "ID"} → {@code "id"}).
     *
     * @param columnName SQL column name in {@code UPPER_SNAKE_CASE}
     * @return a new {@link Column}
     */
    protected Column column(String columnName) {
        if (columnName == null || columnName.isBlank()) {
            throw new IllegalArgumentException("columnName must not be blank");
        }
        return new Column(alias + "." + columnName, toCamelCase(columnName));
    }

    /**
     * Creates a {@link Column} with an explicit Java alias.
     * Use this when the auto-derived camelCase alias does not match the
     * property name on your target bean.
     *
     * @param columnName SQL column name, e.g. {@code "PRODUCT_NR"}
     * @param alias      explicit Java alias, e.g. {@code "productNr"}
     * @return a new {@link Column}
     */
    protected Column column(String columnName, String alias) {
        if (columnName == null || columnName.isBlank()) {
            throw new IllegalArgumentException("columnName must not be blank");
        }
        if (alias == null || alias.isBlank()) {
            throw new IllegalArgumentException("alias must not be blank");
        }
        return new Column(this.alias + "." + columnName, alias);
    }

    // -------------------------------------------------------------------------
    // SQL accessors
    // -------------------------------------------------------------------------

    /**
     * Returns the {@code "TABLE_NAME alias"} fragment suitable for a FROM clause,
     * e.g. {@code "PRODUCT p"}.
     */
    public String toFromSql() {
        return tableName + " " + alias;
    }

    /** Returns the raw table name, e.g. {@code "PRODUCT"}. */
    public String getTableName() {
        return tableName;
    }

    /** Returns the table alias, e.g. {@code "p"}. */
    public String getAlias() {
        return alias;
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /**
     * Converts {@code UPPER_SNAKE_CASE} to {@code lowerCamelCase}.
     * <ul>
     *   <li>{@code "PRODUCT_NR"} → {@code "productNr"}</li>
     *   <li>{@code "FIRST_NAME"} → {@code "firstName"}</li>
     *   <li>{@code "ID"}        → {@code "id"}</li>
     * </ul>
     */
    static String toCamelCase(String snakeCase) {
        if (snakeCase == null || snakeCase.isBlank()) {
            return snakeCase;
        }
        String[] parts = snakeCase.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            if (!parts[i].isEmpty()) {
                sb.append(Character.toUpperCase(parts[i].charAt(0)));
                sb.append(parts[i].substring(1));
            }
        }
        return sb.toString();
    }
}
