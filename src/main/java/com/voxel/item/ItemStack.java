package com.voxel.item;

public class ItemStack {
    private final Item item;
    private int count;

    public ItemStack(Item item, int count) {
        this.item = item;
        this.count = count;
    }

    public ItemStack(Item item) {
        this(item, 1);
    }

    public Item getItem() {
        return item;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public boolean isEmpty() {
        return item == null || count <= 0;
    }

    @Override
    public String toString() {
        return item != null ? item.getIdentifier() + " x" + count : "Empty";
    }
}
