package com.voxel.registry;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Stream;

public interface Registry<T> extends Iterable<T> {
    Identifier getRegistryId();

    T register(Identifier id, T entry);

    T register(int rawId, Identifier id, T entry);

    T get(Identifier id);

    T get(String id);

    T get(int rawId);

    Identifier getId(T entry);

    int getRawId(T entry);

    boolean containsId(Identifier id);

    boolean contains(T entry);

    Set<Identifier> keySet();

    Collection<T> values();

    int size();

    Stream<T> stream();
}
