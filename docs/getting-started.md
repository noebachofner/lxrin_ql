# Getting Started with LxrinQL

This guide walks you through setting up LxrinQL in an Eclipse Scout Maven project.

---

## Prerequisites

- Java 17 or higher
- Maven 3.8+
- An Eclipse Scout project (version 23.2.0+)

---

## Step 1: Add the dependency

In your Scout server module's `pom.xml`, add:

```xml
<dependency>
    <groupId>com.lxrin</groupId>
    <artifactId>lxrin-ql</artifactId>
    <version>1.0.0</version>
</dependency>
```

Eclipse Scout RT is already present in your Scout project, so the `provided` dependency is automatically satisfied.

---

## Step 2: Static import

In any Scout service class, add:

```java
import static com.lxrin.ql.LxrinQL.*;
```

This single import gives you access to all query-building methods and all condition factories.

---

## Step 3: Write your first query

### SELECT into a table page data

```java
@Override
public PersonTablePageData getPersonTableData(PersonSearchFormData filter) {
    PersonTablePageData pageData = new PersonTablePageData();

    selectInto(pageData)
        .from("PERSON t")
        .select("t.PERSON_ID",  "personId")
        .select("t.FIRST_NAME", "firstName")
        .select("t.LAST_NAME",  "lastName")
        .where(eq("t.STATUS", ":status"))
        .bind("status", "ACTIVE")
        .execute();

    return pageData;
}
```

### SELECT into a typed list

```java
List<PersonBean> people = createContribution(PersonBean.class)
    .from("PERSON t")
    .select("t.PERSON_ID",  "personId")
    .select("t.FIRST_NAME", "firstName")
    .where(eq("t.STATUS", ":status"))
    .bind("status", "ACTIVE")
    .mapWith(row -> {
        PersonBean b = new PersonBean();
        b.setPersonId((Long)   row[0]);
        b.setFirstName((String) row[1]);
        return b;
    })
    .multiple();
```

---

## Step 4: Add conditions

Combine conditions with explicit `and()` / `or()` operators:

```java
.where(
    eq("t.STATUS", ":status"),
    and(),
    ge("t.AGE", ":minAge"),
    and(),
    group(
        isNull("t.DELETED_AT"),
        or(),
        gt("t.DELETED_AT", ":cutoff")
    )
)
```

---

## Step 5: Run the tests

```bash
mvn test
```

All tests use Mockito to mock the SQL executor — no database connection is required.

---

## Next steps

- Read the [API Reference](api-reference.md) for the complete method listing
- See real-world [Examples](examples.md)
- Consult the [WIKI](../WIKI.md) for architecture details and FAQ
