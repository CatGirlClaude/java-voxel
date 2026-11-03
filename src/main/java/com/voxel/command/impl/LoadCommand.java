package com.voxel.command.impl;

import com.voxel.command.Command;
import com.voxel.command.CommandContext;
import com.voxel.i18n.I18n;
import com.voxel.world.WorldSaveManager;

public class LoadCommand implements Command {

    @Override
    public String getName() {
        return "load";
    }

    @Override
    public String getDescription() {
        return "Reload current world from disk";
    }

    @Override
    public String getUsage() {
        return "/load";
    }

    @Override
    public void execute(CommandContext ctx) {
        if (ctx.world().loadWorld(ctx.player())) {
            ctx.success(I18n.t("chat.load_success", WorldSaveManager.getCurrentSaveName()));
        } else {
            ctx.error(I18n.t("chat.load_not_found", WorldSaveManager.getCurrentSaveName()));
        }
    }
}
