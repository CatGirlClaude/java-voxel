package com.voxel.block.state;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class IntegerProperty implements Property<Integer> {
    private final String name;
    private final int min;
    private final int max;
    private final List<Integer> values;

    protected IntegerProperty(String name, int min, int max) {
        if (min >= max) {
            throw new IllegalArgumentException("Min value must be less than max value for IntegerProperty " + name);
        }
        this.name = name;
        this.min = min;
        this.max = max;
        List<Integer> list = new ArrayList<>();
        for (int i = min; i <= max; i++) {
            list.add(i);
        }
        this.values = Collections.unmodifiableList(list);
    }

    public static IntegerProperty of(String name, int min, int max) {
        return new IntegerProperty(name, min, max);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Class<Integer> getValueClass() {
        return Integer.class;
    }

    @Override
    public Collection<Integer> getPossibleValues() {
        return values;
    }

    @Override
    public String formatValue(Integer value) {
        return String.valueOf(value);
    }

    public int getMin() {
        return min;
    }

    public int getMax() {
        return max;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IntegerProperty that)) return false;
        return min == that.min && max == that.max && name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return 31 * name.hashCode() + 31 * min + max;
    }

    @Override
    public String toString() {
        return "IntegerProperty{" + name + ", min=" + min + ", max=" + max + '}';
    }
}
