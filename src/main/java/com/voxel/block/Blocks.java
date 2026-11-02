package com.voxel.block;

import com.voxel.item.BlockItem;
import com.voxel.item.Item;
import com.voxel.registry.Identifier;
import com.voxel.registry.Registries;

public final class Blocks {
    public static final Block[] byId = new Block[256];

    public static final AirBlock AIR = register(0, "air", new AirBlock(Identifier.of("air"), 0));
    public static final GrassBlock GRASS = register(1, "grass_block", new GrassBlock(Identifier.of("grass_block"), 1,
            Block.Properties.of().textures(0, 2, 1)));
    public static final GrassBlock GRASS_BLOCK = GRASS;
    public static final SolidBlock DIRT = register(2, "dirt", new SolidBlock(Identifier.of("dirt"), 2,
            Block.Properties.of().textures(2)));
    public static final SolidBlock STONE = register(3, "stone", new SolidBlock(Identifier.of("stone"), 3,
            Block.Properties.of().textures(3)));
    public static final SolidBlock COBBLESTONE = register(4, "cobblestone", new SolidBlock(Identifier.of("cobblestone"), 4,
            Block.Properties.of().textures(4)));
    public static final LogBlock WOOD = register(5, "oak_wood", new LogBlock(Identifier.of("oak_wood"), 5,
            Block.Properties.of().textures(6, 6, 5)));
    public static final LeavesBlock LEAVES = register(6, "oak_leaves", new LeavesBlock(Identifier.of("oak_leaves"), 6,
            Block.Properties.of().textures(7)));
    public static final FallingBlock SAND = register(7, "sand", new FallingBlock(Identifier.of("sand"), 7,
            Block.Properties.of().textures(8)));
    public static final FluidBlock WATER = register(8, "water", new FluidBlock(Identifier.of("water"), 8,
            Block.Properties.of().textures(9)));
    public static final SolidBlock PLANKS = register(9, "oak_planks", new SolidBlock(Identifier.of("oak_planks"), 9,
            Block.Properties.of().textures(10)));
    public static final SolidBlock BRICK = register(10, "brick", new SolidBlock(Identifier.of("brick"), 10,
            Block.Properties.of().textures(11)));
    public static final GlassBlock GLASS = register(11, "glass", new GlassBlock(Identifier.of("glass"), 11,
            Block.Properties.of().textures(12)));
    public static final SolidBlock SNOW = register(12, "snow", new SolidBlock(Identifier.of("snow"), 12,
            Block.Properties.of().textures(13)));
    public static final LightBlock GLOWSTONE = register(13, "glowstone", new LightBlock(Identifier.of("glowstone"), 13,
            Block.Properties.of().textures(14).lightEmission(15)));
    public static final SolidBlock SANDSTONE = register(14, "sandstone", new SolidBlock(Identifier.of("sandstone"), 14,
            Block.Properties.of().textures(15)));
    public static final SolidBlock CACTUS = register(15, "cactus", new SolidBlock(Identifier.of("cactus"), 15,
            Block.Properties.of().textures(17, 17, 16)));
    public static final LogBlock BIRCH_WOOD = register(16, "birch_wood", new LogBlock(Identifier.of("birch_wood"), 16,
            Block.Properties.of().textures(19, 19, 18)));
    public static final LogBlock SPRUCE_WOOD = register(17, "spruce_wood", new LogBlock(Identifier.of("spruce_wood"), 17,
            Block.Properties.of().textures(28, 28, 20)));
    public static final LeavesBlock SPRUCE_LEAVES = register(18, "spruce_leaves", new LeavesBlock(Identifier.of("spruce_leaves"), 18,
            Block.Properties.of().textures(21)));
    public static final SolidBlock ICE = register(19, "ice", new SolidBlock(Identifier.of("ice"), 19,
            Block.Properties.of().textures(22).transparent(true)));
    public static final FallingBlock RED_SAND = register(20, "red_sand", new FallingBlock(Identifier.of("red_sand"), 20,
            Block.Properties.of().textures(23)));
    public static final SolidBlock TERRACOTTA = register(21, "terracotta", new SolidBlock(Identifier.of("terracotta"), 21,
            Block.Properties.of().textures(24)));
    public static final SnowyGrassBlock SNOWY_GRASS = register(22, "snowy_grass", new SnowyGrassBlock(Identifier.of("snowy_grass"), 22,
            Block.Properties.of().textures(13, 2, 25)));
    public static final GrassBlock TAIGA_GRASS = register(23, "taiga_grass", new GrassBlock(Identifier.of("taiga_grass"), 23,
            Block.Properties.of().textures(26, 2, 27)));
    public static final LeavesBlock BIRCH_LEAVES = register(24, "birch_leaves", new LeavesBlock(Identifier.of("birch_leaves"), 24,
            Block.Properties.of().textures(29)));
    public static final LogBlock JUNGLE_WOOD = register(25, "jungle_wood", new LogBlock(Identifier.of("jungle_wood"), 25,
            Block.Properties.of().textures(31, 31, 30)));
    public static final LeavesBlock JUNGLE_LEAVES = register(26, "jungle_leaves", new LeavesBlock(Identifier.of("jungle_leaves"), 26,
            Block.Properties.of().textures(32)));
    public static final GrassBlock JUNGLE_GRASS = register(27, "jungle_grass", new GrassBlock(Identifier.of("jungle_grass"), 27,
            Block.Properties.of().textures(33, 2, 34)));
    public static final PlantBlock TALL_GRASS = register(28, "tall_grass", new PlantBlock(Identifier.of("tall_grass"), 28,
            Block.Properties.of().textures(35), true));
    public static final PlantBlock VINE = register(29, "vine", new PlantBlock(Identifier.of("vine"), 29,
            Block.Properties.of().textures(36), false));
    public static final FlowerBlock FLOWER = register(30, "orchid", new FlowerBlock(Identifier.of("orchid"), 30,
            Block.Properties.of().textures(37)));
    public static final FlowerBlock ROSE = register(31, "rose", new FlowerBlock(Identifier.of("rose"), 31,
            Block.Properties.of().textures(38)));
    public static final FlowerBlock DANDELION = register(32, "dandelion", new FlowerBlock(Identifier.of("dandelion"), 32,
            Block.Properties.of().textures(39)));
    public static final FlowerBlock BLUE_ORCHID = register(33, "blue_orchid", new FlowerBlock(Identifier.of("blue_orchid"), 33,
            Block.Properties.of().textures(40)));
    public static final FlowerBlock ALLIUM = register(34, "allium", new FlowerBlock(Identifier.of("allium"), 34,
            Block.Properties.of().textures(41)));

    // Water flow levels (35..41)
    public static final FluidBlock WATER_FLOW_7 = register(35, "water_flow_7", new FluidBlock(Identifier.of("water_flow_7"), 35, Block.Properties.of().textures(9)));
    public static final FluidBlock WATER_FLOW_6 = register(36, "water_flow_6", new FluidBlock(Identifier.of("water_flow_6"), 36, Block.Properties.of().textures(9)));
    public static final FluidBlock WATER_FLOW_5 = register(37, "water_flow_5", new FluidBlock(Identifier.of("water_flow_5"), 37, Block.Properties.of().textures(9)));
    public static final FluidBlock WATER_FLOW_4 = register(38, "water_flow_4", new FluidBlock(Identifier.of("water_flow_4"), 38, Block.Properties.of().textures(9)));
    public static final FluidBlock WATER_FLOW_3 = register(39, "water_flow_3", new FluidBlock(Identifier.of("water_flow_3"), 39, Block.Properties.of().textures(9)));
    public static final FluidBlock WATER_FLOW_2 = register(40, "water_flow_2", new FluidBlock(Identifier.of("water_flow_2"), 40, Block.Properties.of().textures(9)));
    public static final FluidBlock WATER_FLOW_1 = register(41, "water_flow_1", new FluidBlock(Identifier.of("water_flow_1"), 41, Block.Properties.of().textures(9)));

    // Natural flora and geography blocks (42..47)
    public static final SugarCaneBlock SUGAR_CANE = register(42, "sugar_cane", new SugarCaneBlock(Identifier.of("sugar_cane"), 42, Block.Properties.of().textures(42)));
    public static final UnderwaterPlantBlock SEAGRASS = register(43, "seagrass", new UnderwaterPlantBlock(Identifier.of("seagrass"), 43, Block.Properties.of().textures(43)));
    public static final UnderwaterPlantBlock KELP = register(44, "kelp", new UnderwaterPlantBlock(Identifier.of("kelp"), 44, Block.Properties.of().textures(44)));
    public static final SolidBlock CORAL = register(45, "coral", new SolidBlock(Identifier.of("coral"), 45, Block.Properties.of().textures(45)));
    public static final FallingBlock GRAVEL = register(46, "gravel", new FallingBlock(Identifier.of("gravel"), 46, Block.Properties.of().textures(46)));
    public static final SolidBlock CLAY = register(47, "clay", new SolidBlock(Identifier.of("clay"), 47, Block.Properties.of().textures(47)));

    private Blocks() {}

    private static <T extends Block> T register(int rawId, String name, T block) {
        Identifier id = Identifier.of(name);
        Registries.BLOCK.register(rawId, id, block);
        byId[rawId & 0xFF] = block;
        // Register matching BlockItem in ITEM registry
        Registries.ITEM.register(rawId, id, new BlockItem(block, Item.Properties.of()));
        return block;
    }

    public static Block get(byte rawId) {
        return byId[rawId & 0xFF];
    }

    public static Block get(int rawId) {
        if (rawId < 0 || rawId >= byId.length) return AIR;
        Block b = byId[rawId];
        return b != null ? b : AIR;
    }

    public static Block get(Identifier id) {
        Block b = Registries.BLOCK.get(id);
        return b != null ? b : AIR;
    }

    public static void init() {
        // Triggers static initialization
    }
}
