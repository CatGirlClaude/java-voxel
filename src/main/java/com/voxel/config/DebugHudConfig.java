package com.voxel.config;

import com.voxel.i18n.I18n;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class DebugHudConfig {

    public enum Side {
        LEFT,
        RIGHT;

        public Side opposite() {
            return this == LEFT ? RIGHT : LEFT;
        }

        public String getDisplayName() {
            return this == LEFT ? I18n.t("hud.debughud.left") : I18n.t("hud.debughud.right");
        }
    }

    public enum ElementId {
        FPS("hud.element.fps", Side.LEFT),
        POSITION("hud.element.position", Side.LEFT),
        CHUNK("hud.element.chunk", Side.LEFT),
        FACING("hud.element.facing", Side.LEFT),
        CHUNKS_3D("hud.element.chunks_3d", Side.LEFT),
        BIOME("hud.element.biome", Side.LEFT),
        LIGHT("hud.element.light", Side.LEFT),
        MOVEMENT("hud.element.movement", Side.LEFT),
        TIME("hud.element.time", Side.LEFT),
        TARGET("hud.element.target", Side.LEFT),
        SEED("hud.element.seed", Side.LEFT),

        MEMORY("hud.element.memory", Side.RIGHT),
        HARDWARE("hud.element.hardware", Side.RIGHT),
        GPU("hud.element.gpu", Side.RIGHT),
        BENCHMARK("hud.element.benchmark", Side.RIGHT);

        public final String nameKey;
        public final Side defaultSide;

        ElementId(String nameKey, Side defaultSide) {
            this.nameKey = nameKey;
            this.defaultSide = defaultSide;
        }
    }

    public static class HudItem {
        public final ElementId id;
        public boolean enabled;
        public Side side;

        public HudItem(ElementId id, boolean enabled, Side side) {
            this.id = id;
            this.enabled = enabled;
            this.side = side;
        }

        public String getDisplayName() {
            return I18n.t(id.nameKey);
        }
    }

    private static final List<HudItem> items = new ArrayList<>();

    static {
        resetToDefaults();
    }

    public static List<HudItem> getItems() {
        return items;
    }

    public static void resetToDefaults() {
        items.clear();
        for (ElementId id : ElementId.values()) {
            items.add(new HudItem(id, true, id.defaultSide));
        }
    }

    public static void toggleEnabled(int index) {
        if (index >= 0 && index < items.size()) {
            items.get(index).enabled = !items.get(index).enabled;
        }
    }

    public static void toggleSide(int index) {
        if (index >= 0 && index < items.size()) {
            HudItem it = items.get(index);
            it.side = it.side.opposite();
        }
    }

    public static void moveUp(int index) {
        if (index > 0 && index < items.size()) {
            Collections.swap(items, index, index - 1);
        }
    }

    public static void moveDown(int index) {
        if (index >= 0 && index < items.size() - 1) {
            Collections.swap(items, index, index + 1);
        }
    }

    public static String serialize() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            HudItem it = items.get(i);
            sb.append(it.id.name()).append(':')
              .append(it.enabled).append(':')
              .append(it.side.name());
            if (i < items.size() - 1) {
                sb.append(';');
            }
        }
        return sb.toString();
    }

    public static void deserialize(String data) {
        if (data == null || data.isBlank()) {
            resetToDefaults();
            return;
        }
        try {
            String[] tokens = data.split(";");
            List<HudItem> loaded = new ArrayList<>();
            for (String tok : tokens) {
                String[] parts = tok.split(":");
                if (parts.length >= 3) {
                    ElementId id = ElementId.valueOf(parts[0].trim());
                    boolean enabled = Boolean.parseBoolean(parts[1].trim());
                    Side side = Side.valueOf(parts[2].trim());
                    loaded.add(new HudItem(id, enabled, side));
                }
            }
            // Ensure all enum values exist
            for (ElementId id : ElementId.values()) {
                boolean found = false;
                for (HudItem it : loaded) {
                    if (it.id == id) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    loaded.add(new HudItem(id, true, id.defaultSide));
                }
            }
            items.clear();
            items.addAll(loaded);
        } catch (Exception e) {
            resetToDefaults();
        }
    }
}
