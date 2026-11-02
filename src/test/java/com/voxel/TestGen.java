package com.voxel;

import com.voxel.math.SimplexNoise;
import com.voxel.world.Biome;
import com.voxel.world.gen.TerrainGenerator;

public class TestGen {
    public static void main(String[] args) {
        long seed = 5144110013345694903L;
        SimplexNoise noise = new SimplexNoise(seed);

        for (int z = -1000; z <= 1000; z += 50) {
            for (int x = -1000; x <= 1000; x += 50) {
                Biome b = TerrainGenerator.computeBiome(noise, x, z);
                if (b == Biome.DESERT || b == Biome.JUNGLE) {
                    System.out.printf("Found %s at (%d, %d)%n", b, x, z);
                }
            }
        }
    }
}
