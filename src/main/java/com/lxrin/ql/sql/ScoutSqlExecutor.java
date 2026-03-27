package com.lxrin.ql.sql;

import com.lxrin.ql.bind.BindMap;
import org.eclipse.scout.rt.server.jdbc.SQL;

import java.util.Map;

/**
 * {@link ISqlExecutor} implementation that delegates to Eclipse Scout's
 * {@link SQL} service.
 *
 * <p>The bind map is converted to an array of {@code NVPair}-compatible objects
 * that Scout's SQL processor understands.  Each entry in the {@link BindMap}
 * becomes a two-element {@code Object[]} {@code { name, value }} which Scout
 * resolves via its standard bind variable mechanism.</p>
 */
public class ScoutSqlExecutor implements ISqlExecutor {

    @Override
    public Object[][] select(String sql, BindMap binds) {
        return SQL.select(sql, toBindArray(binds));
    }

    @Override
    public void selectInto(String sql, BindMap binds, Object outputData) {
        SQL.selectInto(sql, toBindArray(binds, outputData));
    }

    @Override
    public int execute(String sql, BindMap binds) {
        return SQL.update(sql, toBindArray(binds));
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /**
     * Converts a {@link BindMap} to the varargs array that Scout's SQL methods
     * accept as {@code bindBases}.
     */
    private static Object[] toBindArray(BindMap binds, Object... extra) {
        Map<String, Object> map = binds.asMap();
        // Scout resolves named binds from beans/maps; pass the raw map directly.
        // Scout's SQL service supports java.util.Map as a bind base since Scout 22+.
        Object[] result = new Object[1 + extra.length];
        result[0] = map;
        System.arraycopy(extra, 0, result, 1, extra.length);
        return result;
    }
}
