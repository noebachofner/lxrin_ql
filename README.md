# LxrinQL

> **AI-generated** fluent query-builder library for [Eclipse Scout](https://eclipsescout.github.io/) and PostgreSQL.

[![Java](https://img.shields.io/badge/Java-17-blue?logo=java)](https://adoptium.net/)
[![Maven](https://img.shields.io/badge/Maven-3.9-orange?logo=apache-maven)](https://maven.apache.org/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

---

## Overview

**LxrinQL** is a lightweight, fluent Java library that wraps Eclipse Scout's `SQL` service with a readable, type-safe query-builder API. Instead of concatenating SQL strings manually, you express queries as method chains and let LxrinQL generate the SQL for you.

> ⚠️ This project is AI-generated and intended as a learning/prototype tool.

---

## Features

- ✅ Fluent `SELECT` builder (`QueryBuilder`)
- ✅ Fluent `SELECT … INTO` builder (`SelectIntoBuilder`) for Scout table page data
- ✅ Full condition API: `eq`, `ne`, `gt`, `lt`, `ge`, `le`, `like`, `ilike`, `in`, `between`, `isNull`, `isNotNull`, `not`, `group`
- ✅ Named bind parameters via `BindMap`
- ✅ Pluggable `ISqlExecutor` for easy unit testing with mocks
- ✅ Zero runtime dependencies beyond Eclipse Scout RT

---

## Requirements

| Dependency           | Version    |
|----------------------|-----------|
| Java                 | 17+       |
| Eclipse Scout RT     | 23.2.0+   |
| Maven                | 3.8+      |

---

## Installation

Add to your `pom.xml`:

```xml
<dependency>
    <groupId>com.lxrin</groupId>
    <artifactId>lxrin-ql</artifactId>
    <version>1.0.0</version>
</dependency>
```

Eclipse Scout RT must be on the classpath (provided by your host application):

```xml
<dependency>
    <groupId>org.eclipse.scout.rt</groupId>
    <artifactId>org.eclipse.scout.rt.server.jdbc</artifactId>
    <version>23.2.0</version>
    <scope>provided</scope>
</dependency>
```

---

## Quick Start

### Fetching a list of beans

```java
import static com.lxrin.ql.LxrinQL.*;

List<PersonBean> people = createContribution(PersonBean.class)
    .from("PERSON t")
    .select("t.ID",         "id")
    .select("t.FIRST_NAME", "firstName")
    .select("t.LAST_NAME",  "lastName")
    .join("LEFT JOIN ADDRESS a ON a.PERSON_ID = t.ID")
    .where(eq("t.STATUS", ":status"), and(), ge("t.AGE", ":minAge"))
    .bind("status", "ACTIVE")
    .bind("minAge", 18)
    .mapWith(row -> {
        PersonBean p = new PersonBean();
        p.setId((Long)   row[0]);
        p.setFirstName((String) row[1]);
        p.setLastName((String)  row[2]);
        return p;
    })
    .multiple();
```

### Fetching a single value

```java
Long count = createContribution(Long.class)
    .from("PERSON t")
    .select("COUNT(*)", "cnt")
    .where(eq("t.STATUS", ":status"))
    .bind("status", "ACTIVE")
    .single();
```

### Eclipse Scout `selectInto` (table page data)

```java
selectInto(myTablePageData)
    .from("PERSON t")
    .select("t.ID",         "id")
    .select("t.FIRST_NAME", "firstName")
    .where(eq("t.STATUS", ":status"))
    .bind("status", "ACTIVE")
    .execute();
```

---

## Condition Reference

| Method                                    | SQL Output                  |
|-------------------------------------------|-----------------------------|
| `eq("col", ":val")`                       | `col = :val`                |
| `ne("col", ":val")`                       | `col <> :val`               |
| `gt("col", ":val")`                       | `col > :val`                |
| `lt("col", ":val")`                       | `col < :val`                |
| `ge("col", ":val")`                       | `col >= :val`               |
| `le("col", ":val")`                       | `col <= :val`               |
| `like("col", ":val")`                     | `col LIKE :val`             |
| `ilike("col", ":val")`                    | `col ILIKE :val`            |
| `in("col", ":v1", ":v2")`                 | `col IN (:v1, :v2)`         |
| `between("col", ":from", ":to")`          | `col BETWEEN :from AND :to` |
| `isNull("col")`                           | `col IS NULL`               |
| `isNotNull("col")`                        | `col IS NOT NULL`           |
| `not(eq("col", ":val"))`                  | `NOT (col = :val)`          |
| `group(eq("a",":x"), or(), gt("b",":y"))` | `(a = :x OR b > :y)`        |
| `and()`                                   | `AND`                       |
| `or()`                                    | `OR`                        |

---

## Testing

```bash
mvn test
```

Tests use Mockito to mock `ISqlExecutor` — no database required.

---

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/my-feature`)
3. Commit your changes
4. Open a pull request

---

## License

This project is licensed under the [MIT License](LICENSE).
