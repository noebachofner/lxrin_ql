# LxrinQL Examples

Real-world usage examples for common Eclipse Scout patterns.

> All examples assume `import static ch.lxrin.ql.LxrinQL.*;`
>
> **Key principle:** bind parameters are set directly inside the query chain using `.bind("name", value)`.
> No intermediate `Binds` variable is needed for the common case.

---

## Table definitions used in these examples

```java
// OrderTable.java
public class OrderTable extends TableDef {
    public final Column orderId    = column("ORDER_ID");
    public final Column customerId = column("CUSTOMER_ID");
    public final Column total      = column("TOTAL");
    public final Column status     = column("STATUS");
    public final Column createdAt  = column("CREATED_AT");
    public OrderTable() { super("ORDERS", "o"); }
}

// CustomerTable.java
public class CustomerTable extends TableDef {
    public final Column customerId = column("CUSTOMER_ID");
    public final Column firstName  = column("FIRST_NAME");
    public final Column lastName   = column("LAST_NAME");
    public final Column email      = column("EMAIL");
    public CustomerTable() { super("CUSTOMER", "c"); }
}

// ProductTable.java
public class ProductTable extends TableDef {
    public final Column productId  = column("PRODUCT_ID");
    public final Column name       = column("NAME");
    public final Column price      = column("PRICE");
    public final Column category   = column("CATEGORY");
    public final Column featuredAt = column("FEATURED_AT");
    public ProductTable() { super("PRODUCT", "p"); }
}

// CategoryTable.java
public class CategoryTable extends TableDef {
    public final Column categoryId = column("CATEGORY_ID");
    public final Column name       = column("NAME");
    public final Column active     = column("ACTIVE");
    public CategoryTable() { super("CATEGORY", "c"); }
}

// TagTable.java
public class TagTable extends TableDef {
    public final Column tagId = column("TAG_ID");
    public final Column name  = column("NAME");
    public TagTable() { super("TAG", "t"); }
}
```

---

## 1. Table page data with `selectInto`

Bind values inline — no separate variable:

```java
@Override
public OrderTablePageData getOrderTableData(OrderSearchFormData filter) {
    OrderTablePageData pageData = new OrderTablePageData();
    OrderTable o = new OrderTable();

    selectInto(pageData)
        .from(o)
        .select(o.orderId)
        .select(o.customerId)
        .select(o.total)
        .select(o.status)
        .select(o.createdAt)
        .join("LEFT JOIN CUSTOMER c ON c.CUSTOMER_ID = o.CUSTOMER_ID")
        .where(
            eq(o.status, ":status"),
            and(),
            between(o.createdAt, ":from", ":to")
        )
        .bind("status", filter.getStatus().getValue())
        .bind("from",   filter.getDateFrom().getValue())
        .bind("to",     filter.getDateTo().getValue())
        .execute();

    return pageData;
}
```

Generated SQL:
```sql
SELECT o.ORDER_ID, o.CUSTOMER_ID, o.TOTAL, o.STATUS, o.CREATED_AT
FROM ORDERS o
LEFT JOIN CUSTOMER c ON c.CUSTOMER_ID = o.CUSTOMER_ID
WHERE o.STATUS = :status AND o.CREATED_AT BETWEEN :from AND :to
INTO :orderId, :customerId, :total, :status, :createdAt
```

---

## 2. Fetch a single bean

```java
public CustomerBean findCustomer(Long customerId) {
    CustomerTable c = new CustomerTable();

    return createContribution(CustomerBean.class)
        .from(c)
        .select(c.customerId)
        .select(c.firstName)
        .select(c.lastName)
        .select(c.email)
        .where(eq(c.customerId, ":customerId"))
        .bind("customerId", customerId)
        .mapWith(row -> {
            CustomerBean bean = new CustomerBean();
            bean.setCustomerId((Long)   row[0]);
            bean.setFirstName((String)  row[1]);
            bean.setLastName((String)   row[2]);
            bean.setEmail((String)      row[3]);
            return bean;
        })
        .single();
}
```

---

## 3. Count query

```java
public long countActiveOrders() {
    OrderTable o = new OrderTable();

    Long count = createContribution(Long.class)
        .from(o)
        .select("COUNT(*)", "cnt")
        .where(eq(o.status, ":status"))
        .bind("status", "ACTIVE")
        .single();

    return count != null ? count : 0L;
}
```

---

## 4. IN condition with multiple statuses

```java
OrderTable o = new OrderTable();

List<OrderBean> orders = createContribution(OrderBean.class)
    .from(o)
    .select(o.orderId)
    .select(o.status)
    .where(in(o.status, "'PENDING'", "'PROCESSING'", "'SHIPPED'"))
    .mapWith(row -> new OrderBean((Long) row[0], (String) row[1]))
    .multiple();
```

---

## 5. Complex grouped conditions (OR logic)

```java
ProductTable p = new ProductTable();

List<ProductBean> results = createContribution(ProductBean.class)
    .from(p)
    .select(p.productId)
    .select(p.name)
    .select(p.price)
    .where(
        group(
            le(p.price, ":maxPrice"),
            and(),
            eq(p.category, ":cat")
        ),
        or(),
        group(
            ilike(p.name, ":search"),
            and(),
            isNotNull(p.featuredAt)
        )
    )
    .bind("maxPrice", 99.99)
    .bind("cat",      "ELECTRONICS")
    .bind("search",   "%laptop%")
    .mapWith(row -> new ProductBean((Long) row[0], (String) row[1], (Double) row[2]))
    .multiple();
```

---

## 6. Lookup call

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

---

## 7. Optional filters (dynamic WHERE)

```java
public List<CustomerBean> searchCustomers(String lastName, String status) {
    CustomerTable c = new CustomerTable();

    var builder = createContribution(CustomerBean.class)
        .from(c)
        .select(c.customerId)
        .select(c.firstName)
        .select(c.lastName);

    List<Condition> conds = new ArrayList<>();

    if (status != null) {
        conds.add(eq(c.status, ":status"));
        builder.bind("status", status);
    }
    if (lastName != null && !lastName.isBlank()) {
        if (!conds.isEmpty()) conds.add(and());
        conds.add(ilike(c.lastName, ":lastName"));
        builder.bind("lastName", "%" + lastName + "%");
    }

    if (!conds.isEmpty()) {
        builder.where(conds.toArray(new Condition[0]));
    }

    return builder
        .mapWith(row -> new CustomerBean((Long) row[0], (String) row[1], (String) row[2]))
        .multiple();
}
```

---

## 8. Custom condition (lambda)

```java
TagTable t = new TagTable();

Condition startsWithPrefix = () -> "LOWER(t.NAME) LIKE LOWER(:prefix) || '%'";

List<TagBean> tags = createContribution(TagBean.class)
    .from(t)
    .select(t.tagId)
    .select(t.name)
    .where(startsWithPrefix)
    .bind("prefix", "java")
    .mapWith(row -> new TagBean((Long) row[0], (String) row[1]))
    .multiple();
```

---

## 9. Typed binds with `new Binds()` — inline, no variable

When you need typed setters (e.g. `Long`, `LocalDate`), construct `new Binds()` inline in the chain:

```java
PersonTable t = new PersonTable();

List<PersonBean> people = createContribution(PersonBean.class)
    .from(t)
    .select(t.personNr)
    .select(t.firstName)
    .where(eq(t.personNr, ":personNr"), and(), ge(t.age, ":minAge"))
    .bind(new Binds()
        .setLong("personNr", getPersonNr())
        .setInt("minAge",    18))
    .mapWith(row -> new PersonBean((Long) row[0], (String) row[1]))
    .multiple();
```

All typed setters: `setLong`, `setInt`, `setDouble`, `setBigDecimal`, `setString`, `setBoolean`, `setDate`, `setDateTime`.

---

## 10. Unit test with mocked executor

```java
@Test
void testFindCustomer() {
    ISqlExecutor executor = Mockito.mock(ISqlExecutor.class);
    when(executor.select(anyString(), any()))
        .thenReturn(new Object[][]{{42L, "Alice", "Smith", "alice@example.com"}});

    CustomerTable c = new CustomerTable();

    CustomerBean customer = createContribution(CustomerBean.class)
        .from(c)
        .select(c.customerId)
        .select(c.firstName)
        .select(c.lastName)
        .select(c.email)
        .where(eq(c.customerId, ":customerId"))
        .bind("customerId", 42L)
        .executor(executor)
        .mapWith(row -> {
            CustomerBean bean = new CustomerBean();
            bean.setCustomerId((Long)  row[0]);
            bean.setFirstName((String) row[1]);
            bean.setLastName((String)  row[2]);
            bean.setEmail((String)     row[3]);
            return bean;
        })
        .single();

    assertNotNull(customer);
    assertEquals(42L,     customer.getCustomerId());
    assertEquals("Alice", customer.getFirstName());
}
```

---

## 11. Verify generated SQL in a unit test

```java
@Test
void testGeneratedSql() {
    OrderTable o = new OrderTable();

    String sql = createContribution(Object[].class)
        .from(o)
        .select(o.orderId)
        .select(o.status)
        .where(eq(o.status, ":status"), and(), gt(o.total, ":minTotal"))
        .buildSql();

    assertEquals(
        "SELECT o.ORDER_ID, o.STATUS FROM ORDERS o " +
        "WHERE o.STATUS = :status AND o.TOTAL > :minTotal",
        sql
    );
}
```
