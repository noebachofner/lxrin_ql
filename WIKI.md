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
   - [Binds](#binds)
   - [BindMap](#bindmap)
   - [TableDef](#tabledef)
   - [Column](#column)
   - [ISqlExecutor](#isqlexecutor)
4. [Integration with Eclipse Scout](#integration-with-eclipse-scout)
5. [PostgreSQL Tips](#postgresql-tips)
6. [Testing Guide](#testing-guide)
7. [FAQ](#faq)

---

## Introduction

LxrinQL eliminates manual SQL string concatenation when working with Eclipse Scout's `SQL` service. It provides:

- A fluent Java API that composes SQL at runtime
- **Typed table definitions** — define columns once as Java fields and reference them as `table.column` instead of `"alias.COLUMN_NAME"` strings
- **Typed bind parameters** — `Binds` class with `setLong`, `setString`, etc. instead of untyped `Object` values
- Full support for named bind parameters (`:name` syntax)
- Both `select` (returns `Object[][]`) and `selectInto` (fills Scout table page data)
- An injectable `ISqlExecutor` interface for unit testing without a database

---

## Architecture

```
LxrinQL (static factory)
├── QueryBuilder<T>          → SELECT … FROM … JOIN … WHERE …
│   ├── Binds / BindMap      → named parameters (typed or generic)
│   ├── Condition            → WHERE clause fragments
│   └── ISqlExecutor         → SQL execution abstraction
│       └── ScoutSqlExecutor (default – delegates to Scout SQL service)
└── SelectIntoBuilder        → SELECT … FROM … WHERE … INTO …
    ├── Binds / BindMap
    ├── Condition
    └── ISqlExecutor

table package
├── TableDef (abstract)      → base class for typed table definitions
└── Column                   → strongly-typed column reference (SQL expr + alias)
```

### Key design decisions

| Decision | Rationale |
|----------|-----------|
| `TableDef` + `Column` | Eliminates raw SQL strings for table/column names; IDE refactoring safe |
| `Binds` mutable class | Natural imperative style — `b.setLong("id", getPersonNr())` |
| `BindMap` copy-on-write | Functional style; supports safe re-use of partially built queries |
| `Condition` is an interface | Allows custom conditions via lambdas |
| `ISqlExecutor` is injected | Enables Mockito-based unit testing without a database |
| `LogicalOperator` as explicit `Condition` | Keeps the API consistent — no hidden AND insertion |

---

## API Reference

### LxrinQL (entry point)

```java
import static ch.lxrin.ql.LxrinQL.*;
```

| Method | Description |
|--------|-------------|
| `createContribution(Class<T>)` | Creates a `QueryBuilder<T>` |
| `createContribution(Class<?>, Class<T>)` | Two-arg overload; first arg (collection type) is ignored |
| `selectInto(Object tableData)` | Creates a `SelectIntoBuilder` |
| All `Conditions.*` string methods | Re-exported for single-import convenience |
| All `Conditions.*` `Column` overloads | Re-exported — use with `TableDef` columns |

---

### QueryBuilder

Chain builder methods, then call a terminal operation.

#### Builder methods

| Method | Description |
|--------|-------------|
| `.from(TableDef table)` | FROM clause from a typed table definition |
| `.from(String table)` | FROM clause as a raw string, e.g. `"MY_TABLE t"` |
| `.select(Column column)` | Adds a SELECT column from a `TableDef` field |
| `.select(String expr, String alias)` | Adds a SELECT column with a raw expression |
| `.join(String clause)` | Appends a JOIN clause verbatim |
| `.where(Condition...)` | Adds WHERE conditions |
| `.bind(Binds binds)` | Merges all entries from a `Binds` object |
| `.bind(String name, Object value)` | Adds a single named bind parameter |
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
PersonTable t = new PersonTable();
Binds b = new Binds();

List<String> names = createContribution(String.class)
    .from(t)
    .select(t.firstName)
    .where(eq(t.status, b.setString("ACTIVE")), and(), ge(t.age, b.setInt(18)))
    .bind(b)
    .mapWith(row -> (String) row[0])
    .multiple();
```

---

### SelectIntoBuilder

Builds `SELECT … INTO` statements for Eclipse Scout table page data.

#### Builder methods

| Method | Description |
|--------|-------------|
| `.from(TableDef table)` | FROM clause from a typed table definition |
| `.from(String table)` | FROM clause as a raw string |
| `.select(Column column)` | SELECT + INTO alias from a `TableDef` column |
| `.select(String expr, String intoAlias)` | SELECT + INTO alias with raw strings |
| `.join(String clause)` | Appends a JOIN clause |
| `.where(Condition...)` | Adds WHERE conditions |
| `.bind(Binds binds)` | Merges all entries from a `Binds` object |
| `.bind(String name, Object value)` | Adds a single named bind parameter |
| `.executor(ISqlExecutor)` | Overrides the SQL executor |

#### Terminal methods

| Method | Description |
|--------|-------------|
| `.execute()` | Executes `SQL.selectInto(…)` |
| `.buildSql()` | Returns the generated SQL without executing |

#### Example

```java
PersonTable t = new PersonTable();
Binds b = new Binds();

LxrinQL.selectInto(personTableData)
    .from(t)
    .select(t.personNr)
    .select(t.firstName)
    .select(t.lastName)
    .where(eq(t.status, b.setString("ACTIVE")))
    .bind(b)
    .execute();
```

Generated SQL:
```sql
SELECT t.PERSON_NR, t.FIRST_NAME, t.LAST_NAME
FROM PERSON t
WHERE t.STATUS = :status
INTO :personNr, :firstName, :lastName
```

---

### Conditions

Import with `import static ch.lxrin.ql.condition.Conditions.*;` or use `LxrinQL.*`.

All methods come in two overloads:
- `eq(String column, String value)` — raw SQL string
- `eq(Column column, String value)` — typed `Column` from a `TableDef`

#### Comparison

```java
eq(col, ":val")    // col = :val
ne(col, ":val")    // col <> :val
gt(col, ":val")    // col > :val
lt(col, ":val")    // col < :val
ge(col, ":val")    // col >= :val
le(col, ":val")    // col <= :val
like(col, ":val")  // col LIKE :val
ilike(col, ":val") // col ILIKE :val  (PostgreSQL)
```

#### Set membership

```java
in(col, ":v1", ":v2", ":v3")   // col IN (:v1, :v2, :v3)
between(col, ":from", ":to")   // col BETWEEN :from AND :to
```

#### Null checks

```java
isNull(col)     // col IS NULL
isNotNull(col)  // col IS NOT NULL
```

#### Logic

```java
and()                                    // AND
or()                                     // OR
not(eq(col, ":v"))                       // NOT (col = :v)
group(eq(col, ":x"), or(), gt(col2, ":y")) // (col = :x OR col2 > :y)
```

#### Custom conditions (lambda)

```java
Condition custom = () -> "LOWER(t.NAME) = LOWER(:name)";
builder.where(custom);
```

---

### Binds

`ch.lxrin.ql.bind.Binds`

Typed, mutable container for named SQL bind parameters. Supports method chaining.

**Primary pattern — single-argument setters inline inside conditions:**

Create one `Binds b = new Binds()` and call the typed setters directly where
the placeholder is needed.  Each call auto-names the parameter (`p0`, `p1`, …)
and returns the `:placeholder` string:

```java
Binds b = new Binds();

createContribution(PersonBean.class)
    .from(t)
    .where(eq(t.status, b.setString("ACTIVE")),
           and(),
           ge(t.age,    b.setInt(18)),
           and(),
           eq(t.personNr, b.setLong(getPersonNr())))
    .bind(b)
    .multiple();
```

**Alternative — named parameters (useful when sharing a placeholder):**
```java
createContribution(PersonBean.class)
    .from(t)
    .where(eq(t.personNr, ":personNr"))
    .bind(new Binds()
        .setLong("personNr", getPersonNr())
        .setDate("since",    LocalDate.now()))
    .multiple();
```

| Method | Stored type |
|--------|------------|
| `setLong(name, Long)` | `Long` |
| `setInt(name, Integer)` | `Integer` |
| `setDouble(name, Double)` | `Double` |
| `setBigDecimal(name, BigDecimal)` | `BigDecimal` |
| `setString(name, String)` | `String` |
| `setBoolean(name, Boolean)` | `Boolean` |
| `setDate(name, LocalDate)` | `LocalDate` |
| `setDateTime(name, LocalDateTime)` | `LocalDateTime` |
| `set(name, Object)` | generic fallback |
| `get(name)` | reads a stored value |
| `asMap()` | unmodifiable `Map<String, Object>` |
| `toBindMap()` | snapshot as `BindMap` |
| `isEmpty()` | `true` if no entries |

---

### BindMap

`ch.lxrin.ql.bind.BindMap`

Copy-on-write (immutable) map of named SQL parameters. Used internally by the builders; also useful for functional / pre-built bind configurations.

```java
BindMap binds = new BindMap()
    .put("status", "ACTIVE")
    .put("minAge", 18);

binds.get("status");   // "ACTIVE"
binds.asMap();         // unmodifiable Map<String, Object>
binds.isEmpty();       // false
```

> **Tip:** For everyday queries use `.bind("name", value)` directly in the chain. Use `BindMap` only when you need immutable/functional semantics.

---

### TableDef

`ch.lxrin.ql.table.TableDef`

Abstract base class for typed table definitions. Subclass once per database table.

```java
public class ProductTable extends TableDef {
    public final Column productNr = column("PRODUCT_NR"); // alias → "productNr"
    public final Column name      = column("NAME");        // alias → "name"
    public final Column price     = column("PRICE");       // alias → "price"
    // Explicit alias override:
    public final Column vendorRef = column("VENDOR_FK", "vendorId");

    public ProductTable() {
        super("PRODUCT", "p");  // table name, SQL alias
    }
}
```

| Method | Description |
|--------|-------------|
| `column(String columnName)` | Protected. Creates a `Column`; auto-derives camelCase alias |
| `column(String columnName, String alias)` | Protected. Creates a `Column` with an explicit alias |
| `toFromSql()` | Returns `"TABLE_NAME alias"` for the FROM clause |
| `getTableName()` | Returns the raw table name |
| `getAlias()` | Returns the table alias |

**Auto-alias examples:** `PRODUCT_NR` → `productNr`, `FIRST_NAME` → `firstName`, `ID` → `id`.

---

### Column

`ch.lxrin.ql.table.Column`

Strongly-typed column reference created by `TableDef.column(…)`.

| Method | Description |
|--------|-------------|
| `toSql()` | Returns the SQL expression, e.g. `"p.PRODUCT_NR"` |
| `getAlias()` | Returns the Java alias, e.g. `"productNr"` |
| `toString()` | Same as `toSql()` |

Pass a `Column` anywhere a `String` column name is expected in the builder or condition methods.

---

### ISqlExecutor

`ch.lxrin.ql.sql.ISqlExecutor`

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

### Recommended project layout

```
src/main/java/com/example/
├── tables/
│   ├── PersonTable.java      ← extends TableDef
│   ├── OrderTable.java
│   └── ProductTable.java
└── services/
    └── PersonService.java    ← uses LxrinQL
```

### Service method example

Bind parameters inline in the query chain — no separate variable needed:

```java
@Override
public PersonTablePageData getPersonTableData(PersonSearchFormData filter) {
    PersonTablePageData pageData = new PersonTablePageData();
    PersonTable t = new PersonTable();

    LxrinQL.selectInto(pageData)
        .from(t)
        .select(t.personNr)
        .select(t.firstName)
        .select(t.lastName)
        .where(
            eq(t.status, ":status"),
            and(),
            ilike(t.lastName, ":lastName")
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
    CategoryTable c = new CategoryTable();

    List<ILookupRow<Long>> rows = LxrinQL.createContribution(ILookupRow.class)
        .from(c)
        .select(c.categoryId)
        .select(c.name)
        .where(eq(c.active, ":active"), and(), ilike(c.name, ":text"))
        .bind("active", true)
        .bind("text",   "%" + call.getText() + "%")
        .mapWith(row -> new LookupRow<>((Long) row[0], (String) row[1]))
        .multiple();

    setRows(rows);
}
```

### Adding LxrinQL as a JAR to your Scout module

1. Run `mvn install -DskipTests` in the LxrinQL project root.
2. Add the dependency in your Scout server module's `pom.xml` (see Installation section in README).
3. Eclipse Scout RT is already available as `provided` — no extra steps needed.

---

## PostgreSQL Tips

- Use `ilike` for case-insensitive search: `ilike(t.name, ":name")`
- Use `between` for date ranges: `between(t.createdAt, ":from", ":to")`
- For array contains, write a custom condition: `() -> "t.TAGS @> ARRAY[:tag]::text[]"`
- Bind `null` to skip optional filters on the database side (use Scout's `{? … }` syntax in combination)
- Use `new Binds().setDate(…)` / `.setDateTime(…)` inline for proper `LocalDate` / `LocalDateTime` values — Scout's JDBC layer converts them correctly for PostgreSQL

---

## Testing Guide

### Unit testing with Mockito

```java
@Test
void testPersonQuery() {
    ISqlExecutor executor = Mockito.mock(ISqlExecutor.class);
    when(executor.select(anyString(), any()))
        .thenReturn(new Object[][]{{1L, "Alice"}, {2L, "Bob"}});

    PersonTable t = new PersonTable();

    List<String> names = LxrinQL.createContribution(String.class)
        .from(t)
        .select(t.firstName)
        .where(eq(t.status, ":status"))
        .bind("status", "ACTIVE")
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
    PersonTable t = new PersonTable();

    String sql = LxrinQL.createContribution(Object[].class)
        .from(t)
        .select(t.personNr)
        .where(eq(t.status, ":status"))
        .buildSql();

    assertEquals("SELECT t.PERSON_NR FROM PERSON t WHERE t.STATUS = :status", sql);
}
```

### Running the test suite

```bash
mvn test
# 66 tests — no database required
```

---

## FAQ

**Q: Does LxrinQL support INSERT/UPDATE/DELETE?**  
A: `ISqlExecutor.execute()` exists for DML statements, but the fluent builder currently focuses on SELECT. Call `executor.execute(sql, binds.toBindMap())` directly for DML.

**Q: Can I use LxrinQL without Eclipse Scout?**  
A: Yes — implement `ISqlExecutor` yourself to delegate to plain JDBC or any other SQL library.

**Q: Does LxrinQL prevent SQL injection?**  
A: LxrinQL uses Scout's named bind parameters (`:name`), which are parameterized. Column names and table names in `TableDef`, `.from()`, `.join()` are not escaped — never pass user input there.

**Q: How do I bind parameters?**  
A: Use `.bind("name", value)` directly in the query chain — this is the primary pattern:
```java
createContribution(PersonBean.class)
    .from(t)
    .where(eq(t.status, ":status"))
    .bind("status", "ACTIVE")
    .multiple();
```
For typed values (`Long`, `LocalDate`, …), use `new Binds()` inline:
```java
    .bind(new Binds().setLong("personNr", getPersonNr()))
```

**Q: Should I use `Binds` or `BindMap`?**  
A: For everyday queries use `.bind("name", value)` directly in the chain, or `new Binds()` inline when you need typed setters. Use `BindMap` only if you need immutable/copy-on-write semantics (e.g. building shared base queries in a static field).

**Q: How does the auto-alias work in `TableDef`?**  
A: `column("PRODUCT_NR")` splits on `_`, lowercases everything, and capitalises each subsequent word: `product` + `Nr` → `productNr`. Use `column("COLUMN", "myAlias")` to override.

**Q: Can I reuse a partially built query?**  
A: Yes. `BindMap` is copy-on-write, so storing a `QueryBuilder` reference and calling `.bind()` multiple times is safe.

**Q: How do I handle optional filters?**  
A: Build conditions and call `.bind()` conditionally before the terminal method:

```java
PersonTable t = new PersonTable();
var qb = createContribution(PersonBean.class).from(t).select(t.personNr);
List<Condition> conds = new ArrayList<>();

if (status != null) {
    conds.add(eq(t.status, ":status"));
    qb.bind("status", status);
}
if (lastName != null && !lastName.isBlank()) {
    if (!conds.isEmpty()) conds.add(and());
    conds.add(ilike(t.lastName, ":lastName"));
    qb.bind("lastName", "%" + lastName + "%");
}

if (!conds.isEmpty()) qb.where(conds.toArray(new Condition[0]));
List<PersonBean> result = qb.multiple();
```
