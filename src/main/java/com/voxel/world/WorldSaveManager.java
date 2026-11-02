package com.voxel.world;

import com.voxel.block.BlockType;
import com.voxel.config.GameSettings;
import com.voxel.entity.Player;
import com.voxel.util.Logger;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

public final class WorldSaveManager {
    public static final int MAGIC_VOX1 = 0x564F5831;
    public static final int MAGIC_VOX2 = 0x564F5832;
    public static final int MAGIC_VOX3 = 0x564F5833;
    public static final int MAGIC_VOX4 = 0x564F5834;
    public static final int MAGIC_VOX5 = 0x564F5835;
    public static final int MAGIC_VOXR = 0x564F5852; // "VOXR" (Region Format)

    private static final Path SAVES_DIR = com.voxel.util.GamePaths.resolve("saves");
    private static String currentSaveName = "world";

    public record SaveInfo(
            String name,
            Path path,
            long seed,
            float timeOfDay,
            double posX,
            double posY,
            double posZ,
            int modifiedBlocksCount,
            long lastModifiedTime,
            int magic
    ) {
        public boolean isRegionFormat() {
            return magic == MAGIC_VOXR;
        }

        public String formatName() {
            return isRegionFormat() ? "Regiony (.mca)" : "Roznice (.dat)";
        }
    }

    private WorldSaveManager() {}

    public static String getCurrentSaveName() {
        return currentSaveName;
    }

    public static void setCurrentSaveName(String name) {
        if (name != null && !name.isBlank()) {
            currentSaveName = sanitizeSaveName(name);
        }
    }

    public static String sanitizeSaveName(String raw) {
        String clean = raw.trim().replaceAll("[^a-zA-Z0-9_\\-\\s]", "").replace(' ', '_');
        if (clean.isBlank()) clean = "world";
        return clean;
    }

    private static Path getSavePath(String saveName) {
        return SAVES_DIR.resolve(sanitizeSaveName(saveName) + ".dat");
    }

    public static Path getRegionDir(String saveName) {
        return RegionFileManager.getRegionDir(SAVES_DIR, saveName);
    }

    public static boolean hasSaveFile() {
        return hasSaveFile(currentSaveName);
    }

    public static boolean hasSaveFile(String saveName) {
        return Files.exists(getSavePath(saveName));
    }

    public static List<SaveInfo> listSaves() {
        List<SaveInfo> list = new ArrayList<>();
        if (!Files.exists(SAVES_DIR)) {
            return list;
        }

        try (Stream<Path> stream = Files.list(SAVES_DIR)) {
            List<Path> files = stream.filter(p -> p.getFileName().toString().endsWith(".dat")).toList();
            for (Path path : files) {
                String fileName = path.getFileName().toString();
                String saveName = fileName.substring(0, fileName.length() - 4);
                SaveInfo info = readSaveHeader(path, saveName);
                if (info != null) {
                    list.add(info);
                }
            }
        } catch (IOException e) {
            System.err.println("Blad listowania zapisow: " + e.getMessage());
        }

        list.sort((a, b) -> Long.compare(b.lastModifiedTime(), a.lastModifiedTime()));
        return list;
    }

    private static SaveInfo readSaveHeader(Path path, String saveName) {
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(Files.newInputStream(path)))) {
            int magic = in.readInt();
            if (magic != MAGIC_VOXR && magic != MAGIC_VOX5 && magic != MAGIC_VOX4 && magic != MAGIC_VOX3 && magic != MAGIC_VOX2 && magic != MAGIC_VOX1) {
                return null;
            }
            long seed = in.readLong();
            float timeOfDay = in.readFloat();
            double posX, posY, posZ;
            if (magic == MAGIC_VOXR || magic == MAGIC_VOX5 || magic == MAGIC_VOX4 || magic == MAGIC_VOX3 || magic == MAGIC_VOX2) {
                posX = in.readDouble();
                posY = in.readDouble();
                posZ = in.readDouble();
            } else {
                posX = in.readFloat();
                posY = in.readFloat();
                posZ = in.readFloat();
            }
            in.readFloat(); // yaw
            in.readFloat(); // pitch
            in.readBoolean(); // flying
            in.readInt(); // selectedSlot

            if (magic == MAGIC_VOXR || magic == MAGIC_VOX5) {
                // 9 hotbar slot block IDs
                for (int i = 0; i < 9; i++) {
                    in.readByte();
                }
                in.readBoolean(); // noclip
                in.readBoolean(); // dayCyclePaused
            } else if (magic == MAGIC_VOX4) {
                for (int i = 0; i < 9; i++) {
                    in.readByte();
                }
            }

            int count = in.readInt();
            long lastModified = Files.getLastModifiedTime(path).toMillis();
            return new SaveInfo(saveName, path, seed, timeOfDay, posX, posY, posZ, count, lastModified, magic);
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean deleteSave(String saveName) {
        try {
            Path p = getSavePath(saveName);
            if (Files.exists(p)) {
                Files.delete(p);
            }
            Path regionDir = getRegionDir(saveName);
            if (Files.exists(regionDir)) {
                try (Stream<Path> s = Files.walk(regionDir)) {
                    s.sorted(Comparator.reverseOrder()).forEach(path -> {
                        try { Files.delete(path); } catch (IOException ignored) {}
                    });
                }
            }
            return true;
        } catch (IOException e) {
            System.err.println("Blad usuwania zapisu: " + e.getMessage());
        }
        return false;
    }

    public static boolean saveWorld(World world, Player player) {
        return saveWorld(world, player, currentSaveName);
    }

    public static boolean saveWorld(World world, Player player, String saveName) {
        try {
            setCurrentSaveName(saveName);
            Path savePath = getSavePath(currentSaveName);
            Files.createDirectories(savePath.getParent());

            int magicToUse = (GameSettings.saveFormat == 1) ? MAGIC_VOXR : MAGIC_VOX5;

            try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(savePath)))) {
                out.writeInt(magicToUse);
                out.writeLong(world.getSeed());
                out.writeFloat(world.timeOfDay);
                out.writeDouble(player.position.x);
                out.writeDouble(player.position.y);
                out.writeDouble(player.position.z);
                out.writeFloat(player.yaw);
                out.writeFloat(player.pitch);
                out.writeBoolean(player.flying);
                out.writeInt(player.selectedSlot);

                // 9 hotbar blocks
                for (int i = 0; i < 9; i++) {
                    BlockType bt = player.hotbar[i];
                    out.writeByte(bt != null ? bt.id : BlockType.AIR.id);
                }

                // Gamerules & extended player data
                out.writeBoolean(player.noclip);
                out.writeBoolean(world.dayCyclePaused);

                Map<World.BlockPos, Byte> modifiedBlocks = world.getModifiedBlocks();
                out.writeInt(modifiedBlocks.size());
                for (Map.Entry<World.BlockPos, Byte> entry : modifiedBlocks.entrySet()) {
                    World.BlockPos p = entry.getKey();
                    out.writeLong(p.x());
                    out.writeLong(p.y());
                    out.writeLong(p.z());
                    out.writeByte(entry.getValue());
                }
            }

            // Jesli wybrano format Region Storage (MCA), zapisujemy pelne siatki zmodyfikowanych chunkow
            if (GameSettings.saveFormat == 1) {
                Path regionDir = getRegionDir(currentSaveName);
                Map<World.ChunkPos, byte[]> chunksToSave = new HashMap<>(world.getSavedRegionChunks());

                // Sprawdzamy wszystkie pozycje ze zmodyfikowanymi blokami
                for (World.ChunkPos cpos : world.getModifiedChunkPositions()) {
                    Chunk c = world.getChunk(cpos.x(), cpos.y(), cpos.z());
                    if (c != null) {
                        byte[] raw = c.getRawBlocks();
                        chunksToSave.put(cpos, raw);
                        world.getSavedRegionChunks().put(cpos, raw);
                    }
                }

                RegionFileManager.saveRegions(regionDir, chunksToSave);
                Logger.info("SaveManager", "Zapisano stan swiata '%s' w formacie Regionow MCA (%d chunkow, %d blokow) do: %s",
                        currentSaveName, chunksToSave.size(), world.getModifiedBlocks().size(), regionDir.getFileName());
            } else {
                Logger.info("SaveManager", "Zapisano stan swiata '%s' w formacie Roznic (.dat) (%d zmodyfikowanych blokow) do: %s",
                        currentSaveName, world.getModifiedBlocks().size(), savePath.getFileName());
            }

            return true;
        } catch (IOException e) {
            Logger.error("SaveManager", "Blad zapisu swiata '%s': %s", currentSaveName, e.getMessage());
            return false;
        }
    }

    public static boolean loadWorld(World world, Player player) {
        return loadWorld(world, player, currentSaveName);
    }

    public static boolean loadWorld(World world, Player player, String saveName) {
        setCurrentSaveName(saveName);
        Path savePath = getSavePath(currentSaveName);
        if (!Files.exists(savePath)) {
            Logger.warn("SaveManager", "Plik zapisu nie istnieje: %s", savePath);
            return false;
        }

        try (DataInputStream in = new DataInputStream(new BufferedInputStream(Files.newInputStream(savePath)))) {
            int magic = in.readInt();
            if (magic != MAGIC_VOXR && magic != MAGIC_VOX5 && magic != MAGIC_VOX4 && magic != MAGIC_VOX3 && magic != MAGIC_VOX2 && magic != MAGIC_VOX1) {
                Logger.error("SaveManager", "Nieprawidlowy format naglowka pliku zapisu (magic: 0x%08X)", magic);
                return false;
            }
            long seed = in.readLong();
            world.resetSeed(seed);
            world.timeOfDay = in.readFloat();

            if (magic == MAGIC_VOXR || magic == MAGIC_VOX5 || magic == MAGIC_VOX4 || magic == MAGIC_VOX3 || magic == MAGIC_VOX2) {
                player.position.x = in.readDouble();
                player.position.y = in.readDouble();
                player.position.z = in.readDouble();
            } else {
                player.position.x = in.readFloat();
                player.position.y = in.readFloat();
                player.position.z = in.readFloat();
            }
            player.velocity.set(0, 0, 0);
            player.yaw = in.readFloat();
            player.pitch = in.readFloat();
            player.flying = in.readBoolean();
            player.selectedSlot = in.readInt();

            if (magic == MAGIC_VOXR || magic == MAGIC_VOX5) {
                for (int i = 0; i < 9; i++) {
                    byte bId = in.readByte();
                    player.hotbar[i] = BlockType.fromId(bId);
                }
                player.noclip = in.readBoolean();
                world.dayCyclePaused = in.readBoolean();
            } else if (magic == MAGIC_VOX4) {
                for (int i = 0; i < 9; i++) {
                    byte bId = in.readByte();
                    player.hotbar[i] = BlockType.fromId(bId);
                }
                player.noclip = false;
                world.dayCyclePaused = false;
            } else {
                System.arraycopy(BlockType.HOTBAR_BLOCKS, 0, player.hotbar, 0, 9);
                player.noclip = false;
                world.dayCyclePaused = false;
            }

            int count = in.readInt();
            Map<World.BlockPos, Byte> modifiedBlocks = world.getModifiedBlocks();
            modifiedBlocks.clear();
            world.clearSavedRegionChunks();

            for (int i = 0; i < count; i++) {
                long bx, by, bz;
                byte v;
                if (magic == MAGIC_VOXR || magic == MAGIC_VOX5 || magic == MAGIC_VOX4 || magic == MAGIC_VOX3) {
                    bx = in.readLong();
                    by = in.readLong();
                    bz = in.readLong();
                    v = in.readByte();
                } else if (magic == MAGIC_VOX2) {
                    bx = in.readLong();
                    by = in.readInt();
                    bz = in.readLong();
                    v = in.readByte();
                } else {
                    long k = in.readLong();
                    v = in.readByte();
                    int vx = (int) (k >> 38) & 0x3FFFFFF;
                    if ((vx & 0x2000000) != 0) vx |= ~0x3FFFFFF;
                    int vz = (int) (k >> 12) & 0x3FFFFFF;
                    if ((vz & 0x2000000) != 0) vz |= ~0x3FFFFFF;
                    int vy = (int) (k & 0xFFFL);
                    bx = vx;
                    by = vy;
                    bz = vz;
                }
                modifiedBlocks.put(new World.BlockPos(bx, by, bz), v);
                world.getModifiedChunkPositions().add(new World.ChunkPos(
                        Math.floorDiv(bx, (long) Chunk.SIZE_X),
                        Math.floorDiv(by, (long) Chunk.SIZE_Y),
                        Math.floorDiv(bz, (long) Chunk.SIZE_Z)
                ));
            }

            // Wczytanie chunkow z regionow MCA, jesli zapis byl w tym formacie
            if (magic == MAGIC_VOXR) {
                Path regionDir = getRegionDir(currentSaveName);
                Map<World.ChunkPos, byte[]> loadedRegions = RegionFileManager.loadAllRegions(regionDir);
                world.getSavedRegionChunks().putAll(loadedRegions);
                world.getModifiedChunkPositions().addAll(loadedRegions.keySet());
                Logger.info("SaveManager", "Zaladowano format Regionow MCA: %d pelnych chunkow z %s",
                        loadedRegions.size(), regionDir.getFileName());
            }

            world.clearLoadedChunks();

            long pcx = Math.floorDiv((long) Math.floor(player.position.x), (long) Chunk.SIZE_X);
            long pcy = Math.floorDiv((long) Math.floor(player.position.y), (long) Chunk.SIZE_Y);
            long pcz = Math.floorDiv((long) Math.floor(player.position.z), (long) Chunk.SIZE_Z);
            world.generateInitialChunks(pcx, pcy, pcz, 3, 2);

            // Apply rule overrides from game settings if enabled
            applyGameruleOverrides(world, player);

            return true;
        } catch (IOException e) {
            System.err.println("Blad odczytu swiata: " + e.getMessage());
            return false;
        }
    }

    public static void applyGameruleOverrides(World world, Player player) {
        if (world == null || player == null) return;
        if (GameSettings.enableOverrides) {
            if (GameSettings.overrideTime) {
                world.timeOfDay = GameSettings.overrideTimeValue;
            }
            if (GameSettings.overrideDayCycle) {
                world.dayCyclePaused = GameSettings.overrideDayCycleValue;
            }
            if (GameSettings.overrideFly == 1) {
                player.flying = true;
            } else if (GameSettings.overrideFly == 2) {
                player.flying = false;
            }
            if (GameSettings.overrideNoclip == 1) {
                player.noclip = true;
                player.flying = true;
            } else if (GameSettings.overrideNoclip == 2) {
                player.noclip = false;
            }
        }
    }
}
