package com.voxel.block;

import com.voxel.block.state.Property;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class BlockState {
    private final Block block;
    private final Map<Property<?>, Comparable<?>> values;

    public BlockState(Block block) {
        this(block, Collections.emptyMap());
    }

    public BlockState(Block block, Map<Property<?>, Comparable<?>> values) {
        this.block = block;
        this.values = Collections.unmodifiableMap(new HashMap<>(values));
    }

    public Block getBlock() {
        return block;
    }

    @SuppressWarnings("unchecked")
    public <T extends Comparable<T>> T get(Property<T> property) {
        Comparable<?> val = values.get(property);
        if (val == null) {
            throw new IllegalArgumentException("Property " + property.getName() + " does not exist on block " + block.getIdentifier());
        }
        return (T) val;
    }

    public <T extends Comparable<T>, V extends T> BlockState with(Property<T> property, V value) {
        Comparable<?> current = values.get(property);
        if (current == null) {
            throw new IllegalArgumentException("Property " + property.getName() + " does not exist on block " + block.getIdentifier());
        }
        if (current.equals(value)) {
            return this;
        }
        return block.getStateWith(this, property, value);
    }

    public boolean contains(Property<?> property) {
        return values.containsKey(property);
    }

    public Map<Property<?>, Comparable<?>> getValues() {
        return values;
    }

    public byte getRawId() {
        return (byte) block.getRawId();
    }

    public boolean isSolid() {
        return block.isSolid();
    }

    public boolean isTransparent() {
        return block.isTransparent();
    }

    public boolean isEmissive() {
        return block.isEmissive();
    }

    public boolean isCrossModel() {
        return block.isCrossModel();
    }

    public int getLightEmission() {
        return block.getLightEmission();
    }

    public int getTextureForFace(int face) {
        return block.getTextureForFace(face, this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BlockState that)) return false;
        return block.equals(that.block) && values.equals(that.values);
    }

    @Override
    public int hashCode() {
        return 31 * block.hashCode() + values.hashCode();
    }

    @Override
    public String toString() {
        return "BlockState{" + block.getIdentifier() + (values.isEmpty() ? "" : values) + "}";
    }
}
