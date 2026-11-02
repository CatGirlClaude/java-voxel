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

    public void addVertex(float x, float y, float z, float u, float v, float skyLight, float blockLight) {
        if (size + 7 > data.length) {
            data = Arrays.copyOf(data, data.length * 2);
        }
        data[size++] = x;
        data[size++] = y;
        data[size++] = z;
        data[size++] = u;
        data[size++] = v;
        data[size++] = skyLight;
        data[size++] = blockLight;
    }
}
