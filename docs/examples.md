# LxrinQL Examples

Real-world usage examples for common Eclipse Scout patterns.

> All examples assume `import static ch.lxrin.ql.LxrinQL.*;`
>
> **Key principle:** create one `Binds b = new Binds()` per query and call
> the single-argument typed setters **directly inside your condition expressions**.
> `b.setLong(value)` auto-names the parameter and returns the `:placeholder`
> string that the condition consumes.  Then pass `.bind(b)` once to the builder.

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

```java
@Override
public OrderTablePageData getOrderTableData(OrderSearchFormData filter) {
    OrderTablePageData pageData = new OrderTablePageData();
    OrderTable o = new OrderTable();
    Binds b = new Binds();

    selectInto(pageData)
        .from(o)
        .select(o.orderId)
        .select(o.customerId)
        .select(o.total)
        .select(o.status)
        .select(o.createdAt)
        .join("LEFT JOIN CUSTOMER c ON c.CUSTOMER_ID = o.CUSTOMER_ID")
        .where(
            eq(o.status,    b.setString(filter.getStatus().getValue())),
            and(),
            between(o.createdAt, b.setDate(filter.getDateFrom().getValue()),
                                 b.setDate(filter.getDateTo().getValue()))
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
WHERE o.STATUS = :p0 AND o.CREATED_AT BETWEEN :p1 AND :p2
INTO :orderId, :customerId, :total, :status, :createdAt
```

---

## 2. Fetch a single bean

```java
public CustomerBean findCustomer(Long customerId) {
    CustomerTable c = new CustomerTable();
    Binds b = new Binds();

    return createContribution(CustomerBean.class)
        .from(c)
        .select(c.customerId)
        .select(c.firstName)
        .select(c.lastName)
        .select(c.email)
        .where(eq(c.customerId, b.setLong(customerId)))
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
    Binds b = new Binds();

    Long count = createContribution(Long.class)
        .from(o)
        .select("COUNT(*)", "cnt")
        .where(eq(o.status, b.setString("ACTIVE")))
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
Binds b = new Binds();

List<ProductBean> results = createContribution(ProductBean.class)
    .from(p)
    .select(p.productId)
    .select(p.name)
    .select(p.price)
    .where(
        group(
            le(p.price,    b.setDouble(99.99)),
            and(),
            eq(p.category, b.setString("ELECTRONICS"))
        ),
        or(),
        group(
            ilike(p.name, b.setString("%laptop%")),
            and(),
            isNotNull(p.featuredAt)
        )
    )
    .bind(b)
    .mapWith(row -> new ProductBean((Long) row[0], (String) row[1], (Double) row[2]))
    .multiple();
```

---

## 6. Lookup call

```java
@Override
protected void execLoadData(ILookupCall<Long> call) {
    CategoryTable c = new CategoryTable();
    Binds b = new Binds();

    List<ILookupRow<Long>> rows = LxrinQL.createContribution(ILookupRow.class)
        .from(c)
        .select(c.categoryId)
        .select(c.name)
        .where(eq(c.active,  b.setBoolean(true)),
               and(),
               ilike(c.name, b.setString("%" + call.getText() + "%")))
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
    Binds b = new Binds();

    var builder = createContribution(CustomerBean.class)
        .from(c)
        .select(c.customerId)
        .select(c.firstName)
        .select(c.lastName);

    List<Condition> conds = new ArrayList<>();

    if (status != null) {
        conds.add(eq(c.status, b.setString(status)));
    }
    if (lastName != null && !lastName.isBlank()) {
        if (!conds.isEmpty()) conds.add(and());
        conds.add(ilike(c.lastName, b.setString("%" + lastName + "%")));
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

## 8. Custom condition (lambda) with inline bind

```java
TagTable t = new TagTable();
Binds b = new Binds();

String prefixParam = b.setString("java");    // registers :p0 → "java"
Condition startsWithPrefix = () -> "LOWER(t.NAME) LIKE LOWER(" + prefixParam + ") || '%'";

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

## 9. All typed setters — quick reference

```java
Binds b = new Binds();

// single-argument (auto-named) — recommended for inline use
String r0 = b.setLong(42L);
String r1 = b.setInt(10);
String r2 = b.setDouble(0.05);
String r3 = b.setBigDecimal(new BigDecimal("19.99"));
String r4 = b.setString("Alice");
String r5 = b.setBoolean(true);
String r6 = b.setDate(LocalDate.of(2024, 1, 1));
String r7 = b.setDateTime(LocalDateTime.now());
// r0=":p0", r1=":p1", r2=":p2", … each is the :placeholder for that value

// two-argument (named) — use when you want an explicit name
b.setString("status", "ACTIVE");
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
    Binds b = new Binds();

    CustomerBean customer = createContribution(CustomerBean.class)
        .from(c)
        .select(c.customerId)
        .select(c.firstName)
        .select(c.lastName)
        .select(c.email)
        .where(eq(c.customerId, b.setLong(42L)))
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
    Binds b = new Binds();

    String sql = createContribution(Object[].class)
        .from(o)
        .select(o.orderId)
        .select(o.status)
        .where(eq(o.status, b.setString("ACTIVE")), and(), gt(o.total, b.setDouble(0.0)))
        .buildSql();

    // SQL uses the auto-generated placeholder names :p0, :p1
    assertTrue(sql.startsWith("SELECT o.ORDER_ID, o.STATUS FROM ORDERS o WHERE"));
    assertTrue(sql.contains("o.STATUS = :p0"));
    assertTrue(sql.contains("o.TOTAL > :p1"));
}
```

---

## 12. `qlid` — Auto-incrementing IDs for Eclipse Scout CodeTypes

Type `qlid` + **Ctrl+Space** to insert the next persisted long ID.
The counter is stored in `~/.lxrin_ql_id_seq` and survives IDE restarts.

```java
public class PersonStatusCodeType extends AbstractCodeType<Long, String> {

    public static final long ID = 1000L;   // ← "qlid" Ctrl+Space

    public static class ActiveCode extends AbstractCode<String> {
        public static final long ID = 1001L;   // ← "qlid" Ctrl+Space
    }

    public static class InactiveCode extends AbstractCode<String> {
        public static final long ID = 1002L;   // ← "qlid" Ctrl+Space
    }
}
```

See [Getting Started](getting-started.md#step-2-optional-install-the-qlid-intellij-live-template) for setup instructions.
