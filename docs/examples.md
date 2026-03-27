# LxrinQL Examples

Real-world usage examples for common Eclipse Scout patterns.

---

## 1. Table page data (selectInto)

```java
@Override
public OrderTablePageData getOrderTableData(OrderSearchFormData filter) {
    OrderTablePageData pageData = new OrderTablePageData();

    LxrinQL.selectInto(pageData)
        .from("ORDERS o")
        .select("o.ORDER_ID",    "orderId")
        .select("o.CUSTOMER_ID", "customerId")
        .select("o.TOTAL",       "total")
        .select("o.STATUS",      "status")
        .select("o.CREATED_AT",  "createdAt")
        .join("LEFT JOIN CUSTOMER c ON c.CUSTOMER_ID = o.CUSTOMER_ID")
        .where(
            eq("o.STATUS", ":status"),
            and(),
            between("o.CREATED_AT", ":from", ":to")
        )
        .bind("status", filter.getStatus().getValue())
        .bind("from",   filter.getDateFrom().getValue())
        .bind("to",     filter.getDateTo().getValue())
        .execute();

    return pageData;
}
```

---

## 2. Fetch a single bean

```java
public CustomerBean findCustomer(Long customerId) {
    return LxrinQL.createContribution(CustomerBean.class)
        .from("CUSTOMER c")
        .select("c.CUSTOMER_ID", "customerId")
        .select("c.FIRST_NAME",  "firstName")
        .select("c.LAST_NAME",   "lastName")
        .select("c.EMAIL",       "email")
        .where(eq("c.CUSTOMER_ID", ":id"))
        .bind("id", customerId)
        .mapWith(row -> {
            CustomerBean b = new CustomerBean();
            b.setCustomerId((Long)   row[0]);
            b.setFirstName((String)  row[1]);
            b.setLastName((String)   row[2]);
            b.setEmail((String)      row[3]);
            return b;
        })
        .single();
}
```

---

## 3. Count query

```java
public long countActiveOrders() {
    Long count = LxrinQL.createContribution(Long.class)
        .from("ORDERS o")
        .select("COUNT(*)", "cnt")
        .where(eq("o.STATUS", ":status"))
        .bind("status", "ACTIVE")
        .single();
    return count != null ? count : 0L;
}
```

---

## 4. IN condition with multiple statuses

```java
List<OrderBean> orders = LxrinQL.createContribution(OrderBean.class)
    .from("ORDERS o")
    .select("o.ORDER_ID", "orderId")
    .select("o.STATUS",   "status")
    .where(in("o.STATUS", "'PENDING'", "'PROCESSING'", "'SHIPPED'"))
    .mapWith(row -> new OrderBean((Long) row[0], (String) row[1]))
    .multiple();
```

---

## 5. Complex grouped conditions (OR logic)

```java
List<ProductBean> results = LxrinQL.createContribution(ProductBean.class)
    .from("PRODUCT p")
    .select("p.PRODUCT_ID", "productId")
    .select("p.NAME",       "name")
    .select("p.PRICE",      "price")
    .where(
        group(
            le("p.PRICE", ":maxPrice"),
            and(),
            eq("p.CATEGORY", ":cat")
        ),
        or(),
        group(
            ilike("p.NAME", ":search"),
            and(),
            isNotNull("p.FEATURED_AT")
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
    List<ILookupRow<Long>> rows = LxrinQL.createContribution(ILookupRow.class)
        .from("CATEGORY c")
        .select("c.CATEGORY_ID", "key")
        .select("c.NAME",        "text")
        .where(
            eq("c.ACTIVE", ":active"),
            and(),
            ilike("c.NAME", ":text")
        )
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
public List<PersonBean> searchPeople(String lastName, String status) {
    var builder = LxrinQL.createContribution(PersonBean.class)
        .from("PERSON t")
        .select("t.PERSON_ID",  "id")
        .select("t.FIRST_NAME", "firstName")
        .select("t.LAST_NAME",  "lastName");

    List<Condition> conds = new ArrayList<>();

    if (status != null) {
        conds.add(eq("t.STATUS", ":status"));
        builder.bind("status", status);
    }
    if (lastName != null && !lastName.isBlank()) {
        if (!conds.isEmpty()) conds.add(and());
        conds.add(ilike("t.LAST_NAME", ":lastName"));
        builder.bind("lastName", "%" + lastName + "%");
    }

    if (!conds.isEmpty()) {
        builder.where(conds.toArray(new Condition[0]));
    }

    return builder
        .mapWith(row -> new PersonBean((Long) row[0], (String) row[1], (String) row[2]))
        .multiple();
}
```

---

## 8. Custom condition (lambda)

```java
// Match records where the name starts with a given prefix, case-insensitively
Condition startsWithPrefix = () -> "LOWER(t.NAME) LIKE LOWER(:prefix) || '%'";

List<TagBean> tags = LxrinQL.createContribution(TagBean.class)
    .from("TAG t")
    .select("t.TAG_ID", "tagId")
    .select("t.NAME",   "name")
    .where(startsWithPrefix)
    .bind("prefix", "java")
    .mapWith(row -> new TagBean((Long) row[0], (String) row[1]))
    .multiple();
```

---

## 9. Unit test with mocked executor

```java
@Test
void testFindCustomer() {
    ISqlExecutor executor = Mockito.mock(ISqlExecutor.class);
    when(executor.select(anyString(), any()))
        .thenReturn(new Object[][]{{42L, "Alice", "Smith", "alice@example.com"}});

    CustomerBean customer = LxrinQL.createContribution(CustomerBean.class)
        .from("CUSTOMER c")
        .select("c.CUSTOMER_ID", "customerId")
        .select("c.FIRST_NAME",  "firstName")
        .select("c.LAST_NAME",   "lastName")
        .select("c.EMAIL",       "email")
        .where(eq("c.CUSTOMER_ID", ":id"))
        .bind("id", 42L)
        .executor(executor)
        .mapWith(row -> {
            CustomerBean b = new CustomerBean();
            b.setCustomerId((Long)  row[0]);
            b.setFirstName((String) row[1]);
            b.setLastName((String)  row[2]);
            b.setEmail((String)     row[3]);
            return b;
        })
        .single();

    assertNotNull(customer);
    assertEquals(42L,    customer.getCustomerId());
    assertEquals("Alice", customer.getFirstName());
}
```
