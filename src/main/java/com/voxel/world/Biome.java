package com.voxel.world;

import com.voxel.i18n.I18n;

public enum Biome {
    OCEAN("Ocean", 0.55f, 0.73f, 0.36f),
    DEEP_OCEAN("Deep Ocean", 0.50f, 0.70f, 0.38f),
    FROZEN_OCEAN("Frozen Ocean", 0.51f, 0.69f, 0.58f),
    BEACH("Beach", 0.60f, 0.72f, 0.38f),
    PLAINS("Plains", 0.57f, 0.74f, 0.35f),
    FOREST("Oak Forest", 0.47f, 0.75f, 0.35f),
    BIRCH_FOREST("Birch Forest", 0.53f, 0.74f, 0.33f),
    DESERT("Desert", 0.75f, 0.72f, 0.33f),
    MESA("Badlands (Mesa)", 0.71f, 0.65f, 0.35f),
    TAIGA("Taiga", 0.53f, 0.72f, 0.51f),
    SNOWY_TUNDRA("Snowy Tundra", 0.51f, 0.69f, 0.58f),
    JUNGLE("Jungle", 0.35f, 0.79f, 0.24f),
    SWAMP("Swamp", 0.42f, 0.44f, 0.22f),
    MOUNTAINS("Mountains", 0.54f, 0.71f, 0.54f),
    RIVER("River", 0.48f, 0.74f, 0.38f);

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

    public String getDisplayName() {
        String key = "biome." + name().toLowerCase();
        String val = I18n.t(key);
        return val.startsWith("biome.") ? displayName : val;
    }

    public float[] getGrassColor() {
        return new float[]{grassR, grassG, grassB};
    }
}
