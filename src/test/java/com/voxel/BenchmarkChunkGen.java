package com.voxel;

import com.voxel.math.SimplexNoise;
import com.voxel.world.BenchmarkResult;
import com.voxel.world.Chunk;
import com.voxel.world.World;
import com.voxel.world.mesh.ChunkMesher;

public class BenchmarkChunkGen {
    public static void main(String[] args) {
        long seed = 5144110013345694903L;
        SimplexNoise noise = new SimplexNoise(seed);
        World world = new World(seed);

        int count = 500;
        Chunk[] testChunks = new Chunk[count];
        for (int i = 0; i < count; i++) {
            long cx = (i % 25) - 12;
            long cy = ((i / 25) % 5) - 2;
            long cz = (i / 125) - 2;
            testChunks[i] = new Chunk(cx, cy, cz);
        }

        // Warmup
        for (int i = 0; i < 50; i++) {
            testChunks[i].generateTerrain(noise);
        }

        // Single-threaded Terrain Gen
        long startGen = System.nanoTime();
        for (int i = 0; i < count; i++) {
            testChunks[i].generateTerrain(noise);
        }
        long endGen = System.nanoTime();
        double totalGenMs = (endGen - startGen) / 1_000_000.0;
        double avgGenMs = totalGenMs / count;
        double genRate = (count / (totalGenMs / 1000.0));

        System.out.printf("[Single-Thread] Terrain Gen: %d chunks in %.2f ms | Avg: %.3f ms/chunk | Rate: %.0f chunks/sec%n",
                count, totalGenMs, avgGenMs, genRate);

        // Single-threaded Meshing
        long startMesh = System.nanoTime();
        int meshed = 0;
        for (int i = 0; i < 200; i++) {
            Chunk c = testChunks[i];
            if (!c.isAllAir()) {
                ChunkMesher.buildChunkMesh(c, world);
                meshed++;
            }
        }
        long endMesh = System.nanoTime();
        double totalMeshMs = (endMesh - startMesh) / 1_000_000.0;
        double avgMeshMs = totalMeshMs / meshed;
        double meshRate = (meshed / (totalMeshMs / 1000.0));

        System.out.printf("[Single-Thread] Meshing: %d meshes in %.2f ms | Avg: %.3f ms/mesh | Rate: %.0f meshes/sec%n",
                meshed, totalMeshMs, avgMeshMs, meshRate);

        // Full Multi-threaded Engine Benchmark
        System.out.println("\n=== Uruchamianie Multi-Threaded Engine Benchmark (512 chunkow) ===");
        BenchmarkResult res = world.runBenchmark(512);
        System.out.println(res.getFormattedSummary());
        System.out.printf("  -> Generacja terenu : %.1f ms (%.0f chunks/s | %.3f ms/chunk)%n",
                res.terrainGenMs(), res.terrainGenRate(), res.avgTerrainGenMs());
        System.out.printf("  -> Budowa siatek    : %.1f ms (%.0f meshes/s | %.3f ms/mesh)%n",
                res.meshingMs(), res.meshingRate(), res.avgMeshingMs());
        System.out.printf("  -> Calkowite tempo  : %.0f chunkow/sekunde (Czas laczny: %.1f ms)%n",
                res.totalRate(), res.totalTimeMs());
        System.out.printf("  -> Ocena wydajnosci : %s%n", res.performanceRating());

        world.cleanup();
    }
}
