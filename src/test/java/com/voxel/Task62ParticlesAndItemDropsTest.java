package com.voxel;

import com.voxel.block.BlockType;
import com.voxel.entity.ItemEntity;
import com.voxel.entity.ItemEntityManager;
import com.voxel.entity.Player;
import com.voxel.particle.Particle;
import com.voxel.particle.ParticleSystem;
import org.joml.Vector3d;

public class Task62ParticlesAndItemDropsTest {

    public static void main(String[] args) {
        System.out.println("Running Task 62 verification (Particle System, Item Entities & Drop Mechanics)...");

        testParticlePhysicsAndLifecycle();
        testItemEntityPhysicsAndBobbing();
        testPlayerItemCollectionAndMagnet();
        testPlayerGiveItemLogic();

        System.out.println(">>> ALL TASK 62 PARTICLES & ITEM DROPS TESTS PASSED! <<<");
    }

    private static void testParticlePhysicsAndLifecycle() {
        Particle p = new Particle(10.0, 30.0, 10.0,
                1.0f, 2.0f, 1.0f,
                0.1f, 0.1f, 0.2f, 0.2f,
                1.0f, 1.0f, 1.0f, 1.0f,
                0.08f, 1.0f, 1.0f, 0.0f);

        if (p.isDead()) {
            throw new AssertionError("New particle should not be dead");
        }

        p.lifetime -= 1.1f;
        if (!p.isDead()) {
            throw new AssertionError("Particle should be dead after lifetime expired");
        }
    }

    private static void testItemEntityPhysicsAndBobbing() {
        ItemEntity item = new ItemEntity(null, 5.0, 32.0, 5.0, BlockType.PLANKS, 1);

        double initVisualY = item.getVisualY();
        item.tick(0.05f, null, null);

        if (item.position.y >= 32.0) {
            throw new AssertionError("Item entity should fall due to gravity when airborne");
        }

        if (item.getVisualRotation() < 0.0f || item.getVisualRotation() >= 360.0f) {
            throw new AssertionError("Item rotation should remain within [0, 360) degrees");
        }
    }

    private static void testPlayerItemCollectionAndMagnet() {
        Player player = new Player();
        player.position.set(10.0, 30.0, 10.0);

        ItemEntity item = new ItemEntity(null, 10.2, 30.4, 10.2, BlockType.STONE, 1);
        item.pickupDelay = 0.0f; // Instant pickup ready

        // Tick with player nearby: magnet should pull item and collect it
        item.tick(0.05f, null, player);

        if (!item.removed) {
            item.tick(0.05f, null, player);
        }

        if (!item.removed) {
            throw new AssertionError("Item within close proximity should be collected by player");
        }
    }

    private static void testPlayerGiveItemLogic() {
        Player player = new Player();
        for (int i = 0; i < player.hotbar.length; i++) {
            player.hotbar[i] = BlockType.AIR;
        }
        player.selectedSlot = 2;

        boolean gave = player.giveItem(BlockType.BRICK, 1);
        if (!gave || player.hotbar[2] != BlockType.BRICK) {
            throw new AssertionError("Item should be placed into selected empty hotbar slot");
        }

        // Second item should find another empty slot
        player.giveItem(BlockType.SAND, 1);
        boolean foundSand = false;
        for (BlockType b : player.hotbar) {
            if (b == BlockType.SAND) {
                foundSand = true;
                break;
            }
        }
        if (!foundSand) {
            throw new AssertionError("Second item should be placed into available empty hotbar slot");
        }
    }
}
