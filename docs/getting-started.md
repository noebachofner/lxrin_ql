# Getting Started with LxrinQL

This guide walks you through setting up LxrinQL in an Eclipse Scout Maven project.

---

## Prerequisites

- Java 17 or higher
- Maven 3.8+
- An Eclipse Scout project (version 23.2.0+)
- IntelliJ IDEA (for the optional `qlid` live template)

---

## Step 1: Build and install the library

Clone the repository and install the JAR to your local Maven cache:

```bash
git clone https://github.com/noebachofner/lxrin_ql.git
cd lxrin_ql
mvn install -DskipTests
```

Then add the dependency to your Scout server module's `pom.xml`:

```xml
<dependency>
    <groupId>ch.lxrin</groupId>
    <artifactId>lxrin-ql</artifactId>
    <version>1.0.0</version>
</dependency>
```

Eclipse Scout RT is already present in your Scout project — the `provided` dependency is automatically satisfied.

---

## Step 2: (Optional) Install the `qlid` IntelliJ live template

LxrinQL ships a live template that inserts a persisted auto-incrementing `long`
ID every time you type `qlid` + **Ctrl+Space**.  The counter starts at **1000**
and survives IDE restarts.  Perfect for Eclipse Scout `CodeType` IDs.

1. In IntelliJ: **File → Manage IDE Settings → Import Settings**
2. Select **`live-templates/LxrinQL.xml`** from this repository
3. Make sure *Live templates* is checked → click **OK**

```java
public class PersonStatusCodeType extends AbstractCodeType<Long, String> {

    public static final long ID = 1000L;   // ← typed "qlid" Ctrl+Space

    public static class ActiveCode extends AbstractCode<String> {
        public static final long ID = 1001L;   // ← next "qlid"
    }

    public static class InactiveCode extends AbstractCode<String> {
        public static final long ID = 1002L;   // ← next "qlid"
    }
}
```

---

## Step 3: Define your tables

Create one class per database table in a `tables` package. Each class extends `TableDef` and declares its columns as `public final Column` fields.

```java
// src/main/java/com/example/tables/PersonTable.java
import ch.lxrin.ql.table.TableDef;
import ch.lxrin.ql.table.Column;

public class PersonTable extends TableDef {

    public final Column personNr  = column("PERSON_NR");   // alias → "personNr"
    public final Column firstName = column("FIRST_NAME");  // alias → "firstName"
    public final Column lastName  = column("LAST_NAME");   // alias → "lastName"
    public final Column status    = column("STATUS");      // alias → "status"
    public final Column age       = column("AGE");         // alias → "age"

    public PersonTable() {
        super("PERSON", "t");  // SQL table name, alias used in queries
    }
}
```

Column aliases are derived automatically from `UPPER_SNAKE_CASE` → `lowerCamelCase`.  
Use `column("COLUMN_NAME", "myAlias")` to override.

---

## Step 4: Static import

In any Scout service class, add:

```java
import static ch.lxrin.ql.LxrinQL.*;
```

This single import gives you access to all query-building methods, condition factories, and typed overloads.

---

## Step 5: Write your first query

Create a `Binds b = new Binds()` and call the **single-argument typed setters directly inside the condition** — the name is auto-generated and the `:placeholder` is returned inline:

### SELECT into a table page data

```java
@Override
public PersonTablePageData getPersonTableData(PersonSearchFormData filter) {
    PersonTablePageData pageData = new PersonTablePageData();
    PersonTable t = new PersonTable();
    Binds b = new Binds();

    selectInto(pageData)
        .from(t)
        .select(t.personNr)
        .select(t.firstName)
        .select(t.lastName)
        .where(eq(t.status, b.setString("ACTIVE")),
               and(),
               ilike(t.lastName, b.setString("%" + filter.getLastName().getValue() + "%")))
        .bind(b)
        .execute();

    return pageData;
}
```

Generated SQL:
```sql
SELECT t.PERSON_NR, t.FIRST_NAME, t.LAST_NAME
FROM PERSON t
WHERE t.STATUS = :p0 AND t.LAST_NAME ILIKE :p1
INTO :personNr, :firstName, :lastName
```

### SELECT into a typed list

```java
PersonTable t = new PersonTable();
Binds b = new Binds();

List<PersonBean> people = createContribution(PersonBean.class)
    .from(t)
    .select(t.personNr)
    .select(t.firstName)
    .where(eq(t.personNr, b.setLong(getPersonNr())),
           and(),
           eq(t.status,   b.setString("ACTIVE")))
    .bind(b)
    .mapWith(row -> {
        PersonBean bean = new PersonBean();
        bean.setPersonNr((Long)   row[0]);
        bean.setFirstName((String) row[1]);
        return bean;
    })
    .multiple();
```

---

## Step 6: Add conditions

Combine conditions with explicit `and()` / `or()` operators:

```java
PersonTable t = new PersonTable();
Binds b = new Binds();

createContribution(PersonBean.class)
    .from(t)
    .select(t.personNr)
    .where(
        eq(t.status,  b.setString("ACTIVE")),
        and(),
        ge(t.age,     b.setInt(18)),
        and(),
        group(
            isNull(t.deletedAt),
            or(),
            gt(t.deletedAt, b.setString("2024-01-01"))
        )
    )
    .bind(b)
    .multiple();
```

---

## Step 7: Run the tests

```bash
mvn test
```

All 77 tests use Mockito to mock the SQL executor — no database connection is required.

---

## Next steps

- Read the [API Reference](api-reference.md) for the complete method listing
- See real-world [Examples](examples.md)
- Consult the [WIKI](../WIKI.md) for architecture details and FAQ
