package com.lxrin.ql;

import com.lxrin.ql.bind.Binds;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class BindsTest {

    @Test
    void setLong_storesValue() {
        Binds b = new Binds().setLong("personNr", 42L);
        assertEquals(42L, b.get("personNr"));
    }

    @Test
    void setInt_storesValue() {
        Binds b = new Binds().setInt("age", 30);
        assertEquals(30, b.get("age"));
    }

    @Test
    void setDouble_storesValue() {
        Binds b = new Binds().setDouble("price", 9.99);
        assertEquals(9.99, b.get("price"));
    }

    @Test
    void setBigDecimal_storesValue() {
        BigDecimal bd = new BigDecimal("123.45");
        Binds b = new Binds().setBigDecimal("amount", bd);
        assertEquals(bd, b.get("amount"));
    }

    @Test
    void setString_storesValue() {
        Binds b = new Binds().setString("status", "ACTIVE");
        assertEquals("ACTIVE", b.get("status"));
    }

    @Test
    void setBoolean_storesValue() {
        Binds b = new Binds().setBoolean("active", true);
        assertEquals(true, b.get("active"));
    }

    @Test
    void setDate_storesValue() {
        LocalDate d = LocalDate.of(2024, 1, 15);
        Binds b = new Binds().setDate("startDate", d);
        assertEquals(d, b.get("startDate"));
    }

    @Test
    void setDateTime_storesValue() {
        LocalDateTime dt = LocalDateTime.of(2024, 1, 15, 10, 30);
        Binds b = new Binds().setDateTime("createdAt", dt);
        assertEquals(dt, b.get("createdAt"));
    }

    @Test
    void set_generic_storesValue() {
        Object obj = new Object();
        Binds b = new Binds().set("obj", obj);
        assertSame(obj, b.get("obj"));
    }

    @Test
    void chaining_storesMultipleValues() {
        Binds b = new Binds()
                .setLong("personNr", 1L)
                .setString("status", "ACTIVE")
                .setInt("age", 25);

        assertEquals(1L, b.get("personNr"));
        assertEquals("ACTIVE", b.get("status"));
        assertEquals(25, b.get("age"));
        assertEquals(3, b.asMap().size());
    }

    @Test
    void isEmpty_trueWhenNoValues() {
        assertTrue(new Binds().isEmpty());
    }

    @Test
    void isEmpty_falseAfterSet() {
        assertFalse(new Binds().setString("x", "y").isEmpty());
    }

    @Test
    void toBindMap_convertsAllEntries() {
        Binds b = new Binds().setLong("id", 10L).setString("name", "Alice");
        var map = b.toBindMap();
        assertEquals(10L, map.get("id"));
        assertEquals("Alice", map.get("name"));
    }

    @Test
    void set_throwsOnBlankName() {
        assertThrows(IllegalArgumentException.class, () -> new Binds().setString("", "value"));
        assertThrows(IllegalArgumentException.class, () -> new Binds().setLong(null, 1L));
    }
}
