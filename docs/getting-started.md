# Getting Started with LxrinQL

This guide walks you through setting up LxrinQL in an Eclipse Scout Maven project.

---

## Prerequisites

- Java 17 or higher
- Maven 3.8+
- An Eclipse Scout project (version 23.2.0+)

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
    <groupId>com.lxrin</groupId>
    <artifactId>lxrin-ql</artifactId>
    <version>1.0.0</version>
</dependency>
```

Eclipse Scout RT is already present in your Scout project — the `provided` dependency is automatically satisfied.

---

## Step 2: Define your tables

Create one class per database table in a `tables` package. Each class extends `TableDef` and declares its columns as `public final Column` fields.

```java
// src/main/java/com/example/tables/PersonTable.java
import com.lxrin.ql.table.TableDef;
import com.lxrin.ql.table.Column;

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

## Step 3: Static import

In any Scout service class, add:

```java
import static com.lxrin.ql.LxrinQL.*;
```

This single import gives you access to all query-building methods, condition factories, and typed overloads.

---

## Step 4: Build your binds

Use `Binds` for typed, named parameters:

```java
import com.lxrin.ql.bind.Binds;

Binds b = new Binds()
    .setString("status", "ACTIVE")
    .setInt("minAge", 18);
```

---

## Step 5: Write your first query

### SELECT into a table page data

```java
@Override
public PersonTablePageData getPersonTableData(PersonSearchFormData filter) {
    PersonTablePageData pageData = new PersonTablePageData();
    PersonTable t = new PersonTable();

    Binds b = new Binds()
        .setString("status",   "ACTIVE")
        .setString("lastName", "%" + filter.getLastName().getValue() + "%");

    selectInto(pageData)
        .from(t)
        .select(t.personNr)
        .select(t.firstName)
        .select(t.lastName)
        .where(eq(t.status, ":status"), and(), ilike(t.lastName, ":lastName"))
        .bind(b)
        .execute();

    return pageData;
}
```

Generated SQL:
```sql
SELECT t.PERSON_NR, t.FIRST_NAME, t.LAST_NAME
FROM PERSON t
WHERE t.STATUS = :status AND t.LAST_NAME ILIKE :lastName
INTO :personNr, :firstName, :lastName
```

### SELECT into a typed list

```java
PersonTable t = new PersonTable();

Binds b = new Binds().setString("status", "ACTIVE");

List<PersonBean> people = createContribution(PersonBean.class)
    .from(t)
    .select(t.personNr)
    .select(t.firstName)
    .where(eq(t.status, ":status"))
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

.where(
    eq(t.status, ":status"),
    and(),
    ge(t.age, ":minAge"),
    and(),
    group(
        isNull(t.deletedAt),
        or(),
        gt(t.deletedAt, ":cutoff")
    )
)
```

---

## Step 7: Run the tests

```bash
mvn test
```

All 66 tests use Mockito to mock the SQL executor — no database connection is required.

---

## Next steps

- Read the [API Reference](api-reference.md) for the complete method listing
- See real-world [Examples](examples.md)
- Consult the [WIKI](../WIKI.md) for architecture details and FAQ
