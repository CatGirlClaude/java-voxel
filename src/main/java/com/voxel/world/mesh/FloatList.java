package com.voxel.world.mesh;

import java.util.Arrays;

public class FloatList {
    public float[] data;
    public int size = 0;

    public FloatList(int capacity) {
        this.data = new float[capacity];
    }

    public void clear() {
        size = 0;
    }

    public FloatList copy() {
        FloatList copy = new FloatList(this.size);
        if (this.size > 0) {
            System.arraycopy(this.data, 0, copy.data, 0, this.size);
            copy.size = this.size;
        }
        return copy;
    }

    public void addVertex(float x, float y, float z, float u, float v, float skyLight, float blockLight, float r, float g, float b) {
        if (size + 10 > data.length) {
            data = Arrays.copyOf(data, Math.max(data.length * 2, size + 10));
        }
        data[size++] = x;
        data[size++] = y;
        data[size++] = z;
        data[size++] = u;
        data[size++] = v;
        data[size++] = skyLight;
        data[size++] = blockLight;
        data[size++] = r;
        data[size++] = g;
        data[size++] = b;
    }

    public void addVertex(float x, float y, float z, float u, float v, float skyLight, float blockLight) {
        addVertex(x, y, z, u, v, skyLight, blockLight, 1.0f, 1.0f, 1.0f);
    }
}
