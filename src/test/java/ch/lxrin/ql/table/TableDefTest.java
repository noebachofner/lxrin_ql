package ch.lxrin.ql.table;

import ch.lxrin.ql.LxrinQL;
import ch.lxrin.ql.bind.Binds;
import ch.lxrin.ql.sql.ISqlExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static ch.lxrin.ql.LxrinQL.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class TableDefTest {

    // -------------------------------------------------------------------------
    // Concrete table definition used in all tests
    // -------------------------------------------------------------------------

    static class ProductTable extends TableDef {
        public final Column productNr = column("PRODUCT_NR");   // auto-alias → "productNr"
        public final Column name      = column("NAME");          // auto-alias → "name"
        public final Column price     = column("PRICE");         // auto-alias → "price"
        public final Column statusCode = column("STATUS_CODE", "statusCode"); // explicit alias

        public ProductTable() {
            super("PRODUCT", "p");
        }
    }

    private final ProductTable products = new ProductTable();
    private ISqlExecutor executor;

    @BeforeEach
    void setUp() {
        executor = Mockito.mock(ISqlExecutor.class);
    }

    // -------------------------------------------------------------------------
    // TableDef basics
    // -------------------------------------------------------------------------

    @Test
    void tableDef_toFromSql() {
        assertEquals("PRODUCT p", products.toFromSql());
    }

    @Test
    void tableDef_getters() {
        assertEquals("PRODUCT", products.getTableName());
        assertEquals("p",       products.getAlias());
    }

    @Test
    void tableDef_throwsOnBlankTableName() {
        assertThrows(IllegalArgumentException.class, () -> new TableDef("", "p") {});
        assertThrows(IllegalArgumentException.class, () -> new TableDef("PRODUCT", "") {});
    }

    // -------------------------------------------------------------------------
    // Column basics
    // -------------------------------------------------------------------------

    @Test
    void column_autoAlias_camelCase() {
        assertEquals("p.PRODUCT_NR", products.productNr.toSql());
        assertEquals("productNr",    products.productNr.getAlias());
    }

    @Test
    void column_autoAlias_singleWord() {
        assertEquals("p.NAME", products.name.toSql());
        assertEquals("name",   products.name.getAlias());
    }

    @Test
    void column_explicitAlias() {
        assertEquals("p.STATUS_CODE", products.statusCode.toSql());
        assertEquals("statusCode",    products.statusCode.getAlias());
    }

    @Test
    void column_toString_returnsSqlExpression() {
        assertEquals("p.PRODUCT_NR", products.productNr.toString());
    }

    // -------------------------------------------------------------------------
    // toCamelCase helper
    // -------------------------------------------------------------------------

    @Test
    void toCamelCase_upperSnake() {
        assertEquals("productNr",  TableDef.toCamelCase("PRODUCT_NR"));
        assertEquals("firstName",  TableDef.toCamelCase("FIRST_NAME"));
        assertEquals("id",         TableDef.toCamelCase("ID"));
        assertEquals("statusCode", TableDef.toCamelCase("STATUS_CODE"));
    }

    // -------------------------------------------------------------------------
    // QueryBuilder integration
    // -------------------------------------------------------------------------

    @Test
    void queryBuilder_fromTableDef_generatesCorrectSql() {
        String sql = createContribution(Object[].class)
                .from(products)
                .select(products.productNr)
                .select(products.name)
                .buildSql();

        assertEquals("SELECT p.PRODUCT_NR, p.NAME FROM PRODUCT p", sql);
    }

    @Test
    void queryBuilder_columnInCondition() {
        String sql = createContribution(Object[].class)
                .from(products)
                .select(products.productNr)
                .where(eq(products.productNr, ":productNr"), and(), gt(products.price, ":minPrice"))
                .buildSql();

        assertEquals("SELECT p.PRODUCT_NR FROM PRODUCT p WHERE p.PRODUCT_NR = :productNr AND p.PRICE > :minPrice", sql);
    }

    @Test
    void queryBuilder_bindObjectIntegration() {
        when(executor.select(anyString(), any()))
                .thenReturn(new Object[][]{{1L, "Widget"}});

        Binds b = new Binds()
                .setLong("productNr", 1L)
                .setString("status", "ACTIVE");

        List<String> names = createContribution(String.class)
                .from(products)
                .select(products.productNr)
                .select(products.name)
                .where(eq(products.productNr, ":productNr"))
                .bind(b)
                .executor(executor)
                .mapWith(row -> (String) row[1])
                .multiple();

        assertEquals(List.of("Widget"), names);
    }

    // -------------------------------------------------------------------------
    // SelectIntoBuilder integration
    // -------------------------------------------------------------------------

    @Test
    void selectIntoBuilder_fromTableDef_generatesCorrectSql() {
        Object fakeData = new Object();
        String sql = LxrinQL.selectInto(fakeData)
                .from(products)
                .select(products.productNr)
                .select(products.name)
                .buildSql();

        assertEquals("SELECT p.PRODUCT_NR, p.NAME FROM PRODUCT p INTO :productNr, :name", sql);
    }

    @Test
    void selectIntoBuilder_bindsObject() {
        ISqlExecutor mockExec = Mockito.mock(ISqlExecutor.class);
        Object fakeData = new Object();

        Binds b = new Binds().setString("status", "ACTIVE");

        LxrinQL.selectInto(fakeData)
                .from(products)
                .select(products.name)
                .where(eq(products.statusCode, ":status"))
                .bind(b)
                .executor(mockExec)
                .execute();

        Mockito.verify(mockExec).selectInto(anyString(), any(), any());
    }
}
