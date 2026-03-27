package com.lxrin.ql.bind;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Typed, mutable container for named SQL bind parameters.
 *
 * <p>Unlike the functional {@link BindMap}, {@code Binds} is a mutable object
 * with dedicated type-specific setter methods to improve clarity and prevent
 * accidental type mismatches at the call site.</p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * Binds b = new Binds();
 * b.setLong("personNr", getPersonNr());
 * b.setString("status", "ACTIVE");
 *
 * createContribution(PersonBean.class)
 *     .from("PERSON p")
 *     .select("p.ID", "id")
 *     .where(eq("p.PERSON_NR", ":personNr"), and(), eq("p.STATUS", ":status"))
 *     .bind(b)
 *     .multiple();
 * }</pre>
 *
 * <p>{@code Binds} also supports method chaining:</p>
 * <pre>{@code
 * Binds b = new Binds()
 *     .setLong("personNr", getPersonNr())
 *     .setString("status", "ACTIVE");
 * }</pre>
 */
public class Binds {

    private final Map<String, Object> values = new LinkedHashMap<>();

    // -------------------------------------------------------------------------
    // Type-specific setters
    // -------------------------------------------------------------------------

    /** Adds a {@code Long} bind parameter. */
    public Binds setLong(String name, Long value) {
        values.put(requireName(name), value);
        return this;
    }

    /** Adds an {@code Integer} bind parameter. */
    public Binds setInt(String name, Integer value) {
        values.put(requireName(name), value);
        return this;
    }

    /** Adds a {@code Double} bind parameter. */
    public Binds setDouble(String name, Double value) {
        values.put(requireName(name), value);
        return this;
    }

    /** Adds a {@link BigDecimal} bind parameter. */
    public Binds setBigDecimal(String name, BigDecimal value) {
        values.put(requireName(name), value);
        return this;
    }

    /** Adds a {@code String} bind parameter. */
    public Binds setString(String name, String value) {
        values.put(requireName(name), value);
        return this;
    }

    /** Adds a {@code Boolean} bind parameter. */
    public Binds setBoolean(String name, Boolean value) {
        values.put(requireName(name), value);
        return this;
    }

    /** Adds a {@link LocalDate} bind parameter. */
    public Binds setDate(String name, LocalDate value) {
        values.put(requireName(name), value);
        return this;
    }

    /** Adds a {@link LocalDateTime} bind parameter. */
    public Binds setDateTime(String name, LocalDateTime value) {
        values.put(requireName(name), value);
        return this;
    }

    /**
     * Generic setter – use a typed setter when possible.
     *
     * @param name  bind name (without leading {@code :})
     * @param value bind value
     */
    public Binds set(String name, Object value) {
        values.put(requireName(name), value);
        return this;
    }

    // -------------------------------------------------------------------------
    // Read access
    // -------------------------------------------------------------------------

    /**
     * Returns the value bound to {@code name}, or {@code null} if absent.
     */
    public Object get(String name) {
        return values.get(name);
    }

    /**
     * Returns {@code true} if no parameters have been set.
     */
    public boolean isEmpty() {
        return values.isEmpty();
    }

    /**
     * Returns an unmodifiable view of all bind entries.
     */
    public Map<String, Object> asMap() {
        return Collections.unmodifiableMap(values);
    }

    /**
     * Converts this {@code Binds} into an immutable {@link BindMap}.
     * The returned {@link BindMap} is a snapshot; further changes to this
     * {@code Binds} will not be reflected.
     */
    public BindMap toBindMap() {
        BindMap map = new BindMap();
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            map = map.put(entry.getKey(), entry.getValue());
        }
        return map;
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private static String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("bind name must not be blank");
        }
        return name;
    }

    @Override
    public String toString() {
        return "Binds" + values;
    }
}
