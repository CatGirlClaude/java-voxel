package com.voxel.command.impl;

import com.voxel.command.Command;
import com.voxel.command.CommandContext;
import com.voxel.i18n.I18n;

public class SpawnCommand implements Command {

    @Override
    public String getName() {
        return "spawn";
    }

    @Override
    public String getDescription() {
        return "Teleport player to world spawn";
    }

    @Override
    public String getUsage() {
        return "/spawn";
    }

    @Override
    public void execute(CommandContext ctx) {
        long sy = ctx.world().findSpawnY(0, 0);
        TeleportCommand.teleportPlayer(ctx.world(), ctx.player(), 0.5, sy, 0.5);
        ctx.success(I18n.t("chat.spawn_tp", sy));
    }
}
