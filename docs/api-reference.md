# LxrinQL API Reference

Complete reference for all public types and methods.

---

## `LxrinQL`

`ch.lxrin.ql.LxrinQL`

Static entry point. All methods are `public static`.

### Factory methods

| Signature | Returns | Description |
|-----------|---------|-------------|
| `createContribution(Class<T> elementType)` | `QueryBuilder<T>` | Creates a SELECT builder |
| `createContribution(Class<?> collectionType, Class<T> elementType)` | `QueryBuilder<T>` | Two-arg overload; first arg ignored |
| `selectInto(Object tableData)` | `SelectIntoBuilder` | Creates a SELECT INTO builder |

### Re-exported condition factories

All `Conditions.*` methods (both `String` and `Column` overloads) are available directly on `LxrinQL`:

```java
import static ch.lxrin.ql.LxrinQL.*;
// then use: eq(...), and(), group(...), etc.
// works with both String column names and Column objects from TableDef
```

---

## `QueryBuilder<T>`

`ch.lxrin.ql.QueryBuilder`

### Builder methods

| Method | Returns | Description |
|--------|---------|-------------|
| `from(TableDef table)` | `QueryBuilder<T>` | FROM clause from a typed table definition |
| `from(String table)` | `QueryBuilder<T>` | FROM clause, e.g. `"MY_TABLE t"` |
| `select(Column column)` | `QueryBuilder<T>` | Adds a `Column` from a `TableDef` |
| `select(String expr, String alias)` | `QueryBuilder<T>` | Adds a SELECT column with raw string |
| `join(String joinClause)` | `QueryBuilder<T>` | Appends a JOIN verbatim |
| `where(Condition... conditions)` | `QueryBuilder<T>` | Appends WHERE conditions |
| `bind(Binds binds)` | `QueryBuilder<T>` | Merges all entries from a `Binds` object |
| `bind(String name, Object value)` | `QueryBuilder<T>` | Adds a single bind parameter |
| `mapWith(RowMapper<T> mapper)` | `QueryBuilder<T>` | Sets the row mapper |
| `executor(ISqlExecutor executor)` | `QueryBuilder<T>` | Overrides SQL executor |

### Terminal methods

| Method | Returns | Description |
|--------|---------|-------------|
| `single()` | `T` or `null` | Executes; returns first row or null |
| `multiple()` | `List<T>` | Executes; returns all rows |
| `buildSql()` | `String` | Returns SQL without executing |

**Throws** `IllegalStateException` from `buildSql()` / `single()` / `multiple()` if `from()` was never called.

---

## `SelectIntoBuilder`

`ch.lxrin.ql.SelectIntoBuilder`

### Builder methods

| Method | Returns | Description |
|--------|---------|-------------|
| `from(TableDef table)` | `SelectIntoBuilder` | FROM clause from a typed table definition |
| `from(String table)` | `SelectIntoBuilder` | FROM clause as a raw string |
| `select(Column column)` | `SelectIntoBuilder` | SELECT + INTO alias from a `TableDef` column |
| `select(String expr, String intoAlias)` | `SelectIntoBuilder` | SELECT + INTO alias with raw strings |
| `join(String joinClause)` | `SelectIntoBuilder` | Appends a JOIN verbatim |
| `where(Condition... conditions)` | `SelectIntoBuilder` | Appends WHERE conditions |
| `bind(Binds binds)` | `SelectIntoBuilder` | Merges all entries from a `Binds` object |
| `bind(String name, Object value)` | `SelectIntoBuilder` | Adds a single bind parameter |
| `executor(ISqlExecutor executor)` | `SelectIntoBuilder` | Overrides SQL executor |

### Terminal methods

| Method | Returns | Description |
|--------|---------|-------------|
| `execute()` | `void` | Executes the SELECT INTO |
| `buildSql()` | `String` | Returns SQL without executing |

**Throws** `IllegalArgumentException` from constructor if `tableData` is null.  
**Throws** `IllegalStateException` from `buildSql()` / `execute()` if `from()` was never called.

---

## `Binds`

`ch.lxrin.ql.bind.Binds`

Typed, mutable, chainable bind parameter container. Use `new Binds()` inline in the query chain when you need typed setters:

```java
createContribution(PersonBean.class)
    .from(t)
    .where(eq(t.personNr, ":personNr"))
    .bind(new Binds().setLong("personNr", getPersonNr()))
    .multiple();
```

For simple `String` / `int` values, chain `.bind("name", value)` directly — no `Binds` object needed:

```java
    .bind("status", "ACTIVE")
    .bind("minAge", 18)
```

| Method | Stored type | Description |
|--------|------------|-------------|
| `setLong(name, Long)` | `Long` | Adds a Long parameter |
| `setInt(name, Integer)` | `Integer` | Adds an Integer parameter |
| `setDouble(name, Double)` | `Double` | Adds a Double parameter |
| `setBigDecimal(name, BigDecimal)` | `BigDecimal` | Adds a BigDecimal parameter |
| `setString(name, String)` | `String` | Adds a String parameter |
| `setBoolean(name, Boolean)` | `Boolean` | Adds a Boolean parameter |
| `setDate(name, LocalDate)` | `LocalDate` | Adds a LocalDate parameter |
| `setDateTime(name, LocalDateTime)` | `LocalDateTime` | Adds a LocalDateTime parameter |
| `set(name, Object)` | `Object` | Generic fallback |
| `get(name)` | — | Returns a stored value, or `null` |
| `asMap()` | — | Unmodifiable `Map<String, Object>` |
| `toBindMap()` | `BindMap` | Snapshot as an immutable `BindMap` |
| `isEmpty()` | — | `true` if no entries |

All setters return `this` for chaining. All setters throw `IllegalArgumentException` on blank/null name.

---

## `BindMap`

`ch.lxrin.ql.bind.BindMap`

Immutable (copy-on-write) map of named SQL parameters.

| Method | Description |
|--------|-------------|
| `put(String name, Object value)` | Returns a **new** `BindMap` with the entry added |
| `get(String name)` | Returns the value for a name, or `null` |
| `asMap()` | Returns an unmodifiable `Map<String, Object>` |
| `isEmpty()` | Returns `true` if no entries |

---

## `TableDef`

`ch.lxrin.ql.table.TableDef`

Abstract base class for typed table definitions.

### Subclass example

```java
public class PersonTable extends TableDef {
    public final Column personNr  = column("PERSON_NR");
    public final Column firstName = column("FIRST_NAME");
    public final Column status    = column("STATUS");

    public PersonTable() { super("PERSON", "t"); }
}
```

### Methods

| Method | Modifier | Description |
|--------|---------|-------------|
| `column(String columnName)` | protected | Creates a `Column`; auto-derives camelCase alias |
| `column(String columnName, String alias)` | protected | Creates a `Column` with an explicit alias |
| `toFromSql()` | public | Returns `"TABLE_NAME alias"` for the FROM clause |
| `getTableName()` | public | Returns the raw SQL table name |
| `getAlias()` | public | Returns the table alias |

**Auto-alias rule:** `UPPER_SNAKE_CASE` → `lowerCamelCase`  
Examples: `PERSON_NR` → `personNr`, `FIRST_NAME` → `firstName`, `ID` → `id`

---

## `Column`

`ch.lxrin.ql.table.Column`

Typed column reference. Created exclusively via `TableDef.column(…)`.

| Method | Description |
|--------|-------------|
| `toSql()` | SQL expression, e.g. `"t.PERSON_NR"` |
| `getAlias()` | Java alias, e.g. `"personNr"` |
| `toString()` | Same as `toSql()` |

`Column` can be passed to all `from()`, `select()`, and condition methods.

---

## `Conditions`

`ch.lxrin.ql.condition.Conditions`

Static factory methods for SQL WHERE fragments. Every method has a `String` overload and a `Column` overload.

### Comparison

| Method (String) | Method (Column) | SQL |
|----------------|----------------|-----|
| `eq(col, val)` | `eq(Column, val)` | `col = val` |
| `ne(col, val)` | `ne(Column, val)` | `col <> val` |
| `gt(col, val)` | `gt(Column, val)` | `col > val` |
| `lt(col, val)` | `lt(Column, val)` | `col < val` |
| `ge(col, val)` | `ge(Column, val)` | `col >= val` |
| `le(col, val)` | `le(Column, val)` | `col <= val` |
| `like(col, val)` | `like(Column, val)` | `col LIKE val` |
| `ilike(col, val)` | `ilike(Column, val)` | `col ILIKE val` |

### Set / range

| Method (String) | Method (Column) | SQL |
|----------------|----------------|-----|
| `in(col, val...)` | `in(Column, val...)` | `col IN (v1, v2, ...)` |
| `between(col, from, to)` | `between(Column, from, to)` | `col BETWEEN from AND to` |

### Null checks

| Method (String) | Method (Column) | SQL |
|----------------|----------------|-----|
| `isNull(col)` | `isNull(Column)` | `col IS NULL` |
| `isNotNull(col)` | `isNotNull(Column)` | `col IS NOT NULL` |

### Logic

| Method | SQL |
|--------|-----|
| `and()` | `AND` |
| `or()` | `OR` |
| `not(condition)` | `NOT (...)` |
| `group(condition...)` | `(...)` |

---

## `Condition`

`ch.lxrin.ql.condition.Condition`

Functional interface. `String toSql()` returns the SQL fragment.

Custom conditions via lambda:
```java
Condition custom = () -> "EXTRACT(YEAR FROM t.BIRTH_DATE) = :year";
```

---

## `ISqlExecutor`

`ch.lxrin.ql.sql.ISqlExecutor`

| Method | Description |
|--------|-------------|
| `select(String sql, BindMap binds)` | Returns `Object[][]` |
| `selectInto(String sql, BindMap binds, Object outputData)` | Fills output bean |
| `execute(String sql, BindMap binds)` | Returns affected row count |

Default implementation: `ScoutSqlExecutor` (delegates to `org.eclipse.scout.rt.server.jdbc.SQL`).

---

## `RowMapper<T>`

`ch.lxrin.ql.RowMapper`

Functional interface: `T map(Object[] row)`.

```java
RowMapper<String> nameMapper = row -> (String) row[0];
```
