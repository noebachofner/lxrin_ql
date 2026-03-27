package com.lxrin.ql;

import com.lxrin.ql.bind.BindMap;
import com.lxrin.ql.sql.ISqlExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static com.lxrin.ql.LxrinQL.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class QueryBuilderTest {

    private ISqlExecutor executor;

    @BeforeEach
    void setUp() {
        executor = Mockito.mock(ISqlExecutor.class);
    }

    // -------------------------------------------------------------------------
    // SQL building
    // -------------------------------------------------------------------------

    @Test
    void buildSql_selectAll() {
        String sql = createContribution(Object[].class)
                .from("MY_TABLE t")
                .buildSql();

        assertEquals("SELECT * FROM MY_TABLE t", sql);
    }

    @Test
    void buildSql_withSelectColumns() {
        String sql = createContribution(Object[].class)
                .from("MY_TABLE t")
                .select("t.ID",   "id")
                .select("t.NAME", "name")
                .buildSql();

        assertEquals("SELECT t.ID, t.NAME FROM MY_TABLE t", sql);
    }

    @Test
    void buildSql_withJoin() {
        String sql = createContribution(Object[].class)
                .from("MY_TABLE t")
                .select("t.ID", "id")
                .join("LEFT JOIN OTHER o ON o.ID = t.OTHER_ID")
                .buildSql();

        assertEquals("SELECT t.ID FROM MY_TABLE t LEFT JOIN OTHER o ON o.ID = t.OTHER_ID", sql);
    }

    @Test
    void buildSql_withWhereConditions() {
        String sql = createContribution(Object[].class)
                .from("MY_TABLE t")
                .select("t.ID", "id")
                .where(eq("t.STATUS", ":status"), and(), gt("t.AGE", ":minAge"))
                .buildSql();

        assertEquals("SELECT t.ID FROM MY_TABLE t WHERE t.STATUS = :status AND t.AGE > :minAge", sql);
    }

    @Test
    void buildSql_allClauses() {
        String sql = createContribution(Object[].class)
                .from("MY_TABLE t")
                .select("t.ID",   "id")
                .select("t.NAME", "name")
                .join("INNER JOIN ADDR a ON a.PID = t.ID")
                .where(eq("t.STATUS", ":status"), and(), le("t.SCORE", ":maxScore"))
                .buildSql();

        assertEquals(
                "SELECT t.ID, t.NAME FROM MY_TABLE t " +
                "INNER JOIN ADDR a ON a.PID = t.ID " +
                "WHERE t.STATUS = :status AND t.SCORE <= :maxScore",
                sql);
    }

    @Test
    void buildSql_throwsWhenNoFrom() {
        assertThrows(IllegalStateException.class,
                () -> createContribution(Object[].class).buildSql());
    }

    // -------------------------------------------------------------------------
    // Bind parameters
    // -------------------------------------------------------------------------

    @Test
    void bind_storesValues() {
        QueryBuilder<Object[]> builder = createContribution(Object[].class)
                .from("T")
                .bind("status", "ACTIVE")
                .bind("minAge", 18);

        assertEquals("ACTIVE", builder.getBinds().get("status"));
        assertEquals(18,       builder.getBinds().get("minAge"));
    }

    // -------------------------------------------------------------------------
    // single() / multiple()
    // -------------------------------------------------------------------------

    @Test
    void single_returnsFirstColumnOfFirstRow() {
        when(executor.select(anyString(), any())).thenReturn(new Object[][]{{"hello"}});

        String result = createContribution(String.class)
                .from("T")
                .executor(executor)
                .single();

        assertEquals("hello", result);
    }

    @Test
    void single_returnsNullWhenNoRows() {
        when(executor.select(anyString(), any())).thenReturn(new Object[0][0]);

        String result = createContribution(String.class)
                .from("T")
                .executor(executor)
                .single();

        assertNull(result);
    }

    @Test
    void multiple_returnsMappedList() {
        when(executor.select(anyString(), any())).thenReturn(new Object[][]{{1L, "Alice"}, {2L, "Bob"}});

        List<String> names = createContribution(String.class)
                .from("T")
                .executor(executor)
                .mapWith(row -> (String) row[1])
                .multiple();

        assertEquals(List.of("Alice", "Bob"), names);
    }

    @Test
    void multiple_returnsEmptyListWhenNoRows() {
        when(executor.select(anyString(), any())).thenReturn(null);

        List<String> result = createContribution(String.class)
                .from("T")
                .executor(executor)
                .multiple();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void createContribution_twoArgOverload_works() {
        when(executor.select(anyString(), any())).thenReturn(new Object[][]{{"X"}});

        String result = LxrinQL.createContribution(List.class, String.class)
                .from("T")
                .executor(executor)
                .single();

        assertEquals("X", result);
    }

    // -------------------------------------------------------------------------
    // Conditions coverage
    // -------------------------------------------------------------------------

    @Test
    void conditions_allOperators() {
        String sql = createContribution(Object[].class)
                .from("T")
                .select("T.A", "a")
                .where(
                    ne("T.A", ":a"),
                    and(),
                    lt("T.B", ":b"),
                    and(),
                    ge("T.C", ":c"),
                    and(),
                    le("T.D", ":d"),
                    and(),
                    like("T.E", ":e"),
                    and(),
                    in("T.F", ":f1", ":f2"),
                    and(),
                    isNull("T.G"),
                    and(),
                    isNotNull("T.H"),
                    and(),
                    between("T.I", ":from", ":to")
                )
                .buildSql();

        assertTrue(sql.contains("T.A <> :a"));
        assertTrue(sql.contains("T.B < :b"));
        assertTrue(sql.contains("T.C >= :c"));
        assertTrue(sql.contains("T.D <= :d"));
        assertTrue(sql.contains("T.E LIKE :e"));
        assertTrue(sql.contains("T.F IN (:f1, :f2)"));
        assertTrue(sql.contains("T.G IS NULL"));
        assertTrue(sql.contains("T.H IS NOT NULL"));
        assertTrue(sql.contains("T.I BETWEEN :from AND :to"));
    }

    @Test
    void conditions_notAndGroup() {
        String sql = createContribution(Object[].class)
                .from("T")
                .where(not(eq("T.STATUS", ":s")),
                       and(),
                       group(gt("T.A", ":a"), or(), lt("T.B", ":b")))
                .buildSql();

        assertTrue(sql.contains("NOT (T.STATUS = :s)"));
        assertTrue(sql.contains("(T.A > :a OR T.B < :b)"));
    }
}
