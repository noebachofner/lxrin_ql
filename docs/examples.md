# LxrinQL Examples

Real-world usage examples for common Eclipse Scout patterns.

> All examples assume `import static com.lxrin.ql.LxrinQL.*;`

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
    public final Column productId = column("PRODUCT_ID");
    public final Column name      = column("NAME");
    public final Column price     = column("PRICE");
    public final Column category  = column("CATEGORY");
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

## 1. Table page data with `selectInto` + `TableDef` + `Binds`

```java
@Override
public OrderTablePageData getOrderTableData(OrderSearchFormData filter) {
    OrderTablePageData pageData = new OrderTablePageData();
    OrderTable o = new OrderTable();

    Binds b = new Binds()
        .setString("status", filter.getStatus().getValue())
        .setDate("from",     filter.getDateFrom().getValue())
        .setDate("to",       filter.getDateTo().getValue());

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
        .bind(b)
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

## 2. Fetch a single bean with typed columns and binds

```java
public CustomerBean findCustomer(Long customerId) {
    CustomerTable c = new CustomerTable();

    Binds b = new Binds().setLong("customerId", customerId);

    return createContribution(CustomerBean.class)
        .from(c)
        .select(c.customerId)
        .select(c.firstName)
        .select(c.lastName)
        .select(c.email)
        .where(eq(c.customerId, ":customerId"))
        .bind(b)
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

    Binds b = new Binds().setString("status", "ACTIVE");

    Long count = createContribution(Long.class)
        .from(o)
        .select("COUNT(*)", "cnt")
        .where(eq(o.status, ":status"))
        .bind(b)
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

Binds b = new Binds()
    .setDouble("maxPrice", 99.99)
    .setString("cat",      "ELECTRONICS")
    .setString("search",   "%laptop%");

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
    .bind(b)
    .mapWith(row -> new ProductBean((Long) row[0], (String) row[1], (Double) row[2]))
    .multiple();
```

---

## 6. Lookup call with `TableDef` and `Binds`

```java
@Override
protected void execLoadData(ILookupCall<Long> call) {
    CategoryTable c = new CategoryTable();

    Binds b = new Binds()
        .setBoolean("active", true)
        .setString("text",    "%" + call.getText() + "%");

    List<ILookupRow<Long>> rows = LxrinQL.createContribution(ILookupRow.class)
        .from(c)
        .select(c.categoryId)
        .select(c.name)
        .where(eq(c.active, ":active"), and(), ilike(c.name, ":text"))
        .bind(b)
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

    Binds b = new Binds();
    List<Condition> conds = new ArrayList<>();

    if (status != null) {
        conds.add(eq(c.status, ":status"));
        b.setString("status", status);
    }
    if (lastName != null && !lastName.isBlank()) {
        if (!conds.isEmpty()) conds.add(and());
        conds.add(ilike(c.lastName, ":lastName"));
        b.setString("lastName", "%" + lastName + "%");
    }

    if (!conds.isEmpty()) {
        builder.where(conds.toArray(new Condition[0]));
    }

    return builder
        .bind(b)
        .mapWith(row -> new CustomerBean((Long) row[0], (String) row[1], (String) row[2]))
        .multiple();
}
```

---

## 8. Custom condition (lambda)

```java
TagTable t = new TagTable();

// Match records where the name starts with a prefix, case-insensitively
Condition startsWithPrefix = () -> "LOWER(t.NAME) LIKE LOWER(:prefix) || '%'";

Binds b = new Binds().setString("prefix", "java");

List<TagBean> tags = createContribution(TagBean.class)
    .from(t)
    .select(t.tagId)
    .select(t.name)
    .where(startsWithPrefix)
    .bind(b)
    .mapWith(row -> new TagBean((Long) row[0], (String) row[1]))
    .multiple();
```

---

## 9. Using `Binds` — all typed setters

```java
Binds b = new Binds()
    .setLong("id",          42L)
    .setInt("count",        10)
    .setDouble("rate",      0.05)
    .setBigDecimal("price", new BigDecimal("19.99"))
    .setString("name",      "Alice")
    .setBoolean("active",   true)
    .setDate("since",       LocalDate.of(2024, 1, 1))
    .setDateTime("before",  LocalDateTime.now());
```

---

## 10. Unit test with mocked executor

```java
@Test
void testFindCustomer() {
    ISqlExecutor executor = Mockito.mock(ISqlExecutor.class);
    when(executor.select(anyString(), any()))
        .thenReturn(new Object[][]{{42L, "Alice", "Smith", "alice@example.com"}});

    CustomerTable c = new CustomerTable();
    Binds b = new Binds().setLong("customerId", 42L);

    CustomerBean customer = createContribution(CustomerBean.class)
        .from(c)
        .select(c.customerId)
        .select(c.firstName)
        .select(c.lastName)
        .select(c.email)
        .where(eq(c.customerId, ":customerId"))
        .bind(b)
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
