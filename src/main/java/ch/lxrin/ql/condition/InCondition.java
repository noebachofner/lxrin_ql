package ch.lxrin.ql.condition;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * SQL {@code IN} condition: {@code column IN (v1, v2, ...)}.
 * <p>
 * Values that start with {@code :} are treated as named bind references.
 * Plain string values are quoted automatically.
 * </p>
 */
public class InCondition implements Condition {

    private final String column;
    private final List<String> values;

    public InCondition(String column, String... values) {
        if (column == null || column.isBlank()) throw new IllegalArgumentException("column must not be blank");
        if (values == null || values.length == 0) throw new IllegalArgumentException("at least one value required");
        this.column = column;
        this.values = Arrays.asList(values);
    }

    @Override
    public String toSql() {
        String list = values.stream().collect(Collectors.joining(", "));
        return column + " IN (" + list + ")";
    }
}
