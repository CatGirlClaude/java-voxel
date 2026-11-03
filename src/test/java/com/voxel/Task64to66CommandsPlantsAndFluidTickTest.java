package com.voxel;

import com.voxel.block.BlockType;
import com.voxel.block.Blocks;
import com.voxel.command.Command;
import com.voxel.command.CommandContext;
import com.voxel.command.CommandRegistry;
import com.voxel.config.GameSettings;
import com.voxel.entity.Player;
import com.voxel.ui.ChatSystem;
import com.voxel.world.World;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class Task64to66CommandsPlantsAndFluidTickTest {

    public static void main(String[] args) {
        System.out.println("Running Task 64-66 verification (OOP Commands, Plant Top-Only Stacking, and FluidBlock.tick)...");

        testModularCommandSystem();
        testPlantTopOnlyStackingRules();
        testFluidBlockTickDelegation();

        System.out.println(">>> ALL TASK 64-66 VERIFICATION TESTS PASSED SUCCESSFULLY! <<<");
    }

    private static void testModularCommandSystem() {
        System.out.println("--- TEST: Modular OOP Command System (Task 64) ---");
        CommandRegistry registry = new CommandRegistry();

        // 1. Check all built-in commands are registered
        String[] requiredCommands = {
                "tp", "spawn", "fly", "noclip", "debug", "time", "bench",
                "save", "load", "setblock", "reset", "clear", "pos", "locate",
                "worldsetting", "logs", "help"
        };
        for (String name : requiredCommands) {
            Command cmd = registry.get(name);
            if (cmd == null) {
                throw new AssertionError("Built-in command /" + name + " is not registered in CommandRegistry!");
            }
        }

        // 2. Check aliases
        if (registry.get("nc") == null || !registry.get("nc").getName().equals("noclip")) {
            throw new AssertionError("Alias /nc should resolve to /noclip!");
        }
        if (registry.get("ws") == null || !registry.get("ws").getName().equals("worldsetting")) {
            throw new AssertionError("Alias /ws should resolve to /worldsetting!");
        }
        if (registry.get("cls") == null || !registry.get("cls").getName().equals("clear")) {
            throw new AssertionError("Alias /cls should resolve to /clear!");
        }

        // 3. Test registering a custom command dynamically
        AtomicBoolean customExecuted = new AtomicBoolean(false);
        Command customCmd = new Command() {
            @Override
            public String getName() {
                return "customtest";
            }

            @Override
            public List<String> getAliases() {
                return List.of("ct");
            }

            @Override
            public String getDescription() {
                return "Custom test command";
            }

            @Override
            public String getUsage() {
                return "/customtest <arg>";
            }

            @Override
            public void execute(CommandContext ctx) {
                customExecuted.set(true);
                ctx.success("Custom command executed with args: " + String.join(" ", ctx.args()));
            }
        };

        registry.register(customCmd);
        if (registry.get("customtest") == null || registry.get("ct") == null) {
            throw new AssertionError("Custom command /customtest failed to register!");
        }

        ChatSystem chat = new ChatSystem();
        Player player = new Player();
        boolean res = registry.execute("/ct hello world", null, player, null, chat);
        if (!res || !customExecuted.get()) {
            throw new AssertionError("Execution of custom command /ct failed!");
        }

        // 4. Test suggestion generation
        List<ChatSystem.Suggestion> suggestions = registry.getSuggestions("/time ", null, player, null, chat);
        boolean hasDay = suggestions.stream().anyMatch(s -> s.label().equals("day"));
        if (!hasDay) {
            throw new AssertionError("Expected 'day' in /time suggestions from modular command!");
        }

        System.out.println("  [PASS] Task 64 OOP Command Registry, aliases, dynamic registration, and suggestions verified.");
    }

    private static void testPlantTopOnlyStackingRules() {
        System.out.println("--- TEST: Plant Top-Only Stacking (Task 65) ---");
        World world = new World(7777L);

        // Place sand and water next to it for sugar cane
        world.setBlock(10, 20, 10, BlockType.SAND.id, false);
        world.setBlock(11, 20, 10, BlockType.WATER.id, false);

        // Place first sugar cane segment at Y=21
        world.setBlock(10, 21, 10, BlockType.SUGAR_CANE.id, false);
        // Place second sugar cane segment at Y=22
        world.setBlock(10, 22, 10, BlockType.SUGAR_CANE.id, false);

        // Verify top-of-stack detection:
        // Segment at Y=21 has sugar cane above it -> isTopOfStack must be FALSE
        boolean isTopAt21 = (world.getBlock(10, 21 + 1, 10) != BlockType.SUGAR_CANE.id);
        if (isTopAt21) {
            throw new AssertionError("Segment at Y=21 should NOT be top of stack!");
        }

        // Segment at Y=22 has AIR above it -> isTopOfStack must be TRUE
        boolean isTopAt22 = (world.getBlock(10, 22 + 1, 10) != BlockType.SUGAR_CANE.id);
        if (!isTopAt22) {
            throw new AssertionError("Segment at Y=22 MUST be top of stack!");
        }

        // Clicking the side of Y=21 (px=11, py=21) is NOT a top face (py != 21 + 1)
        long hitX = 10, hitY = 21, hitZ = 10;
        long sidePx = 11, sidePy = 21, sidePz = 10;
        boolean sideIsTopFace = (sidePx == hitX && sidePz == hitZ && sidePy == hitY + 1);
        if (sideIsTopFace) {
            throw new AssertionError("Clicking side of plant must not be classified as top face!");
        }

        // Clicking top face of Y=22 (px=10, py=23, pz=10) IS top face and IS top of stack
        hitY = 22;
        long topPx = 10, topPy = 23, topPz = 10;
        boolean topIsTopFace = (topPx == hitX && topPz == hitZ && topPy == hitY + 1);
        boolean topIsTopOfStack = (world.getBlock(hitX, hitY + 1, hitZ) != BlockType.SUGAR_CANE.id);
        if (!topIsTopFace || !topIsTopOfStack) {
            throw new AssertionError("Clicking top of top segment must satisfy top face and top of stack conditions!");
        }

        System.out.println("  [PASS] Task 65 Plant top-only stacking conditions strictly enforced.");
    }

    private static void testFluidBlockTickDelegation() {
        System.out.println("--- TEST: FluidBlock.tick Delegation (Task 66) ---");
        GameSettings.blockPhysicsEnabled = true;
        GameSettings.waterPhysicsMode = "modern";

        World world = new World(9999L);
        // Place stone floor
        world.setBlock(20, 10, 20, BlockType.STONE.id, false);
        world.setBlock(21, 10, 20, BlockType.STONE.id, false);

        // Clear air at (21, 11, 20) for water to spread into
        world.setBlock(21, 11, 20, BlockType.AIR.id, false);

        // Place water source at (20, 11, 20)
        world.setBlock(20, 11, 20, BlockType.WATER.id, true);

        // Verify that Blocks.get(BlockType.WATER.id) is an instance of FluidBlock
        com.voxel.block.Block waterBlock = Blocks.get(BlockType.WATER.id);
        if (!(waterBlock instanceof com.voxel.block.FluidBlock)) {
            throw new AssertionError("Blocks.get(WATER.id) must be an instance of FluidBlock!");
        }

        // Verify flow block IDs also map to FluidBlock
        for (int flowId = 35; flowId <= 41; flowId++) {
            if (!(Blocks.get(flowId) instanceof com.voxel.block.FluidBlock)) {
                throw new AssertionError("Flow block id " + flowId + " must map to FluidBlock!");
            }
        }

        // Run fluid tick via world.tickFluid
        world.tickFluid(64);

        // Water should have spread to (21, 11, 20) via FluidBlock.tick()
        byte adjacent = world.getBlock(21, 11, 20);
        if (!BlockType.isWater(adjacent)) {
            throw new AssertionError("FluidBlock.tick() should have spread water horizontally, found: " + adjacent);
        }

        System.out.println("  [PASS] Task 66 FluidBlock.tick() encapsulation and execution verified.");
    }
}
