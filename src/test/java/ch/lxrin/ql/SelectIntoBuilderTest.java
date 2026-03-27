package ch.lxrin.ql;

import ch.lxrin.ql.sql.ISqlExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static ch.lxrin.ql.LxrinQL.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SelectIntoBuilderTest {

    private ISqlExecutor executor;
    private Object fakeTableData;

    @BeforeEach
    void setUp() {
        executor = Mockito.mock(ISqlExecutor.class);
        fakeTableData = new Object(); // placeholder for AbstractTablePageData
    }

    @Test
    void buildSql_basicSelectInto() {
        String sql = selectInto(fakeTableData)
                .from("MY_TABLE t")
                .select("t.ID",   "id")
                .select("t.NAME", "name")
                .buildSql();

        assertEquals("SELECT t.ID, t.NAME FROM MY_TABLE t INTO :id, :name", sql);
    }

    @Test
    void buildSql_withJoinAndWhere() {
        String sql = selectInto(fakeTableData)
                .from("MY_TABLE t")
                .select("t.ID",   "id")
                .select("t.NAME", "name")
                .join("LEFT JOIN ADDR a ON a.PID = t.ID")
                .where(eq("t.STATUS", ":status"))
                .buildSql();

        assertEquals(
                "SELECT t.ID, t.NAME FROM MY_TABLE t " +
                "LEFT JOIN ADDR a ON a.PID = t.ID " +
                "WHERE t.STATUS = :status " +
                "INTO :id, :name",
                sql);
    }

    @Test
    void buildSql_throwsWhenNoFrom() {
        assertThrows(IllegalStateException.class,
                () -> selectInto(fakeTableData).select("T.ID","id").buildSql());
    }

    @Test
    void execute_delegatesToExecutor() {
        selectInto(fakeTableData)
                .from("MY_TABLE t")
                .select("t.ID", "id")
                .bind("status", "ACTIVE")
                .executor(executor)
                .execute();

        verify(executor, times(1))
                .selectInto(anyString(), any(), eq(fakeTableData));
    }

    @Test
    void selectInto_throwsOnNullTableData() {
        assertThrows(IllegalArgumentException.class, () -> LxrinQL.selectInto(null));
    }

    @Test
    void bind_storesValue() {
        SelectIntoBuilder builder = selectInto(fakeTableData)
                .from("T")
                .bind("status", "ACTIVE");

        assertEquals("ACTIVE", builder.getBinds().get("status"));
    }

    @Test
    void buildSql_noColumnsProducesSelectStar() {
        String sql = selectInto(fakeTableData)
                .from("MY_TABLE")
                .buildSql();

        assertEquals("SELECT * FROM MY_TABLE", sql);
    }
}
