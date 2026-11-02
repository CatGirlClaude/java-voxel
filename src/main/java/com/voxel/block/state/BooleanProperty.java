package com.voxel.block.state;

import java.util.Collection;
import java.util.List;

public class BooleanProperty implements Property<Boolean> {
    private final String name;
    private static final List<Boolean> VALUES = List.of(Boolean.TRUE, Boolean.FALSE);

    protected BooleanProperty(String name) {
        this.name = name;
    }

    public static BooleanProperty of(String name) {
        return new BooleanProperty(name);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Class<Boolean> getValueClass() {
        return Boolean.class;
    }

    @Override
    public Collection<Boolean> getPossibleValues() {
        return VALUES;
    }

    @Override
    public String formatValue(Boolean value) {
        return value.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BooleanProperty that)) return false;
        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return "BooleanProperty{" + name + "}";
    }
}
