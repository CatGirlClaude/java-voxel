package com.voxel.world;

public enum Biome {
    OCEAN("Ocean", 0.55f, 0.73f, 0.36f),
    DEEP_OCEAN("Gleboki ocean", 0.50f, 0.70f, 0.38f),
    FROZEN_OCEAN("Zamarzniety ocean", 0.51f, 0.69f, 0.58f),
    BEACH("Plaza", 0.60f, 0.72f, 0.38f),
    PLAINS("Rowniny", 0.57f, 0.74f, 0.35f),
    FOREST("Las debowy", 0.47f, 0.75f, 0.35f),
    BIRCH_FOREST("Las brzozowy", 0.53f, 0.74f, 0.33f),
    DESERT("Pustynia", 0.75f, 0.72f, 0.33f),
    MESA("Mesa (Kaniony)", 0.71f, 0.65f, 0.35f),
    TAIGA("Tajga", 0.53f, 0.72f, 0.51f),
    SNOWY_TUNDRA("Sniezna tundra", 0.51f, 0.69f, 0.58f),
    JUNGLE("Dzungla", 0.35f, 0.79f, 0.24f),
    SWAMP("Bagna", 0.42f, 0.44f, 0.22f),
    MOUNTAINS("Gory wysokie", 0.54f, 0.71f, 0.54f),
    RIVER("Rzeka", 0.48f, 0.74f, 0.38f);

    public final String displayName;
    public final float grassR;
    public final float grassG;
    public final float grassB;

    Biome(String displayName, float grassR, float grassG, float grassB) {
        this.displayName = displayName;
        this.grassR = grassR;
        this.grassG = grassG;
        this.grassB = grassB;
    }

    public float[] getGrassColor() {
        return new float[]{grassR, grassG, grassB};
    }
}
