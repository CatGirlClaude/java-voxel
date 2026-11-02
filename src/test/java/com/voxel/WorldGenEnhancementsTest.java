package com.voxel.world;

import com.voxel.block.BlockType;
import com.voxel.block.Blocks;
import com.voxel.entity.Player;
import com.voxel.math.SimplexNoise;
import com.voxel.registry.Registries;
import com.voxel.render.TextureAtlas;
import com.voxel.world.gen.TerrainGenerator;
import org.joml.Vector3d;

import java.nio.file.Files;
import java.nio.file.Path;

public class WorldGenEnhancementsTest {

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("  RUNNING WORLD GEN ENHANCEMENTS TEST SUITE       ");
        System.out.println("=================================================");

        SimplexNoise noise = new SimplexNoise(123456789L);
        World world = new World(123456789L);

        testRiverGeneration(noise, world);
        testSugarCaneLogic(world);
        testUnderwaterFloraAndWaterlogging(world, noise);
        testFallingGravelPhysics(world);
        testPngTextureExportAndAtlas();

        System.out.println("\n>>> ALL WORLD GEN ENHANCEMENTS TESTS PASSED (100% PASS)! <<<");
        world.cleanup();
    }

    private static void testRiverGeneration(SimplexNoise noise, World world) {
        System.out.println("\n--- TEST 1: River Generation & Riverbed Carving ---");

        // Scan a 2000x2000 area to find a deep river channel point
        long riverWx = Long.MIN_VALUE, riverWz = Long.MIN_VALUE;
        for (long x = 100; x < 2100; x += 8) {
            for (long z = 100; z < 2100; z += 8) {
                if (TerrainGenerator.computeBiome(noise, x, z) == Biome.RIVER
                        && TerrainGenerator.sampleRiver(noise, x, z) < 0.022) {
                    riverWx = x;
                    riverWz = z;
                    break;
                }
            }
            if (riverWx != Long.MIN_VALUE) break;
        }

        if (riverWx == Long.MIN_VALUE) {
            throw new AssertionError("Nie znaleziono biomu RIVER w obszarze testowym!");
        }

        double rVal = TerrainGenerator.sampleRiver(noise, riverWx, riverWz);
        int surfY = TerrainGenerator.computeSurfaceHeight(noise, riverWx, riverWz);
        System.out.printf("Found River at (%d, %d): RiverNoise=%.4f | SurfaceY=%d (SeaLevel=%d)\n",
                riverWx, riverWz, rVal, surfY, Chunk.SEA_LEVEL);

        if (surfY >= Chunk.SEA_LEVEL) {
            throw new AssertionError("Koryto rzeki powinno byc wyzlobione ponizej poziomu morza! surfY=" + surfY);
        }

        // Generate chunk containing the river and verify blocks
        Chunk chunk = world.getOrCreateChunk(Math.floorDiv(riverWx, 16), Math.floorDiv(surfY, 16), Math.floorDiv(riverWz, 16));
        int lx = (int) Math.floorMod(riverWx, 16);
        int ly = (int) Math.floorMod(surfY, 16);
        int lz = (int) Math.floorMod(riverWz, 16);
        byte bedBlock = chunk.getBlock(lx, ly, lz);
        System.out.printf("Riverbed block at floor (Y=%d): %s (ID=%d)\n", surfY, BlockType.fromId(bedBlock).name, bedBlock);

        // Bed should be gravel, clay, sand, dirt, stone (NOT grass)
        if (bedBlock == BlockType.GRASS.id || bedBlock == BlockType.TAIGA_GRASS.id) {
            throw new AssertionError("Koryto rzeki pod woda nie powinno zawierac trawy! bedBlock=" + bedBlock);
        }

        // Block above bed up to SEA_LEVEL should be water
        for (int y = surfY + 1; y <= Chunk.SEA_LEVEL; y++) {
            Chunk waterChunk = world.getOrCreateChunk(Math.floorDiv(riverWx, 16), Math.floorDiv(y, 16), Math.floorDiv(riverWz, 16));
            int wly = (int) Math.floorMod(y, 16);
            byte wBlock = waterChunk.getBlock(lx, wly, lz);
            if (!BlockType.isWater(wBlock) && !BlockType.isUnderwaterPlant(wBlock)) {
                throw new AssertionError("Powyzej dna rzeki az do poziomu morza powinna byc woda! y=" + y + ", block=" + wBlock);
            }
        }

        System.out.println("River generation & waterbed carving OK.");
    }

    private static void testSugarCaneLogic(World world) {
        System.out.println("\n--- TEST 2: Sugar Cane Survival & Physics ---");

        long x = 500, y = 50, z = 500;

        // 1. Dry sand with no adjacent water -> cannot survive
        world.setBlock(x, y - 1, z, BlockType.SAND.id, false);
        world.setBlock(x + 1, y - 1, z, BlockType.STONE.id, false);
        world.setBlock(x - 1, y - 1, z, BlockType.STONE.id, false);
        world.setBlock(x, y - 1, z + 1, BlockType.STONE.id, false);
        world.setBlock(x, y - 1, z - 1, BlockType.STONE.id, false);
        world.setBlock(x, y, z, BlockType.SUGAR_CANE.id, false);

        boolean drySurvival = Blocks.SUGAR_CANE.canSurvive(world, x, y, z);
        if (drySurvival) {
            throw new AssertionError("Trzcina cukrowa nie powinna moc przetrwac bez wody w poblizu!");
        }

        // 2. Add water adjacent to sand -> can survive
        world.setBlock(x + 1, y - 1, z, BlockType.WATER.id, false);
        boolean waterSurvival = Blocks.SUGAR_CANE.canSurvive(world, x, y, z);
        if (!waterSurvival) {
            throw new AssertionError("Trzcina cukrowa powinna przetrwac obok wody!");
        }

        // 3. Stacking: cane on top of cane -> can survive
        world.setBlock(x, y + 1, z, BlockType.SUGAR_CANE.id, false);
        boolean stackedSurvival = Blocks.SUGAR_CANE.canSurvive(world, x, y + 1, z);
        if (!stackedSurvival) {
            throw new AssertionError("Trzcina cukrowa powinna moc rosnac na innej trzcinie!");
        }

        // 4. Neighbor update: destroying soil destroys bottom sugar cane
        world.setBlock(x, y - 1, z, BlockType.AIR.id, true);
        world.updateNeighbors(x, y - 1, z);
        byte caneAfter = world.getBlock(x, y, z);
        if (caneAfter != BlockType.AIR.id) {
            throw new AssertionError("Zniszczenie podloza powinno natychmiast zniszczyc trzcine cukrowa! id=" + caneAfter);
        }

        System.out.println("Sugar cane placement, survival and neighbor destruction OK.");
    }

    private static void testUnderwaterFloraAndWaterlogging(World world, SimplexNoise noise) {
        System.out.println("\n--- TEST 3: Underwater Flora (Seagrass, Kelp, Coral) & Swimming ---");

        Player player = new Player(world);
        player.setPosition(100.5, 27.0, 100.5);

        // Place seagrass at (100, 27, 100)
        world.setBlock(100, 26, 100, BlockType.DIRT.id, false);
        world.setBlock(100, 27, 100, BlockType.SEAGRASS.id, false);

        // Player standing inside seagrass should be considered in water
        if (!player.isInWater(world)) {
            throw new AssertionError("Gracz wewnatrz trawy morskiej powinien byc traktowany jak w wodzie!");
        }

        // Head inside kelp at (100, 28, 100)
        world.setBlock(100, 28, 100, BlockType.KELP.id, false);
        player.setPosition(100.5, 26.5, 100.5);
        if (!player.isHeadUnderwater(world)) {
            throw new AssertionError("Glowa gracza wewnatrz kelpu powinna byc traktowana jak pod woda!");
        }

        // Verify registrations
        if (Registries.BLOCK.get("seagrass") != Blocks.SEAGRASS) {
            throw new AssertionError("Rejestr Blocks.SEAGRASS niezgodny!");
        }
        if (Registries.BLOCK.get("kelp") != Blocks.KELP) {
            throw new AssertionError("Rejestr Blocks.KELP niezgodny!");
        }
        if (Registries.BLOCK.get("coral") != Blocks.CORAL) {
            throw new AssertionError("Rejestr Blocks.CORAL niezgodny!");
        }
        if (Registries.BLOCK.get("clay") != Blocks.CLAY) {
            throw new AssertionError("Rejestr Blocks.CLAY niezgodny!");
        }
        if (Registries.BLOCK.get("gravel") != Blocks.GRAVEL) {
            throw new AssertionError("Rejestr Blocks.GRAVEL niezgodny!");
        }

        System.out.println("Underwater flora blocks and player water physics OK.");
    }

    private static void testFallingGravelPhysics(World world) {
        System.out.println("\n--- TEST 4: Falling Gravel Physics ---");

        com.voxel.config.GameSettings.blockPhysicsEnabled = true;
        long x = 300, y = 42, z = 300;
        world.setBlock(x, y - 2, z, BlockType.STONE.id, true); // floor
        world.setBlock(x, y - 1, z, BlockType.AIR.id, true);
        world.setBlock(x, y, z, BlockType.GRAVEL.id, true);

        // Update world physics
        world.update(new Vector3d(x, y, z), 0.1f);

        byte below = world.getBlock(x, y - 1, z);
        byte current = world.getBlock(x, y, z);

        if (below != BlockType.GRAVEL.id || current != BlockType.AIR.id) {
            throw new AssertionError("Zwir powinien opasc w dol w powietrze! below=" + below + ", current=" + current);
        }

        System.out.println("Falling gravel physics OK.");
    }

    private static void testPngTextureExportAndAtlas() {
        System.out.println("\n--- TEST 5: Texture Atlas & PNG Asset Generation ---");

        TextureAtlas.ensurePngAssetsExist();

        Path runDir = TextureAtlas.RUN_TEXTURES_DIR;
        String[] expectedPngs = {
                "sugar_cane.png", "seagrass.png", "kelp.png", "coral.png", "gravel.png", "clay.png"
        };

        for (String png : expectedPngs) {
            Path file = runDir.resolve(png);
            if (!Files.exists(file)) {
                throw new AssertionError("Brak wygenerowanego pliku PNG: " + file);
            }
            System.out.println("Verified PNG asset exists on disk: " + png);
        }

        System.out.println("Texture Atlas PNG assets OK.");
    }
}
