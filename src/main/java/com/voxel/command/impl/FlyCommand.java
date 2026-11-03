package com.voxel.command.impl;

import com.voxel.command.Command;
import com.voxel.command.CommandContext;
import com.voxel.entity.Player;
import com.voxel.i18n.I18n;

public class FlyCommand implements Command {

    @Override
    public String getName() {
        return "fly";
    }

    @Override
    public String getDescription() {
        return "Toggle creative flight mode";
    }

    @Override
    public String getUsage() {
        return "/fly";
    }

    @Override
    public void execute(CommandContext ctx) {
        Player player = ctx.player();
        player.flying = !player.flying;
        if (!player.flying) {
            player.noclip = false;
        }
        player.velocity.y = 0;
        ctx.success(I18n.t("chat.fly_mode", player.flying ? I18n.t("options.on") : I18n.t("options.off")));
    }
}
