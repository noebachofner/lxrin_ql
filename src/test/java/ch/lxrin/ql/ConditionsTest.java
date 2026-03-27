package ch.lxrin.ql;

import ch.lxrin.ql.condition.Conditions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConditionsTest {

    @Test void eq()      { assertEquals("col = :val",   Conditions.eq("col",":val").toSql()); }
    @Test void ne()      { assertEquals("col <> :val",  Conditions.ne("col",":val").toSql()); }
    @Test void gt()      { assertEquals("col > :val",   Conditions.gt("col",":val").toSql()); }
    @Test void lt()      { assertEquals("col < :val",   Conditions.lt("col",":val").toSql()); }
    @Test void ge()      { assertEquals("col >= :val",  Conditions.ge("col",":val").toSql()); }
    @Test void le()      { assertEquals("col <= :val",  Conditions.le("col",":val").toSql()); }
    @Test void like()    { assertEquals("col LIKE :val",Conditions.like("col",":val").toSql()); }
    @Test void ilike()   { assertEquals("col ILIKE :val",Conditions.ilike("col",":val").toSql()); }
    @Test void isNull()  { assertEquals("col IS NULL",  Conditions.isNull("col").toSql()); }
    @Test void isNotNull(){ assertEquals("col IS NOT NULL", Conditions.isNotNull("col").toSql()); }
    @Test void and()     { assertEquals("AND",          Conditions.and().toSql()); }
    @Test void or()      { assertEquals("OR",           Conditions.or().toSql()); }

    @Test void in() {
        assertEquals("col IN (:v1, :v2)", Conditions.in("col", ":v1", ":v2").toSql());
    }

    @Test void between() {
        assertEquals("col BETWEEN :from AND :to", Conditions.between("col",":from",":to").toSql());
    }

    @Test void not() {
        assertEquals("NOT (col = :val)", Conditions.not(Conditions.eq("col",":val")).toSql());
    }

    @Test void group() {
        assertEquals("(col = :a AND col2 > :b)",
                Conditions.group(
                        Conditions.eq("col",":a"),
                        Conditions.and(),
                        Conditions.gt("col2",":b")
                ).toSql());
    }

    @Test void simpleCondition_throwsOnBlankColumn() {
        assertThrows(IllegalArgumentException.class, () -> Conditions.eq("", ":val"));
    }

    @Test void inCondition_throwsOnNoValues() {
        assertThrows(IllegalArgumentException.class, () -> Conditions.in("col"));
    }
}
