package com.voxel.command.impl;

import com.voxel.command.Command;
import com.voxel.command.CommandContext;
import com.voxel.entity.Player;
import com.voxel.i18n.I18n;

import java.util.List;

public class PositionCommand implements Command {

    @Override
    public String getName() {
        return "pos";
    }

    @Override
    public List<String> getAliases() {
        return List.of("where");
    }

    @Override
    public String getDescription() {
        return "Display current player coordinates";
    }

    @Override
    public String getUsage() {
        return "/pos";
    }

    @Override
    public void execute(CommandContext ctx) {
        Player player = ctx.player();
        ctx.info(I18n.t("chat.pos", player.position.x, player.position.y, player.position.z));
    }
}
