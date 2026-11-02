package com.voxel.world;

public enum Biome {
    OCEAN("Ocean"),
    DEEP_OCEAN("Gleboki ocean"),
    FROZEN_OCEAN("Zamarzniety ocean"),
    BEACH("Plaza"),
    PLAINS("Rowniny"),
    FOREST("Las debowy"),
    BIRCH_FOREST("Las brzozowy"),
    DESERT("Pustynia"),
    MESA("Mesa (Kaniony)"),
    TAIGA("Tajga"),
    SNOWY_TUNDRA("Sniezna tundra"),
    JUNGLE("Dzungla"),
    SWAMP("Bagna"),
    MOUNTAINS("Gory wysokie");

    public final String displayName;

    Biome(String displayName) {
        this.displayName = displayName;
    }
}
