package com.voxel.block;

public class BlockState {
    private final Block block;

    public BlockState(Block block) {
        this.block = block;
    }

    public Block getBlock() {
        return block;
    }

    public byte getRawId() {
        return (byte) block.getRawId();
    }

    public boolean isSolid() {
        return block.isSolid();
    }

    public boolean isTransparent() {
        return block.isTransparent();
    }

    public boolean isEmissive() {
        return block.isEmissive();
    }

    public boolean isCrossModel() {
        return block.isCrossModel();
    }

    public int getLightEmission() {
        return block.getLightEmission();
    }

    public int getTextureForFace(int face) {
        return block.getTextureForFace(face, this);
    }

    @Override
    public String toString() {
        return "BlockState{" + block.getIdentifier() + "}";
    }
}
