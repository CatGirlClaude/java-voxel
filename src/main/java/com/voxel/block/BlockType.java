package com.voxel.block;

public enum BlockType {
    AIR(0, "Powietrze", -1, -1, -1, false, true, false),
    GRASS(1, "Trawa", 0, 2, 1, true, false, false),
    DIRT(2, "Ziemia", 2, 2, 2, true, false, false),
    STONE(3, "Kamien", 3, 3, 3, true, false, false),
    COBBLESTONE(4, "Bruk", 4, 4, 4, true, false, false),
    WOOD(5, "Debowe drewno", 6, 6, 5, true, false, false),
    LEAVES(6, "Liscie", 7, 7, 7, true, false, false),
    SAND(7, "Piasek", 8, 8, 8, true, false, false),
    WATER(8, "Woda", 9, 9, 9, false, true, false),
    PLANKS(9, "Deski", 10, 10, 10, true, false, false),
    BRICK(10, "Cegla", 11, 11, 11, true, false, false),
    GLASS(11, "Szklo", 12, 12, 12, true, true, false),
    SNOW(12, "Snieg", 13, 13, 13, true, false, false),
    GLOWSTONE(13, "Jasnoglaz", 14, 14, 14, true, false, true),
    SANDSTONE(14, "Piaskowiec", 15, 15, 15, true, false, false),
    CACTUS(15, "Kaktus", 17, 17, 16, true, false, false),
    BIRCH_WOOD(16, "Brzoza", 19, 19, 18, true, false, false),
    SPRUCE_WOOD(17, "Swierk", 28, 28, 20, true, false, false),
    SPRUCE_LEAVES(18, "Igliwie", 21, 21, 21, true, false, false),
    ICE(19, "Lod", 22, 22, 22, true, true, false),
    RED_SAND(20, "Czerwony piasek", 23, 23, 23, true, false, false),
    TERRACOTTA(21, "Terakota (Mesa)", 24, 24, 24, true, false, false),
    SNOWY_GRASS(22, "Zasniezona trawa", 13, 2, 25, true, false, false),
    TAIGA_GRASS(23, "Trawa tajgowa", 26, 2, 27, true, false, false),
    BIRCH_LEAVES(24, "Liscie brzozowe", 29, 29, 29, true, false, false),
    JUNGLE_WOOD(25, "Drewno dzunglowe", 31, 31, 30, true, false, false),
    JUNGLE_LEAVES(26, "Liscie dzunglowe", 32, 32, 32, true, false, false),
    JUNGLE_GRASS(27, "Trawa dzunglowa", 33, 2, 34, true, false, false),
    TALL_GRASS(28, "Wysoka trawa", 35, 35, 35, false, false, false, true),
    VINE(29, "Liany / Pnacza", 36, 36, 36, false, false, false, true),
    FLOWER(30, "Kwiaty / Orchidea", 37, 37, 37, false, false, false, true),
    ROSE(31, "Czerwona roza", 38, 38, 38, false, false, false, true),
    DANDELION(32, "Mniszek lekarski", 39, 39, 39, false, false, false, true),
    BLUE_ORCHID(33, "Niebieska orchidea", 40, 40, 40, false, false, false, true),
    ALLIUM(34, "Czosnek / Allium", 41, 41, 41, false, false, false, true),
    WATER_FLOW_7(35, "Flowing Water 7", 9, 9, 9, false, true, false),
    WATER_FLOW_6(36, "Flowing Water 6", 9, 9, 9, false, true, false),
    WATER_FLOW_5(37, "Flowing Water 5", 9, 9, 9, false, true, false),
    WATER_FLOW_4(38, "Flowing Water 4", 9, 9, 9, false, true, false),
    WATER_FLOW_3(39, "Flowing Water 3", 9, 9, 9, false, true, false),
    WATER_FLOW_2(40, "Flowing Water 2", 9, 9, 9, false, true, false),
    WATER_FLOW_1(41, "Flowing Water 1", 9, 9, 9, false, true, false),
    SUGAR_CANE(42, "Trzcina cukrowa", 42, 42, 42, false, false, false, true),
    SEAGRASS(43, "Trawa morska", 43, 43, 43, false, true, false, true),
    KELP(44, "Wodorosty", 44, 44, 44, false, true, false, true),
    CORAL(45, "Koralowiec", 45, 45, 45, true, false, false, false),
    GRAVEL(46, "Zwir", 46, 46, 46, true, false, false, false),
    CLAY(47, "Glina", 47, 47, 47, true, false, false, false);

    public final byte id;
    public final String name;
    public final int topTex;
    public final int bottomTex;
    public final int sideTex;
    public final boolean solid;
    public final boolean transparent;
    public final boolean emissive;
    public final boolean crossModel;

    private static final BlockType[] BY_ID = new BlockType[128];

    public static final BlockType[] HOTBAR_BLOCKS = {
        GRASS, STONE, WOOD, BIRCH_WOOD, JUNGLE_WOOD, TALL_GRASS, JUNGLE_LEAVES, GLASS, GLOWSTONE
    };

    static {
        for (BlockType b : values()) {
            BY_ID[b.id] = b;
        }
    }

    BlockType(int id, String name, int topTex, int bottomTex, int sideTex,
              boolean solid, boolean transparent, boolean emissive) {
        this(id, name, topTex, bottomTex, sideTex, solid, transparent, emissive, false);
    }

    BlockType(int id, String name, int topTex, int bottomTex, int sideTex,
              boolean solid, boolean transparent, boolean emissive, boolean crossModel) {
        this.id = (byte) id;
        this.name = name;
        this.topTex = topTex;
        this.bottomTex = bottomTex;
        this.sideTex = sideTex;
        this.solid = solid;
        this.transparent = transparent;
        this.emissive = emissive;
        this.crossModel = crossModel;
    }

    public Block toBlock() {
        return Blocks.get(id);
    }

    public static BlockType fromId(byte id) {
        if (id < 0 || id >= BY_ID.length || BY_ID[id] == null) {
            return AIR;
        }
        return BY_ID[id];
    }

    public static BlockType byName(String token) {
        if (token == null || token.isBlank()) return null;
        String s = token.trim().toLowerCase();
        try {
            int id = Integer.parseInt(s);
            return fromId((byte) id);
        } catch (NumberFormatException ignored) {}

        return switch (s) {
            case "air", "powietrze", "puste", "brak" -> AIR;
            case "grass", "trawa" -> GRASS;
            case "dirt", "ziemia" -> DIRT;
            case "stone", "kamien" -> STONE;
            case "cobble", "cobblestone", "bruk" -> COBBLESTONE;
            case "wood", "oak_wood", "drewno", "dab" -> WOOD;
            case "leaves", "liscie" -> LEAVES;
            case "sand", "piasek" -> SAND;
            case "water", "woda" -> WATER;
            case "planks", "deski" -> PLANKS;
            case "brick", "bricks", "cegla", "cegly" -> BRICK;
            case "glass", "szklo" -> GLASS;
            case "snow", "snieg" -> SNOW;
            case "glowstone", "jasnoglaz", "swiatlo" -> GLOWSTONE;
            case "sandstone", "piaskowiec" -> SANDSTONE;
            case "cactus", "kaktus" -> CACTUS;
            case "birch", "birch_wood", "brzoza" -> BIRCH_WOOD;
            case "spruce", "spruce_wood", "swierk" -> SPRUCE_WOOD;
            case "spruce_leaves", "igliwie" -> SPRUCE_LEAVES;
            case "ice", "lod" -> ICE;
            case "red_sand", "czerwony_piasek" -> RED_SAND;
            case "terracotta", "terakota", "mesa" -> TERRACOTTA;
            case "snowy_grass", "zasniezona_trawa" -> SNOWY_GRASS;
            case "taiga_grass", "trawa_tajgowa" -> TAIGA_GRASS;
            case "birch_leaves", "liscie_brzozowe" -> BIRCH_LEAVES;
            case "jungle_wood", "drewno_dzunglowe", "jungle" -> JUNGLE_WOOD;
            case "jungle_leaves", "liscie_dzunglowe" -> JUNGLE_LEAVES;
            case "jungle_grass", "trawa_dzunglowa" -> JUNGLE_GRASS;
            case "tall_grass", "wysoka_trawa", "trawa_rosnaca", "trawka" -> TALL_GRASS;
            case "vine", "vines", "liana", "liany", "pnacza" -> VINE;
            case "flower", "kwiat", "kwiaty", "orchidea" -> FLOWER;
            case "rose", "red_tulip", "roza", "czerwona_roza" -> ROSE;
            case "dandelion", "mniszek", "mniszek_lekarski", "zolty_kwiat" -> DANDELION;
            case "blue_orchid", "niebieska_orchidea", "niebieski_kwiat" -> BLUE_ORCHID;
            case "allium", "czosnek", "fioletowy_kwiat" -> ALLIUM;
            case "sugar_cane", "trzcina", "trzcina_cukrowa" -> SUGAR_CANE;
            case "seagrass", "trawa_morska" -> SEAGRASS;
            case "kelp", "wodorosty" -> KELP;
            case "coral", "koral", "koralowiec" -> CORAL;
            case "gravel", "zwir" -> GRAVEL;
            case "clay", "glina" -> CLAY;
            default -> {
                for (BlockType bt : values()) {
                    if (bt.name().equalsIgnoreCase(s) || bt.name.equalsIgnoreCase(s)) {
                        yield bt;
                    }
                }
                yield null;
            }
        };
    }

    public boolean isWater() {
        return this == WATER || (this.id >= WATER_FLOW_7.id && this.id <= WATER_FLOW_1.id);
    }

    public static boolean isWater(byte id) {
        return id == WATER.id || (id >= WATER_FLOW_7.id && id <= WATER_FLOW_1.id);
    }

    public static int getWaterLevel(byte id) {
        if (id == WATER.id) return 8;
        if (id >= WATER_FLOW_7.id && id <= WATER_FLOW_1.id) {
            return 7 - (id - WATER_FLOW_7.id);
        }
        return 0;
    }

    public static byte getFlowBlockId(int level) {
        if (level >= 8) return WATER.id;
        if (level <= 0) return AIR.id;
        return (byte) (WATER_FLOW_7.id + (7 - level));
    }

    public static boolean isWashableDecoration(byte id) {
        if (id == AIR.id) return false;
        BlockType bt = fromId(id);
        if (bt == SEAGRASS || bt == KELP) return false;
        return bt.crossModel;
    }

    public static boolean isUnderwaterPlant(byte id) {
        return id == SEAGRASS.id || id == KELP.id;
    }

    public String getDisplayName() {
        String key = "block." + name().toLowerCase();
        String val = com.voxel.i18n.I18n.t(key);
        return val.startsWith("block.") ? name : val;
    }

    public int getTextureForFace(int face) {
        // 0: +Y (top), 1: -Y (bottom), 2..5: sides
        if (face == 0) return topTex;
        if (face == 1) return bottomTex;
        return sideTex;
    }
}
