package ch.lxrin.ql.condition;

/**
 * A basic column-operator-value condition (e.g. {@code col = :val}, {@code col > :val}).
 */
public class SimpleCondition implements Condition {

    private final String column;
    private final String operator;
    private final String value;

    public SimpleCondition(String column, String operator, String value) {
        if (column == null || column.isBlank()) throw new IllegalArgumentException("column must not be blank");
        if (operator == null || operator.isBlank()) throw new IllegalArgumentException("operator must not be blank");
        if (value == null) throw new IllegalArgumentException("value must not be null");
        this.column = column;
        this.operator = operator;
        this.value = value;
    }

    @Override
    public String toSql() {
        return column + " " + operator + " " + value;
    }
}
