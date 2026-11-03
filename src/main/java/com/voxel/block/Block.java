package com.voxel.block;

import com.voxel.block.state.Property;
import com.voxel.registry.Identifier;
import com.voxel.world.World;

import java.util.*;

public class Block {
    protected final Identifier identifier;
    protected final int rawId;
    protected final String translationKey;
    protected final boolean solid;
    protected final boolean transparent;
    protected final boolean emissive;
    protected final boolean crossModel;
    protected final int lightEmission;
    protected final int textureTop;
    protected final int textureBottom;
    protected final int textureSide;

    protected final List<Property<?>> properties;
    protected final Map<Map<Property<?>, Comparable<?>>, BlockState> stateMap = new HashMap<>();
    protected BlockState defaultState;

    public static class Properties {
        private boolean solid = true;
        private boolean transparent = false;
        private boolean emissive = false;
        private boolean crossModel = false;
        private int lightEmission = 0;
        private int textureTop = 0;
        private int textureBottom = 0;
        private int textureSide = 0;

        public static Properties of() {
            return new Properties();
        }

        public Properties solid(boolean solid) { this.solid = solid; return this; }
        public Properties transparent(boolean transparent) { this.transparent = transparent; return this; }
        public Properties emissive(boolean emissive) { this.emissive = emissive; return this; }
        public Properties crossModel(boolean crossModel) { this.crossModel = crossModel; return this; }
        public Properties lightEmission(int lightEmission) { this.lightEmission = lightEmission; return this; }
        public Properties textures(int all) { this.textureTop = all; this.textureBottom = all; this.textureSide = all; return this; }
        public Properties textures(int top, int bottom, int side) { this.textureTop = top; this.textureBottom = bottom; this.textureSide = side; return this; }
        public Properties textures(int topBottom, int side) { this.textureTop = topBottom; this.textureBottom = topBottom; this.textureSide = side; return this; }
    }

    public Block(Identifier identifier, int rawId, Properties props) {
        this(identifier, rawId, props, Collections.emptyList());
    }

    public Block(Identifier identifier, int rawId, Properties props, Property<?>... properties) {
        this(identifier, rawId, props, Arrays.asList(properties));
    }

    public Block(Identifier identifier, int rawId, Properties props, List<Property<?>> properties) {
        this.identifier = identifier;
        this.rawId = rawId;
        this.translationKey = "block." + identifier.getNamespace() + "." + identifier.getPath();
        this.solid = props.solid;
        this.transparent = props.transparent;
        this.emissive = props.emissive;
        this.crossModel = props.crossModel;
        this.lightEmission = props.lightEmission;
        this.textureTop = props.textureTop;
        this.textureBottom = props.textureBottom;
        this.textureSide = props.textureSide;
        this.properties = Collections.unmodifiableList(new ArrayList<>(properties));
        buildStates();
    }

    protected void buildStates() {
        if (properties == null || properties.isEmpty()) {
            this.defaultState = new BlockState(this, Collections.emptyMap());
            stateMap.put(Collections.emptyMap(), defaultState);
            return;
        }

        List<Map<Property<?>, Comparable<?>>> combinations = new ArrayList<>();
        combinations.add(new HashMap<>());
        for (Property<?> prop : properties) {
            List<Map<Property<?>, Comparable<?>>> next = new ArrayList<>();
            for (Map<Property<?>, Comparable<?>> current : combinations) {
                for (Comparable<?> val : prop.getPossibleValues()) {
                    Map<Property<?>, Comparable<?>> m = new HashMap<>(current);
                    m.put(prop, val);
                    next.add(m);
                }
            }
            combinations = next;
        }

        for (Map<Property<?>, Comparable<?>> comb : combinations) {
            stateMap.put(comb, new BlockState(this, comb));
        }
        this.defaultState = stateMap.get(combinations.getFirst());
    }

    public <T extends Comparable<T>, V extends T> BlockState getStateWith(BlockState current, Property<T> property, V value) {
        Map<Property<?>, Comparable<?>> newValues = new HashMap<>(current.getValues());
        newValues.put(property, value);
        BlockState target = stateMap.get(newValues);
        if (target != null) {
            return target;
        }
        BlockState created = new BlockState(this, newValues);
        stateMap.put(newValues, created);
        return created;
    }

    public List<Property<?>> getProperties() {
        return properties;
    }

    public Collection<BlockState> getPossibleStates() {
        return stateMap.values();
    }

    public BlockState getDefaultState() {
        return defaultState;
    }

    public BlockType toBlockType() {
        return BlockType.fromId((byte) rawId);
    }

    public Identifier getIdentifier() {
        return identifier;
    }

    public int getRawId() {
        return rawId;
    }

    public String getTranslationKey() {
        return translationKey;
    }

    public boolean isSolid() { return solid; }
    public boolean isTransparent() { return transparent; }
    public boolean isEmissive() { return emissive; }
    public boolean isCrossModel() { return crossModel; }
    public int getLightEmission() { return lightEmission; }
    public int getTextureTop() { return textureTop; }
    public int getTextureBottom() { return textureBottom; }
    public int getTextureSide() { return textureSide; }

    public int getTextureForFace(int face, BlockState state) {
        return switch (face) {
            case 0 -> textureTop;
            case 1 -> textureBottom;
            default -> textureSide;
        };
    }

    public float[] getColorMultiplier(World world, long x, long y, long z, int face) {
        return null;
    }

    public void neighborChanged(World world, long x, long y, long z, long fromX, long fromY, long fromZ) {
        // Default: no-op
    }

    public boolean canSurvive(World world, long x, long y, long z) {
        return true;
    }

    public boolean isStackable() {
        return false;
    }

    public void onPlaced(World world, long x, long y, long z) {}

    public void onBroken(World world, long x, long y, long z) {}

    public void tick(World world, long x, long y, long z) {}

    @Override
    public String toString() {
        return identifier.toString();
    }
}
