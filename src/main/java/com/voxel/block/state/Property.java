package com.voxel.block.state;

import java.util.Collection;

public interface Property<T extends Comparable<T>> {
    String getName();
    Class<T> getValueClass();
    Collection<T> getPossibleValues();
    String formatValue(T value);
}
