package com.voxel.command.impl;

import com.voxel.command.Command;
import com.voxel.command.CommandContext;
import com.voxel.entity.Player;
import com.voxel.i18n.I18n;
import com.voxel.ui.ChatSystem.Suggestion;
import com.voxel.world.Chunk;
import com.voxel.world.World;

import java.util.ArrayList;
import java.util.List;

public class TeleportCommand implements Command {

    @Override
    public String getName() {
        return "tp";
    }

    @Override
    public List<String> getAliases() {
        return List.of("teleport");
    }

    @Override
    public String getDescription() {
        return "Teleport player to coordinates";
    }

    @Override
    public String getUsage() {
        return "/tp <x> <y> <z> or /tp <x> <z>";
    }

    @Override
    public void execute(CommandContext ctx) {
        String[] args = ctx.args();
        World world = ctx.world();
        Player player = ctx.player();

        if (args.length == 2) {
            try {
                double targetX = CommandContext.parseCoordinate(args[0], player.position.x);
                double targetZ = CommandContext.parseCoordinate(args[1], player.position.z);
                long bx = (long) Math.floor(targetX);
                long bz = (long) Math.floor(targetZ);
                long surfaceY = world.findSpawnY(bx, bz);
                teleportPlayer(world, player, targetX, surfaceY, targetZ);
                ctx.success(I18n.t("chat.tp_success", targetX, (double) surfaceY, targetZ));
            } catch (NumberFormatException e) {
                ctx.error(I18n.t("chat.coords_error"));
            }
        } else if (args.length >= 3) {
            try {
                double targetX = CommandContext.parseCoordinate(args[0], player.position.x);
                double targetY = CommandContext.parseCoordinate(args[1], player.position.y);
                double targetZ = CommandContext.parseCoordinate(args[2], player.position.z);
                teleportPlayer(world, player, targetX, targetY, targetZ);
                ctx.success(I18n.t("chat.tp_success", targetX, targetY, targetZ));
            } catch (NumberFormatException e) {
                ctx.error(I18n.t("chat.coords_error"));
            }
        } else {
            ctx.error(I18n.t("chat.tp_usage"));
        }
    }

    @Override
    public List<Suggestion> suggest(CommandContext ctx, String prefix, String currentArg) {
        List<Suggestion> suggestions = new ArrayList<>();
        if (currentArg.isEmpty()) {
            suggestions.add(new Suggestion(prefix + "~", "~", "Relative current coordinate"));
        }
        return suggestions;
    }

    public static void teleportPlayer(World world, Player player, double x, double y, double z) {
        long cx = Math.floorDiv((long) Math.floor(x), (long) Chunk.SIZE_X);
        long cy = Math.floorDiv((long) Math.floor(y), (long) Chunk.SIZE_Y);
        long cz = Math.floorDiv((long) Math.floor(z), (long) Chunk.SIZE_Z);
        world.generateInitialChunks(cx, cy, cz, 3, 2);
        player.position.set(x, y, z);
        player.velocity.set(0, 0, 0);
    }
}
