# LxrinQL Wiki

> Comprehensive guide to the **LxrinQL** fluent query-builder library for Eclipse Scout.
> 
> ⚠️ _This project is AI-generated._

---

## Table of Contents

1. [Introduction](#introduction)
2. [Architecture](#architecture)
3. [API Reference](#api-reference)
   - [LxrinQL (entry point)](#lxrinql-entry-point)
   - [QueryBuilder](#querybuilder)
   - [SelectIntoBuilder](#selectintobuilder)
   - [Conditions](#conditions)
   - [BindMap](#bindmap)
   - [ISqlExecutor](#isqlexecutor)
4. [Integration with Eclipse Scout](#integration-with-eclipse-scout)
5. [PostgreSQL Tips](#postgresql-tips)
6. [Testing Guide](#testing-guide)
7. [FAQ](#faq)

---

## Introduction

LxrinQL eliminates manual SQL string concatenation when working with Eclipse Scout's `SQL` service. It provides:

- A fluent Java API that composes SQL at runtime
- Full support for named bind parameters (`:name` syntax)
- Both `select` (returns `Object[][]`) and `selectInto` (fills Scout table page data)
- An injectable `ISqlExecutor` interface for unit testing without a database

---

## Architecture

```
LxrinQL (static factory)
├── QueryBuilder<T>        → SELECT … FROM … JOIN … WHERE …
│   ├── BindMap            → named parameters
│   ├── Condition          → WHERE clause fragments
│   └── ISqlExecutor       → SQL execution abstraction
│       └── ScoutSqlExecutor (default, delegates to Scout SQL service)
└── SelectIntoBuilder      → SELECT … FROM … WHERE … INTO …
    ├── BindMap
    ├── Condition
    └── ISqlExecutor
```

### Key design decisions

| Decision | Rationale |
|----------|-----------|
| `Condition` is an interface | Allows custom conditions via lambdas |
| `BindMap` is copy-on-write | Supports safe re-use of partially built queries |
| `ISqlExecutor` is injected | Enables Mockito-based unit testing |
| `LogicalOperator` as explicit `Condition` | Keeps the API consistent — no hidden AND insertion |

---

## API Reference

### LxrinQL (entry point)

```java
import static com.lxrin.ql.LxrinQL.*;
```

| Method | Description |
|--------|-------------|
| `createContribution(Class<T>)` | Creates a `QueryBuilder<T>` |
| `createContribution(Class<?>, Class<T>)` | Two-arg overload; first arg (collection type) is ignored |
| `selectInto(Object tableData)` | Creates a `SelectIntoBuilder` |
| All `Conditions.*` methods | Re-exported for single-import convenience |

---

### QueryBuilder

Chain methods, then call a terminal operation.

#### Configuration methods

| Method | Description |
|--------|-------------|
| `.from(String table)` | Sets the FROM clause, e.g. `"MY_TABLE t"` |
| `.select(String expr, String alias)` | Adds a SELECT column |
| `.join(String clause)` | Appends a JOIN clause verbatim |
| `.where(Condition...)` | Adds WHERE conditions |
| `.bind(String name, Object value)` | Adds a named bind parameter |
| `.mapWith(RowMapper<T>)` | Sets the row-to-object mapper |
| `.executor(ISqlExecutor)` | Overrides the SQL executor (testing) |

#### Terminal methods

| Method | Returns | Description |
|--------|---------|-------------|
| `.single()` | `T` or `null` | Executes and returns the first row, or `null` |
| `.multiple()` | `List<T>` | Executes and returns all rows (never `null`) |
| `.buildSql()` | `String` | Returns the generated SQL without executing |

#### Example

```java
List<String> names = createContribution(String.class)
    .from("PERSON t")
    .select("t.NAME", "name")
    .where(eq("t.ACTIVE", ":active"))
    .bind("active", true)
    .mapWith(row -> (String) row[0])
    .multiple();
```

---

### SelectIntoBuilder

Builds `SELECT … INTO` statements for Eclipse Scout table page data.

#### Configuration methods

| Method | Description |
|--------|-------------|
| `.from(String table)` | Sets the FROM clause |
| `.select(String expr, String intoAlias)` | Adds SELECT column + INTO alias |
| `.join(String clause)` | Appends a JOIN clause |
| `.where(Condition...)` | Adds WHERE conditions |
| `.bind(String name, Object value)` | Adds a named bind parameter |
| `.executor(ISqlExecutor)` | Overrides the SQL executor |

#### Terminal method

| Method | Description |
|--------|-------------|
| `.execute()` | Executes `SQL.selectInto(…)` |
| `.buildSql()` | Returns the generated SQL without executing |

#### Example

```java
LxrinQL.selectInto(personTableData)
    .from("PERSON t")
    .select("t.ID",    "id")
    .select("t.NAME",  "name")
    .select("t.EMAIL", "email")
    .where(eq("t.STATUS", ":status"))
    .bind("status", "ACTIVE")
    .execute();
```

Generated SQL:
```sql
SELECT t.ID, t.NAME, t.EMAIL
FROM PERSON t
WHERE t.STATUS = :status
INTO :id, :name, :email
```

---

### Conditions

Import with `import static com.lxrin.ql.condition.Conditions.*;` or use `LxrinQL.*`.

#### Comparison

```java
eq("col", ":val")    // col = :val
ne("col", ":val")    // col <> :val
gt("col", ":val")    // col > :val
lt("col", ":val")    // col < :val
ge("col", ":val")    // col >= :val
le("col", ":val")    // col <= :val
like("col", ":val")  // col LIKE :val
ilike("col", ":val") // col ILIKE :val  (PostgreSQL)
```

#### Set membership

```java
in("col", ":v1", ":v2", ":v3")          // col IN (:v1, :v2, :v3)
between("col", ":from", ":to")           // col BETWEEN :from AND :to
```

#### Null checks

```java
isNull("col")     // col IS NULL
isNotNull("col")  // col IS NOT NULL
```

#### Logic

```java
and()             // AND
or()              // OR
not(eq("c",":v")) // NOT (c = :v)
group(eq("a",":x"), or(), gt("b",":y"))  // (a = :x OR b > :y)
```

#### Custom conditions (lambda)

```java
Condition custom = () -> "LOWER(t.NAME) = LOWER(:name)";
builder.where(custom);
```

---

### BindMap

`BindMap` is a copy-on-write map of named SQL parameters.

```java
BindMap binds = new BindMap()
    .put("status", "ACTIVE")
    .put("minAge", 18);

binds.get("status");   // "ACTIVE"
binds.asMap();         // unmodifiable Map<String, Object>
binds.isEmpty();       // false
```

---

### ISqlExecutor

```java
public interface ISqlExecutor {
    Object[][] select(String sql, BindMap binds);
    void selectInto(String sql, BindMap binds, Object outputData);
    int execute(String sql, BindMap binds);
}
```

The default implementation `ScoutSqlExecutor` delegates to `org.eclipse.scout.rt.server.jdbc.SQL`.

---

## Integration with Eclipse Scout

### Service method example

```java
@Override
public PersonTablePageData getPersonTableData(PersonSearchFormData filter) {
    PersonTablePageData pageData = new PersonTablePageData();

    LxrinQL.selectInto(pageData)
        .from("PERSON t")
        .select("t.PERSON_ID",  "personId")
        .select("t.FIRST_NAME", "firstName")
        .select("t.LAST_NAME",  "lastName")
        .select("t.EMAIL",      "email")
        .where(
            eq("t.STATUS", ":status"),
            and(),
            ilike("t.LAST_NAME", ":lastName")
        )
        .bind("status",   "ACTIVE")
        .bind("lastName", "%" + filter.getLastName().getValue() + "%")
        .execute();

    return pageData;
}
```

### Lookup call example

```java
@Override
protected void execLoadData(ILookupCall<Long> call) {
    List<ILookupRow<Long>> rows = LxrinQL.createContribution(ILookupRow.class)
        .from("CATEGORY t")
        .select("t.ID",   "key")
        .select("t.NAME", "text")
        .where(eq("t.ACTIVE", ":active"))
        .bind("active", true)
        .mapWith(row -> new LookupRow<>((Long) row[0], (String) row[1]))
        .multiple();

    setRows(rows);
}
```

---

## PostgreSQL Tips

- Use `ilike` for case-insensitive search: `ilike("t.NAME", ":name")`
- Use `between` for date ranges: `between("t.CREATED_AT", ":from", ":to")`
- For array contains, write a custom condition: `() -> "t.TAGS @> ARRAY[:tag]::text[]"`
- Bind `null` to skip optional filters on the database side (use Scout's `{? ... }` syntax in combination)

---

## Testing Guide

### Unit testing with Mockito

```java
@Test
void testPersonQuery() {
    ISqlExecutor executor = Mockito.mock(ISqlExecutor.class);
    when(executor.select(anyString(), any()))
        .thenReturn(new Object[][]{{1L, "Alice"}, {2L, "Bob"}});

    List<String> names = LxrinQL.createContribution(String.class)
        .from("PERSON t")
        .select("t.NAME", "name")
        .executor(executor)
        .mapWith(row -> (String) row[0])
        .multiple();

    assertEquals(List.of("Alice", "Bob"), names);
}
```

### Verifying generated SQL

```java
@Test
void testSqlGeneration() {
    String sql = LxrinQL.createContribution(Object[].class)
        .from("PERSON t")
        .select("t.ID", "id")
        .where(eq("t.STATUS", ":status"))
        .buildSql();

    assertEquals("SELECT t.ID FROM PERSON t WHERE t.STATUS = :status", sql);
}
```

### Running the test suite

```bash
mvn test
```

---

## FAQ

**Q: Does LxrinQL support INSERT/UPDATE/DELETE?**  
A: `ISqlExecutor.execute()` exists for DML statements, but the fluent builder currently focuses on SELECT. You can call `executor.execute(sql, binds)` directly for DML.

**Q: Can I use LxrinQL without Eclipse Scout?**  
A: Yes — implement `ISqlExecutor` yourself to delegate to plain JDBC or any other SQL library.

**Q: Does LxrinQL prevent SQL injection?**  
A: LxrinQL uses Scout's named bind parameters (`:name`), which are parameterized. Column names and table names in `.from()`, `.select()`, `.join()` are not escaped — never pass user input there.

**Q: Can I reuse a partially built query?**  
A: Yes. `BindMap` is copy-on-write, so storing a `QueryBuilder` reference and calling `.bind()` multiple times is safe.

**Q: How do I handle optional filters?**  
A: Build conditions conditionally before calling `.where()`:

```java
QueryBuilder<MyBean> qb = createContribution(MyBean.class).from("T").select("T.ID","id");
List<Condition> conds = new ArrayList<>();
if (status != null) { conds.add(eq("T.STATUS", ":status")); qb.bind("status", status); }
if (!conds.isEmpty()) qb.where(conds.toArray(new Condition[0]));
List<MyBean> result = qb.multiple();
```
