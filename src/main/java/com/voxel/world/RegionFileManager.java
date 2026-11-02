package com.voxel.world;

import com.voxel.util.Logger;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Stream;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * Menedzer zapisu i odczytu chunkow w formacie regionow (styl Minecraft Anvil / MCA).
 * Kazdy plik regionu 'r.<rx>.<rz>.mca' przechowuje siatke 32x32 chunkow w poziomie.
 * Dane blokow kazdego chunka (4096 bajtow) sa kompresowane algorytmem Deflate (zlib).
 */
public final class RegionFileManager {

    public static final int REGION_MAGIC = 0x4D434156; // "MCAV"
    public static final int REGION_VERSION = 1;
    public static final int REGION_CHUNKS_XZ = 32;

    public record RegionCoord(long rx, long rz) {}

    private RegionFileManager() {}

    public static RegionCoord getRegionCoord(long chunkX, long chunkZ) {
        return new RegionCoord(
                Math.floorDiv(chunkX, (long) REGION_CHUNKS_XZ),
                Math.floorDiv(chunkZ, (long) REGION_CHUNKS_XZ)
        );
    }

    public static Path getRegionDir(Path savesDir, String saveName) {
        return savesDir.resolve(WorldSaveManager.sanitizeSaveName(saveName) + "_regions");
    }

    /**
     * Wczytuje wszystkie zmodyfikowane chunki z plikow regionow .mca.
     */
    public static Map<World.ChunkPos, byte[]> loadAllRegions(Path regionDir) {
        Map<World.ChunkPos, byte[]> loadedChunks = new HashMap<>();
        if (!Files.exists(regionDir) || !Files.isDirectory(regionDir)) {
            return loadedChunks;
        }

        try (Stream<Path> stream = Files.list(regionDir)) {
            List<Path> regionFiles = stream
                    .filter(p -> p.getFileName().toString().startsWith("r.") && p.getFileName().toString().endsWith(".mca"))
                    .toList();

            for (Path regionPath : regionFiles) {
                loadSingleRegion(regionPath, loadedChunks);
            }
        } catch (IOException e) {
            Logger.error("RegionFile", "Error listing region files in %s: %s", regionDir, e.getMessage());
        }

        Logger.info("RegionFile", "Loaded %d chunks from MCA region format (%s)", loadedChunks.size(), regionDir.getFileName());
        return loadedChunks;
    }

    private static void loadSingleRegion(Path regionPath, Map<World.ChunkPos, byte[]> outChunks) {
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(Files.newInputStream(regionPath)))) {
            int magic = in.readInt();
            if (magic != REGION_MAGIC) {
                Logger.warn("RegionFile", "Invalid magic header for region file %s: 0x%08X", regionPath.getFileName(), magic);
                return;
            }
            int version = in.readInt();
            if (version > REGION_VERSION) {
                Logger.warn("RegionFile", "Unsupported region file version %s (version %d)", regionPath.getFileName(), version);
                return;
            }

            int chunkCount = in.readInt();
            byte[] compressedBuffer = new byte[8192];
            byte[] decompressedBuffer = new byte[Chunk.SIZE_X * Chunk.SIZE_Y * Chunk.SIZE_Z];

            Inflater inflater = new Inflater();
            for (int i = 0; i < chunkCount; i++) {
                long cx = in.readLong();
                long cy = in.readLong();
                long cz = in.readLong();
                int uncompressedSize = in.readInt();
                int compressedSize = in.readInt();

                if (compressedSize > compressedBuffer.length) {
                    compressedBuffer = new byte[compressedSize + 1024];
                }
                in.readFully(compressedBuffer, 0, compressedSize);

                if (uncompressedSize == decompressedBuffer.length) {
                    inflater.reset();
                    inflater.setInput(compressedBuffer, 0, compressedSize);
                    int read = inflater.inflate(decompressedBuffer);
                    if (read == uncompressedSize) {
                        outChunks.put(new World.ChunkPos(cx, cy, cz), decompressedBuffer.clone());
                    }
                }
            }
            inflater.end();
        } catch (IOException | DataFormatException e) {
            Logger.error("RegionFile", "Error reading chunk from region file %s: %s", regionPath.getFileName(), e.getMessage());
        }
    }

    /**
     * Zapisuje podana mape chunkow do odpowiednich plikow regionow .mca.
     */
    public static void saveRegions(Path regionDir, Map<World.ChunkPos, byte[]> chunksToSave) {
        if (chunksToSave == null || chunksToSave.isEmpty()) {
            return;
        }

        try {
            if (!Files.exists(regionDir)) {
                Files.createDirectories(regionDir);
            }
        } catch (IOException e) {
            Logger.error("RegionFile", "Could not create regions directory: %s", e.getMessage());
            return;
        }

        // Grupowanie chunkow po koordynatach regionu (rx, rz)
        Map<RegionCoord, Map<World.ChunkPos, byte[]>> regionsMap = new HashMap<>();
        for (Map.Entry<World.ChunkPos, byte[]> entry : chunksToSave.entrySet()) {
            World.ChunkPos pos = entry.getKey();
            RegionCoord rc = getRegionCoord(pos.x(), pos.z());
            regionsMap.computeIfAbsent(rc, k -> new HashMap<>()).put(pos, entry.getValue());
        }

        for (Map.Entry<RegionCoord, Map<World.ChunkPos, byte[]>> entry : regionsMap.entrySet()) {
            RegionCoord rc = entry.getKey();
            Map<World.ChunkPos, byte[]> regionChunks = entry.getValue();
            saveSingleRegion(regionDir, rc, regionChunks);
        }
    }

    private static void saveSingleRegion(Path regionDir, RegionCoord rc, Map<World.ChunkPos, byte[]> updatedChunks) {
        String baseName = "r." + rc.rx() + "." + rc.rz() + ".mca";
        Path regionFile = regionDir.resolve(baseName);
        Path tempFile = regionDir.resolve(baseName + ".tmp");

        // Polaczenie istniejacych chunkow z nowymi/zaktualizowanymi chunkami w tym regionie
        Map<World.ChunkPos, byte[]> allRegionChunks = new HashMap<>();
        if (Files.exists(regionFile)) {
            loadSingleRegion(regionFile, allRegionChunks);
        }
        allRegionChunks.putAll(updatedChunks);

        Deflater deflater = new Deflater(Deflater.DEFAULT_COMPRESSION);
        byte[] compressOutput = new byte[8192];

        try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(tempFile)))) {
            out.writeInt(REGION_MAGIC);
            out.writeInt(REGION_VERSION);
            out.writeInt(allRegionChunks.size());

            for (Map.Entry<World.ChunkPos, byte[]> entry : allRegionChunks.entrySet()) {
                World.ChunkPos pos = entry.getKey();
                byte[] rawBlocks = entry.getValue();

                deflater.reset();
                deflater.setInput(rawBlocks);
                deflater.finish();
                int compressedSize = deflater.deflate(compressOutput);

                out.writeLong(pos.x());
                out.writeLong(pos.y());
                out.writeLong(pos.z());
                out.writeInt(rawBlocks.length);
                out.writeInt(compressedSize);
                out.write(compressOutput, 0, compressedSize);
            }
            out.flush();
        } catch (IOException e) {
            Logger.error("RegionFile", "Error writing to temporary file %s: %s", tempFile.getFileName(), e.getMessage());
            try { Files.deleteIfExists(tempFile); } catch (IOException ignored) {}
            return;
        } finally {
            deflater.end();
        }

        try {
            Files.move(tempFile, regionFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            try {
                Files.move(tempFile, regionFile, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ex) {
                Logger.error("RegionFile", "Error replacing region file %s: %s", regionFile.getFileName(), ex.getMessage());
            }
        }
    }
}
