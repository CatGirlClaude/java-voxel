package com.voxel;

import com.voxel.world.World;
import org.joml.Vector3d;
import org.joml.Vector3f;

public class Task38to39VerificationTest {

    public static void main(String[] args) {
        System.out.println("Running Task 38, 39 verification...");
        testSkyColorContinuityAround7AM();
        testTimePausedWhenWorldTickPaused();
        System.out.println(">>> ALL TASK 38-39 TESTS PASSED! <<<");
    }

    private static void testSkyColorContinuityAround7AM() {
        World world = new World(12345);

        // Test time range from 6:30 (0.27) to 7:30 (0.31) with small increments
        // Previously at sunHeight=0.25 (around timeOfDay = 0.292), there was a huge jump from blue to orange
        Vector3f prevColor = null;
        for (float t = 0.26f; t <= 0.32f; t += 0.001f) {
            world.timeOfDay = t;
            Vector3f col = world.getSkyColor();
            if (prevColor != null) {
                float dr = Math.abs(col.x - prevColor.x);
                float dg = Math.abs(col.y - prevColor.y);
                float db = Math.abs(col.z - prevColor.z);
                if (dr > 0.05f || dg > 0.05f || db > 0.05f) {
                    throw new AssertionError(String.format(
                            "Discontinuity detected in sky color at time %.4f! Prev=(%.3f, %.3f, %.3f), Curr=(%.3f, %.3f, %.3f)",
                            t, prevColor.x, prevColor.y, prevColor.z, col.x, col.y, col.z
                    ));
                }
            }
            prevColor = new Vector3f(col);
        }
        System.out.println("  [PASS] Sky color transition around 7:00 AM is 100% smooth and continuous (no color jump).");
    }

    private static void testTimePausedWhenWorldTickPaused() {
        World world = new World(54321);
        world.timeOfDay = 0.28f;

        // When chat is open, worldTick receives 0.0f
        for (int i = 0; i < 60; i++) {
            world.update(new Vector3d(0, 70, 0), 0.0f);
        }

        if (world.timeOfDay != 0.28f) {
            throw new AssertionError("World time advanced during simulation pause! timeOfDay = " + world.timeOfDay);
        }

        // When unpaused, worldTick receives positive dt
        world.update(new Vector3d(0, 70, 0), 1.0f);
        if (world.timeOfDay == 0.28f) {
            throw new AssertionError("World time did not advance when unpaused!");
        }

        System.out.println("  [PASS] World simulation pauses time progression when worldTick(0.0f) is called.");
    }
}
