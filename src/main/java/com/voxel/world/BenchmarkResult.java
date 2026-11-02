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
                "Benchmark [%d chunkow, %d watkow CPU]: Czas: %.1f ms | Generacja: %.0f ch/s (%.2f ms/ch) | Siatki: %.0f mesh/s (%.2f ms/m) | Laczna wydajnosc: %.0f ch/s | Ocena: %s",
                chunkCount, workerThreads, totalTimeMs, terrainGenRate, avgTerrainGenMs, meshingRate, avgMeshingMs, totalRate, performanceRating
        );
    }
}
