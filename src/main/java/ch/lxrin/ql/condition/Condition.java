package ch.lxrin.ql.condition;

/**
 * Represents a single SQL fragment used in a WHERE clause.
 * <p>
 * Implementations produce the SQL snippet via {@link #toSql()}.
 * </p>
 */
public interface Condition {

    /**
     * Returns the SQL fragment for this condition (e.g. {@code "t.STATUS = :status"}).
     *
     * @return SQL fragment – never {@code null}
     */
    String toSql();
}
