# LxrinQL API Reference

Complete reference for all public types and methods.

---

## `LxrinQL`

`com.lxrin.ql.LxrinQL`

Static entry point. All methods are `public static`.

### Factory methods

| Signature | Returns | Description |
|-----------|---------|-------------|
| `createContribution(Class<T> elementType)` | `QueryBuilder<T>` | Creates a SELECT builder |
| `createContribution(Class<?> collectionType, Class<T> elementType)` | `QueryBuilder<T>` | Two-arg overload; first arg ignored |
| `selectInto(Object tableData)` | `SelectIntoBuilder` | Creates a SELECT INTO builder |

### Re-exported condition factories

All `Conditions.*` methods are available directly on `LxrinQL`:

```java
import static com.lxrin.ql.LxrinQL.*;
// then use: eq(...), and(), group(...), etc.
```

---

## `QueryBuilder<T>`

`com.lxrin.ql.QueryBuilder`

### Builder methods

| Method | Returns | Description |
|--------|---------|-------------|
| `from(String table)` | `QueryBuilder<T>` | FROM clause, e.g. `"MY_TABLE t"` |
| `select(String expr, String alias)` | `QueryBuilder<T>` | Adds a SELECT column |
| `join(String joinClause)` | `QueryBuilder<T>` | Appends a JOIN verbatim |
| `where(Condition... conditions)` | `QueryBuilder<T>` | Appends WHERE conditions |
| `bind(String name, Object value)` | `QueryBuilder<T>` | Adds a bind parameter |
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

`com.lxrin.ql.SelectIntoBuilder`

### Builder methods

| Method | Returns | Description |
|--------|---------|-------------|
| `from(String table)` | `SelectIntoBuilder` | FROM clause |
| `select(String expr, String intoAlias)` | `SelectIntoBuilder` | SELECT + INTO alias |
| `join(String joinClause)` | `SelectIntoBuilder` | Appends a JOIN verbatim |
| `where(Condition... conditions)` | `SelectIntoBuilder` | Appends WHERE conditions |
| `bind(String name, Object value)` | `SelectIntoBuilder` | Adds a bind parameter |
| `executor(ISqlExecutor executor)` | `SelectIntoBuilder` | Overrides SQL executor |

### Terminal methods

| Method | Returns | Description |
|--------|---------|-------------|
| `execute()` | `void` | Executes the SELECT INTO |
| `buildSql()` | `String` | Returns SQL without executing |

**Throws** `IllegalArgumentException` from constructor if `tableData` is null.  
**Throws** `IllegalStateException` from `buildSql()` / `execute()` if `from()` was never called.

---

## `Conditions`

`com.lxrin.ql.condition.Conditions`

Static factory methods for SQL WHERE fragments.

### Comparison

| Method | SQL |
|--------|-----|
| `eq(col, val)` | `col = val` |
| `ne(col, val)` | `col <> val` |
| `gt(col, val)` | `col > val` |
| `lt(col, val)` | `col < val` |
| `ge(col, val)` | `col >= val` |
| `le(col, val)` | `col <= val` |
| `like(col, val)` | `col LIKE val` |
| `ilike(col, val)` | `col ILIKE val` |

### Set / range

| Method | SQL |
|--------|-----|
| `in(col, val...)` | `col IN (v1, v2, ...)` |
| `between(col, from, to)` | `col BETWEEN from AND to` |

### Null checks

| Method | SQL |
|--------|-----|
| `isNull(col)` | `col IS NULL` |
| `isNotNull(col)` | `col IS NOT NULL` |

### Logic

| Method | SQL |
|--------|-----|
| `and()` | `AND` |
| `or()` | `OR` |
| `not(condition)` | `NOT (...)` |
| `group(condition...)` | `(...)` |

---

## `Condition`

`com.lxrin.ql.condition.Condition`

Functional interface. `String toSql()` returns the SQL fragment.

Custom conditions via lambda:
```java
Condition custom = () -> "EXTRACT(YEAR FROM t.BIRTH_DATE) = :year";
```

---

## `BindMap`

`com.lxrin.ql.bind.BindMap`

Copy-on-write map of named SQL parameters.

| Method | Description |
|--------|-------------|
| `put(String name, Object value)` | Returns a new `BindMap` with the entry added |
| `get(String name)` | Returns the value for a name, or `null` |
| `asMap()` | Returns an unmodifiable `Map<String, Object>` |
| `isEmpty()` | Returns `true` if no entries |

---

## `ISqlExecutor`

`com.lxrin.ql.sql.ISqlExecutor`

| Method | Description |
|--------|-------------|
| `select(String sql, BindMap binds)` | Returns `Object[][]` |
| `selectInto(String sql, BindMap binds, Object outputData)` | Fills output bean |
| `execute(String sql, BindMap binds)` | Returns affected row count |

Default implementation: `ScoutSqlExecutor` (delegates to `org.eclipse.scout.rt.server.jdbc.SQL`).

---

## `RowMapper<T>`

`com.lxrin.ql.RowMapper`

Functional interface: `T map(Object[] row)`.

```java
RowMapper<String> nameMapper = row -> (String) row[0];
```
