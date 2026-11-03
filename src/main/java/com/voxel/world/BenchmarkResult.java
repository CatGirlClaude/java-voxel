package com.voxel.world;

/**
 * Wyniki pomiaru wydajnosci generatora terenu i potoku budowy siatek.
 */
public record BenchmarkResult(
        int chunkCount,
        int workerThreads,
        double terrainGenMs,
        double terrainGenRate,
        double avgTerrainGenMs,
        int meshedCount,
        double meshingMs,
        double meshingRate,
        double avgMeshingMs,
        double totalTimeMs,
        double totalRate,
        String performanceRating
) {
    public String getFormattedSummary() {
        return String.format(
                "Benchmark [%d chunks, %d CPU threads]: Time: %.1f ms | Generation: %.0f ch/s (%.2f ms/ch) | Meshing: %.0f mesh/s (%.2f ms/m) | Total Rate: %.0f ch/s | Rating: %s",
                chunkCount, workerThreads, totalTimeMs, terrainGenRate, avgTerrainGenMs, meshingRate, avgMeshingMs, totalRate, performanceRating
        );
    }
}
