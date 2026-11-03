package com.voxel.command.impl;

import com.voxel.command.Command;
import com.voxel.command.CommandContext;
import com.voxel.i18n.I18n;
import com.voxel.world.WorldSaveManager;

public class SaveCommand implements Command {

    @Override
    public String getName() {
        return "save";
    }

    @Override
    public String getDescription() {
        return "Save current world";
    }

    @Override
    public String getUsage() {
        return "/save";
    }

    @Override
    public void execute(CommandContext ctx) {
        if (ctx.world().saveWorld(ctx.player())) {
            ctx.success(I18n.t("chat.save_success", WorldSaveManager.getCurrentSaveName()));
        } else {
            ctx.error(I18n.t("chat.save_error"));
        }
    }
}
