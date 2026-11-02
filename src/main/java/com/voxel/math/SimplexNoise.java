package com.voxel.math;

import java.util.Random;

public class SimplexNoise {
    private final int[] p = new int[512];

    public SimplexNoise(long seed) {
        Random rand = new Random(seed);
        int[] perm = new int[256];
        for (int i = 0; i < 256; i++) {
            perm[i] = i;
        }
        for (int i = 255; i > 0; i--) {
            int j = rand.nextInt(i + 1);
            int tmp = perm[i];
            perm[i] = perm[j];
            perm[j] = tmp;
        }
        for (int i = 0; i < 512; i++) {
            p[i] = perm[i & 255];
        }
    }

    public static int fastFloor(double x) {
        int xi = (int) x;
        return x < xi ? xi - 1 : xi;
    }

    private static double fade(double t) {
        return t * t * t * (t * (t * 6.0 - 15.0) + 10.0);
    }

    private static double lerp(double t, double a, double b) {
        return a + t * (b - a);
    }

    private static double grad2D(int hash, double x, double y) {
        int h = hash & 7;
        double u = h < 4 ? x : y;
        double v = h < 4 ? y : x;
        return ((h & 1) != 0 ? -u : u) + ((h & 2) != 0 ? -2.0 * v : 2.0 * v);
    }

    private static double grad3D(int hash, double x, double y, double z) {
        int h = hash & 15;
        double u = h < 8 ? x : y;
        double v = h < 4 ? y : (h == 12 || h == 14 ? x : z);
        return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
    }

    public double noise2D(double x, double y) {
        int floorX = fastFloor(x);
        int floorY = fastFloor(y);
        int X = floorX & 255;
        int Y = floorY & 255;
        x -= floorX;
        y -= floorY;
        double u = fade(x);
        double v = fade(y);
        int A = p[X] + Y;
        int B = p[X + 1] + Y;
        double res = lerp(v,
                lerp(u, grad2D(p[A], x, y), grad2D(p[B], x - 1, y)),
                lerp(u, grad2D(p[A + 1], x, y - 1), grad2D(p[B + 1], x - 1, y - 1))
        );
        return res * 0.5;
    }

    public double noise3D(double x, double y, double z) {
        int floorX = fastFloor(x);
        int floorY = fastFloor(y);
        int floorZ = fastFloor(z);
        int X = floorX & 255;
        int Y = floorY & 255;
        int Z = floorZ & 255;
        x -= floorX;
        y -= floorY;
        z -= floorZ;
        double u = fade(x);
        double v = fade(y);
        double w = fade(z);

        int A = p[X] + Y;
        int AA = p[A] + Z;
        int AB = p[A + 1] + Z;
        int B = p[X + 1] + Y;
        int BA = p[B] + Z;
        int BB = p[B + 1] + Z;

        return lerp(w,
                lerp(v,
                        lerp(u, grad3D(p[AA], x, y, z), grad3D(p[BA], x - 1, y, z)),
                        lerp(u, grad3D(p[AB], x, y - 1, z), grad3D(p[BB], x - 1, y - 1, z))),
                lerp(v,
                        lerp(u, grad3D(p[AA + 1], x, y, z - 1), grad3D(p[BA + 1], x - 1, y, z - 1)),
                        lerp(u, grad3D(p[AB + 1], x, y - 1, z - 1), grad3D(p[BB + 1], x - 1, y - 1, z - 1)))
        );
    }

    public double fbm2D(double x, double y, int octaves, double persistence, double lacunarity) {
        double total = 0.0;
        double frequency = 1.0;
        double amplitude = 1.0;
        double maxValue = 0.0;
        for (int i = 0; i < octaves; i++) {
            total += noise2D(x * frequency, y * frequency) * amplitude;
            maxValue += amplitude;
            amplitude *= persistence;
            frequency *= lacunarity;
        }
        return total / maxValue;
    }
}
