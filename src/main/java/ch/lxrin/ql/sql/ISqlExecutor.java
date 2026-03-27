package ch.lxrin.ql.sql;

import ch.lxrin.ql.bind.BindMap;

/**
 * Abstraction for executing SQL statements.
 *
 * <p>The default implementation delegates to Eclipse Scout's static {@code SQL}
 * service ({@link ScoutSqlExecutor}). Swap this with a mock in unit tests.</p>
 */
public interface ISqlExecutor {

    /**
     * Executes a SELECT statement and returns all rows as a two-dimensional
     * array where {@code result[row][col]}.
     *
     * @param sql     the SQL string (may contain {@code :bindName} placeholders)
     * @param binds   the bind parameters
     * @return rows × columns
     */
    Object[][] select(String sql, BindMap binds);

    /**
     * Executes a SELECT … INTO statement, writing the result columns into the
     * {@code outputData} object (an Eclipse Scout {@code AbstractTablePageData}
     * or similar bean).
     *
     * @param sql        the SQL string including the {@code INTO :col1, :col2} suffix
     * @param binds      the bind parameters
     * @param outputData the target data object
     */
    void selectInto(String sql, BindMap binds, Object outputData);

    /**
     * Executes an INSERT/UPDATE/DELETE statement.
     *
     * @param sql   the SQL string
     * @param binds the bind parameters
     * @return number of affected rows
     */
    int execute(String sql, BindMap binds);
}
