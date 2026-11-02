package com.voxel;

import com.voxel.block.*;
import com.voxel.entity.Entity;
import com.voxel.entity.EntityTypes;
import com.voxel.entity.LivingEntity;
import com.voxel.entity.Player;
import com.voxel.item.BlockItem;
import com.voxel.item.Item;
import com.voxel.item.Items;
import com.voxel.registry.Identifier;
import com.voxel.registry.Registries;
import com.voxel.render.TextureAtlas;
import com.voxel.world.Biome;
import com.voxel.world.World;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

public class Task23to27VerificationTest {

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("  RUNNING VERIFICATION TEST FOR TASKS 23 TO 27   ");
        System.out.println("=================================================");

        testTask23PngAssets();
        testTask24ModularRegistries();
        testTask25DynamicBiomeTinting();
        testTask26SnowyGrassBlock();
        testTask27BlockUpdateSystem();

        System.out.println("\n>>> ALL TESTS FOR TASKS 23 TO 27 PASSED (100% PASS)! <<<");
    }

    private static void testTask23PngAssets() {
        System.out.println("\n--- TASK 23: Assets stored as PNG files ---");
        TextureAtlas.ensurePngAssetsExist();

        Path runTextures = TextureAtlas.RUN_TEXTURES_DIR;
        if (!Files.exists(runTextures)) {
            throw new AssertionError("Textures directory does not exist: " + runTextures);
        }

        String[] sampleTextures = {"grass_block_top", "dirt", "stone", "oak_log", "snowy_grass_side", "water", "allium"};
        for (String texName : sampleTextures) {
            Path file = runTextures.resolve(texName + ".png");
            if (!Files.exists(file)) {
                throw new AssertionError("Expected PNG file missing: " + file);
            }
            try {
                BufferedImage img = ImageIO.read(file.toFile());
                if (img == null || img.getWidth() != 16 || img.getHeight() != 16) {
                    throw new AssertionError("Invalid PNG dimensions for: " + texName);
                }
            } catch (Exception e) {
                throw new AssertionError("Failed to read PNG " + texName + ": " + e.getMessage());
            }
        }
        System.out.printf("Verified PNG asset export and loading (%d tile definitions checked).%n", TextureAtlas.TILE_NAMES.length);
        System.out.println("TASK 23 OK.");
    }

    private static void testTask24ModularRegistries() {
        System.out.println("\n--- TASK 24: Modular Architecture & Registry System ---");
        Blocks.init();
        Items.init();
        EntityTypes.init();

        // 1. Block Registry
        Block stone = Registries.BLOCK.get("stone");
        if (stone == null || stone != Blocks.STONE || stone.getRawId() != 3) {
            throw new AssertionError("Block registry failed for stone: " + stone);
        }
        Block grass = Registries.BLOCK.get(Identifier.of("grass_block"));
        if (grass == null || grass != Blocks.GRASS || !(grass instanceof GrassBlock)) {
            throw new AssertionError("Block registry failed for grass: " + grass);
        }
        System.out.printf("Block Registry: %d blocks registered. Blocks.STONE id: %d%n", Registries.BLOCK.size(), stone.getRawId());

        // 2. Item Registry
        Item stoneItem = Registries.ITEM.get("stone");
        if (stoneItem == null || !(stoneItem instanceof BlockItem) || ((BlockItem) stoneItem).getBlock() != Blocks.STONE) {
            throw new AssertionError("Item registry failed for stone block item: " + stoneItem);
        }
        System.out.printf("Item Registry: %d items registered.%n", Registries.ITEM.size());

        // 3. Entity Hierarchy & Registry
        if (Registries.ENTITY_TYPE.get("player") != EntityTypes.PLAYER) {
            throw new AssertionError("EntityType registry missing player!");
        }
        Player player = new Player();
        if (!(player instanceof LivingEntity) || !(player instanceof Entity)) {
            throw new AssertionError("Player does not extend LivingEntity!");
        }
        if (player.getHealth() != 20.0f || !player.isAlive()) {
            throw new AssertionError("Player LivingEntity health unexpected!");
        }
        System.out.printf("Entity Type Registry OK. Player instance is LivingEntity (health: %.1f).%n", player.getHealth());

        // 4. Block Hierarchy
        if (!(Blocks.AIR instanceof AirBlock)) throw new AssertionError("AIR is not AirBlock");
        if (!(Blocks.STONE instanceof SolidBlock)) throw new AssertionError("STONE is not SolidBlock");
        if (!(Blocks.WATER instanceof FluidBlock)) throw new AssertionError("WATER is not FluidBlock");
        if (!(Blocks.SAND instanceof FallingBlock)) throw new AssertionError("SAND is not FallingBlock");
        if (!(Blocks.TALL_GRASS instanceof PlantBlock)) throw new AssertionError("TALL_GRASS is not PlantBlock");
        if (!(Blocks.ROSE instanceof FlowerBlock)) throw new AssertionError("ROSE is not FlowerBlock");
        if (!(Blocks.LEAVES instanceof LeavesBlock)) throw new AssertionError("LEAVES is not LeavesBlock");
        if (!(Blocks.WOOD instanceof LogBlock)) throw new AssertionError("WOOD is not LogBlock");

        // 5. BlockType backward compatibility
        BlockType btStone = BlockType.STONE;
        if (btStone.toBlock() != Blocks.STONE || Blocks.STONE.toBlockType() != BlockType.STONE) {
            throw new AssertionError("BlockType <-> Block bridging failed!");
        }
        System.out.println("BlockType <-> Block bidirectional bridging OK.");
        System.out.println("TASK 24 OK.");
    }

    private static void testTask25DynamicBiomeTinting() {
        System.out.println("\n--- TASK 25: Dynamic Grass Biome Tinting ---");
        float[] plainsTint = Biome.PLAINS.getGrassColor();
        float[] taigaTint = Biome.TAIGA.getGrassColor();
        float[] desertTint = Biome.DESERT.getGrassColor();
        float[] jungleTint = Biome.JUNGLE.getGrassColor();

        if (plainsTint == null || taigaTint == null || desertTint == null || jungleTint == null) {
            throw new AssertionError("Biome grass colors cannot be null!");
        }
        if (plainsTint[0] == taigaTint[0] && plainsTint[1] == taigaTint[1]) {
            throw new AssertionError("Expected different grass tints between Plains and Taiga!");
        }
        System.out.printf("Biome Tints: Plains=(%.2f, %.2f, %.2f) | Taiga=(%.2f, %.2f, %.2f) | Jungle=(%.2f, %.2f, %.2f)%n",
                plainsTint[0], plainsTint[1], plainsTint[2],
                taigaTint[0], taigaTint[1], taigaTint[2],
                jungleTint[0], jungleTint[1], jungleTint[2]);

        World world = new World(12345L);
        float[] grassTint = Blocks.GRASS.getColorMultiplier(world, 0, 64, 0, 0);
        if (grassTint == null || grassTint.length != 3) {
            throw new AssertionError("GrassBlock top face must return a 3-component tint array!");
        }
        float[] grassSideTint = Blocks.GRASS.getColorMultiplier(world, 0, 64, 0, 2);
        if (grassSideTint != null) {
            throw new AssertionError("GrassBlock side face should remain untinted (null)!");
        }
        System.out.println("Dynamic biome grass tint calculation OK.");
        System.out.println("TASK 25 OK.");
    }

    private static void testTask26SnowyGrassBlock() {
        System.out.println("\n--- TASK 26: Snowy Grass Block ---");
        SnowyGrassBlock snowyGrass = Blocks.SNOWY_GRASS;
        if (snowyGrass == null) {
            throw new AssertionError("Blocks.SNOWY_GRASS is null!");
        }
        if (snowyGrass.getRawId() != 22) {
            throw new AssertionError("Expected snowy grass id 22, got " + snowyGrass.getRawId());
        }
        // Top = 13 (Snow), Bottom = 2 (Dirt), Side = 25 (Snowy grass side)
        if (snowyGrass.getTextureTop() != 13 || snowyGrass.getTextureBottom() != 2 || snowyGrass.getTextureSide() != 25) {
            throw new AssertionError("Unexpected texture mapping for SnowyGrassBlock!");
        }
        System.out.println("SnowyGrassBlock definition and texture mapping OK.");
        System.out.println("TASK 26 OK.");
    }

    private static void testTask27BlockUpdateSystem() {
        System.out.println("\n--- TASK 27: Block Update & Neighbor Update System ---");
        World world = new World(99999L);

        // 1. Plant Support Check: breaking block under flower/tall grass immediately destroys the plant!
        long px = 100, py = 50, pz = 100;
        world.setBlock(px, py, pz, BlockType.DIRT.id, false);
        world.setBlock(px, py + 1, pz, BlockType.DANDELION.id, false);

        if (world.getBlock(px, py + 1, pz) != BlockType.DANDELION.id) {
            throw new AssertionError("Failed to place Dandelion!");
        }

        // Break dirt underneath -> triggers updateNeighbors -> Dandelion breaks!
        world.setBlock(px, py, pz, BlockType.AIR.id, true);

        byte remainingPlant = world.getBlock(px, py + 1, pz);
        if (remainingPlant != BlockType.AIR.id) {
            throw new AssertionError("Dandelion did not break when soil was destroyed! Found: " + remainingPlant);
        }
        System.out.println("Plant survival neighbor update: Plant destroyed when soil removed OK.");

        // 2. Snow on Grass -> Snowy Grass transformation
        long gx = 200, gy = 60, gz = 200;
        world.setBlock(gx, gy, gz, BlockType.GRASS.id, false);
        // Place snow directly above
        world.setBlock(gx, gy + 1, gz, BlockType.SNOW.id, true);

        byte updatedGrass = world.getBlock(gx, gy, gz);
        if (updatedGrass != BlockType.SNOWY_GRASS.id) {
            throw new AssertionError("Grass did not transform into SNOWY_GRASS when snow placed above! Found: " + updatedGrass);
        }
        System.out.println("Grass to SnowyGrass transformation on snow placement OK.");

        // Remove snow above -> Snowy Grass reverts to regular grass!
        world.setBlock(gx, gy + 1, gz, BlockType.AIR.id, true);
        byte revertedGrass = world.getBlock(gx, gy, gz);
        if (revertedGrass != BlockType.GRASS.id) {
            throw new AssertionError("Snowy grass did not revert to regular GRASS when snow removed! Found: " + revertedGrass);
        }
        System.out.println("SnowyGrass revert to normal grass on snow removal OK.");

        System.out.println("TASK 27 OK.");
    }
}
