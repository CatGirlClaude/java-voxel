package com.voxel.item;

import com.voxel.registry.Identifier;

public class Item {
    protected final Identifier identifier;
    protected final int rawId;
    protected final int maxCount;
    protected final String translationKey;

    public static class Properties {
        private int maxCount = 64;

        public static Properties of() {
            return new Properties();
        }

        public Properties maxCount(int maxCount) {
            this.maxCount = maxCount;
            return this;
        }
    }

    public Item(Identifier identifier, int rawId, Properties properties) {
        this.identifier = identifier;
        this.rawId = rawId;
        this.maxCount = properties.maxCount;
        this.translationKey = "item." + identifier.getNamespace() + "." + identifier.getPath();
    }

    public Identifier getIdentifier() {
        return identifier;
    }

    public int getRawId() {
        return rawId;
    }

    public int getMaxCount() {
        return maxCount;
    }

    public String getTranslationKey() {
        return translationKey;
    }

    @Override
    public String toString() {
        return identifier.toString();
    }
}
