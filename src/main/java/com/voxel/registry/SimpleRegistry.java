package com.voxel.registry;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public class SimpleRegistry<T> implements Registry<T> {
    private final Identifier registryId;
    private final Map<Identifier, T> byId = new ConcurrentHashMap<>();
    private final Map<T, Identifier> byEntry = new ConcurrentHashMap<>();
    private final Map<Integer, T> byRawId = new ConcurrentHashMap<>();
    private final Map<T, Integer> byRawEntry = new ConcurrentHashMap<>();
    private final List<T> entriesList = new ArrayList<>();
    private int nextRawId = 0;

    public SimpleRegistry(String registryName) {
        this.registryId = Identifier.of(registryName);
    }

    public SimpleRegistry(Identifier registryId) {
        this.registryId = registryId;
    }

    @Override
    public Identifier getRegistryId() {
        return registryId;
    }

    @Override
    public synchronized T register(Identifier id, T entry) {
        return register(nextRawId, id, entry);
    }

    @Override
    public synchronized T register(int rawId, Identifier id, T entry) {
        Objects.requireNonNull(id, "Registry ID cannot be null");
        Objects.requireNonNull(entry, "Registry entry cannot be null");
        byId.put(id, entry);
        byEntry.put(entry, id);
        byRawId.put(rawId, entry);
        byRawEntry.put(entry, rawId);
        if (!entriesList.contains(entry)) {
            entriesList.add(entry);
        }
        if (rawId >= nextRawId) {
            nextRawId = rawId + 1;
        }
        return entry;
    }

    @Override
    public T get(Identifier id) {
        if (id == null) return null;
        return byId.get(id);
    }

    @Override
    public T get(String id) {
        if (id == null) return null;
        return byId.get(Identifier.of(id));
    }

    @Override
    public T get(int rawId) {
        return byRawId.get(rawId);
    }

    @Override
    public Identifier getId(T entry) {
        if (entry == null) return null;
        return byEntry.get(entry);
    }

    @Override
    public int getRawId(T entry) {
        if (entry == null) return -1;
        return byRawEntry.getOrDefault(entry, -1);
    }

    @Override
    public boolean containsId(Identifier id) {
        return byId.containsKey(id);
    }

    @Override
    public boolean contains(T entry) {
        return byEntry.containsKey(entry);
    }

    @Override
    public Set<Identifier> keySet() {
        return Collections.unmodifiableSet(byId.keySet());
    }

    @Override
    public Collection<T> values() {
        return Collections.unmodifiableList(entriesList);
    }

    @Override
    public int size() {
        return entriesList.size();
    }

    @Override
    public Stream<T> stream() {
        return entriesList.stream();
    }

    @Override
    public Iterator<T> iterator() {
        return Collections.unmodifiableList(entriesList).iterator();
    }
}
