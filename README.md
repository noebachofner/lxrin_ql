# LxrinQL

> **AI-generated** fluent query-builder library for [Eclipse Scout](https://eclipsescout.github.io/) and PostgreSQL.

[![Java](https://img.shields.io/badge/Java-17-blue?logo=java)](https://adoptium.net/)
[![Maven](https://img.shields.io/badge/Maven-3.9-orange?logo=apache-maven)](https://maven.apache.org/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

---

## Overview

**LxrinQL** is a lightweight, fluent Java library that wraps Eclipse Scout's `SQL` service with a readable, type-safe query-builder API. Instead of concatenating SQL strings manually, you express queries as method chains — with strongly-typed table and column definitions — and let LxrinQL generate the SQL for you.

> ⚠️ This project is AI-generated and intended as a learning/prototype tool.

---

## Features

- ✅ **Typed table definitions** — define tables and columns as Java classes; use `products.productNr` instead of `"p.PRODUCT_NR"`
- ✅ **Typed `Binds` class** — `setLong`, `setString`, `setInt`, `setBoolean`, `setDate`, … no more raw `Object` casts
- ✅ Fluent `SELECT` builder (`QueryBuilder`) with `.single()` / `.multiple()`
- ✅ Fluent `SELECT … INTO` builder (`SelectIntoBuilder`) for Scout table page data
- ✅ Full condition API: `eq`, `ne`, `gt`, `lt`, `ge`, `le`, `like`, `ilike`, `in`, `between`, `isNull`, `isNotNull`, `not`, `group` — all accept both `String` and `Column`
- ✅ Low-level `BindMap` for functional / copy-on-write scenarios
- ✅ Pluggable `ISqlExecutor` for easy unit testing with mocks
- ✅ Zero runtime dependencies beyond Eclipse Scout RT

---

## Requirements

| Dependency       | Version  |
|-----------------|---------|
| Java             | 17+     |
| Eclipse Scout RT | 23.2.0+ |
| Maven            | 3.8+    |

---

## Installation

### 1. Build the JAR locally

```bash
git clone https://github.com/noebachofner/lxrin_ql.git
cd lxrin_ql
mvn install -DskipTests
```

### 2. Add to your Scout server module

```xml
<dependency>
    <groupId>ch.lxrin</groupId>
    <artifactId>lxrin-ql</artifactId>
    <version>1.0.0</version>
</dependency>
```

Eclipse Scout RT is already provided by your Scout project — no extra dependency needed.

---

## Quick Start

### Step 1 — Define your table once

```java
// src/main/java/com/example/tables/PersonTable.java
import ch.lxrin.ql.table.TableDef;
import ch.lxrin.ql.table.Column;

public class PersonTable extends TableDef {
    public final Column personNr   = column("PERSON_NR");
    public final Column firstName  = column("FIRST_NAME");
    public final Column lastName   = column("LAST_NAME");
    public final Column status     = column("STATUS");
    public final Column age        = column("AGE");

    public PersonTable() {
        super("PERSON", "t");   // table name, alias
    }
}
```

Column aliases are auto-derived: `FIRST_NAME` → `firstName`, `PERSON_NR` → `personNr`.

### Step 2 — Query with typed columns and inline binds

Bind parameters are set **directly inside the query chain** using `.bind("name", value)`:

```java
import static ch.lxrin.ql.LxrinQL.*;

PersonTable t = new PersonTable();

List<PersonBean> people = createContribution(PersonBean.class)
    .from(t)
    .select(t.personNr)
    .select(t.firstName)
    .select(t.lastName)
    .join("LEFT JOIN ADDRESS a ON a.PERSON_NR = t.PERSON_NR")
    .where(eq(t.status, ":status"), and(), ge(t.age, ":minAge"))
    .bind("status", "ACTIVE")
    .bind("minAge", 18)
    .mapWith(row -> {
        PersonBean p = new PersonBean();
        p.setPersonNr((Long)   row[0]);
        p.setFirstName((String) row[1]);
        p.setLastName((String)  row[2]);
        return p;
    })
    .multiple();
```

### Step 3 — Populate Eclipse Scout table page data

```java
PersonTable t = new PersonTable();

selectInto(personTablePageData)
    .from(t)
    .select(t.personNr)
    .select(t.firstName)
    .select(t.lastName)
    .where(eq(t.status, ":status"))
    .bind("status", "ACTIVE")
    .execute();
// Generated: SELECT t.PERSON_NR, t.FIRST_NAME, t.LAST_NAME
//            FROM PERSON t
//            WHERE t.STATUS = :status
//            INTO :personNr, :firstName, :lastName
```

### Fetching a single scalar value

```java
Long count = createContribution(Long.class)
    .from("PERSON t")
    .select("COUNT(*)", "cnt")
    .where(eq("t.STATUS", ":status"))
    .bind("status", "ACTIVE")
    .single();
```

---

## Bind Parameters

Add named bind parameters directly in the query chain with `.bind("name", value)`:

```java
createContribution(PersonBean.class)
    .from(t)
    .select(t.personNr)
    .where(eq(t.status, ":status"), and(), ge(t.age, ":minAge"))
    .bind("status", "ACTIVE")     // String
    .bind("minAge", 18)           // Integer
    .multiple();
```

When you need typed setters (e.g. for `Long`, `LocalDate`), use `new Binds()` inline in the chain — no intermediate variable needed:

```java
createContribution(PersonBean.class)
    .from(t)
    .select(t.personNr)
    .where(eq(t.personNr, ":personNr"), and(), eq(t.status, ":status"))
    .bind(new Binds()
        .setLong("personNr", getPersonNr())
        .setString("status", "ACTIVE"))
    .multiple();
```

| `Binds` method | Type |
|----------------|------|
| `setLong(name, Long)` | `Long` |
| `setInt(name, Integer)` | `Integer` |
| `setDouble(name, Double)` | `Double` |
| `setBigDecimal(name, BigDecimal)` | `BigDecimal` |
| `setString(name, String)` | `String` |
| `setBoolean(name, Boolean)` | `Boolean` |
| `setDate(name, LocalDate)` | `LocalDate` |
| `setDateTime(name, LocalDateTime)` | `LocalDateTime` |
| `set(name, Object)` | generic fallback |

---

## `TableDef` — Typed Table Definitions

Define a class per database table. Columns are declared as `public final Column` fields.

```java
public class OrderTable extends TableDef {
    public final Column orderId    = column("ORDER_ID");
    public final Column customerId = column("CUSTOMER_ID");
    public final Column total      = column("TOTAL");
    public final Column status     = column("STATUS");
    public final Column createdAt  = column("CREATED_AT");

    public OrderTable() {
        super("ORDERS", "o");
    }
}
```

Use it in a query:

```java
OrderTable o = new OrderTable();

List<OrderBean> orders = createContribution(OrderBean.class)
    .from(o)
    .select(o.orderId)
    .select(o.total)
    .where(eq(o.status, ":status"))
    .bind("status", "ACTIVE")
    .mapWith(row -> new OrderBean((Long) row[0], (Double) row[1]))
    .multiple();
```

Conditions accept both `Column` and `String`:
```java
.where(eq(o.status, ":status"))       // Column overload
.where(eq("o.STATUS", ":status"))     // String overload (still works)
```

---

## Condition Reference

All conditions work with both `String` column names and `Column` objects.

| Method | SQL Output |
|--------|-----------|
| `eq(col, ":val")` | `col = :val` |
| `ne(col, ":val")` | `col <> :val` |
| `gt(col, ":val")` | `col > :val` |
| `lt(col, ":val")` | `col < :val` |
| `ge(col, ":val")` | `col >= :val` |
| `le(col, ":val")` | `col <= :val` |
| `like(col, ":val")` | `col LIKE :val` |
| `ilike(col, ":val")` | `col ILIKE :val` |
| `in(col, ":v1", ":v2")` | `col IN (:v1, :v2)` |
| `between(col, ":from", ":to")` | `col BETWEEN :from AND :to` |
| `isNull(col)` | `col IS NULL` |
| `isNotNull(col)` | `col IS NOT NULL` |
| `not(eq(col, ":val"))` | `NOT (col = :val)` |
| `group(eq(col,":x"), or(), gt(col2,":y"))` | `(col = :x OR col2 > :y)` |
| `and()` | `AND` |
| `or()` | `OR` |

---

## Testing

```bash
mvn test
```

Tests use Mockito to mock `ISqlExecutor` — no database required.  
66 tests covering `QueryBuilder`, `SelectIntoBuilder`, `Conditions`, `Binds`, and `TableDef`.

---

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/my-feature`)
3. Commit your changes
4. Open a pull request

---

## Documentation

| Document | Description |
|----------|-------------|
| [WIKI.md](WIKI.md) | Architecture, full API reference, integration guide, FAQ |
| [Getting Started](docs/getting-started.md) | Step-by-step setup guide |
| [API Reference](docs/api-reference.md) | Complete method listing |
| [Examples](docs/examples.md) | Real-world usage patterns |

---

## License

This project is licensed under the [MIT License](LICENSE).
