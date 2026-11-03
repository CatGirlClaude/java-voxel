package com.voxel.command.impl;

import com.voxel.command.Command;
import com.voxel.command.CommandContext;
import com.voxel.entity.Player;
import com.voxel.i18n.I18n;

import java.util.List;

public class NoclipCommand implements Command {

    @Override
    public String getName() {
        return "noclip";
    }

    @Override
    public List<String> getAliases() {
        return List.of("nc");
    }

    @Override
    public String getDescription() {
        return "Toggle ghost movement through blocks";
    }

    @Override
    public String getUsage() {
        return "/noclip or /nc";
    }

    @Override
    public void execute(CommandContext ctx) {
        Player player = ctx.player();
        player.noclip = !player.noclip;
        if (player.noclip) {
            player.flying = true;
        }
        player.velocity.set(0, 0, 0);
        ctx.success(I18n.t("chat.noclip_mode", player.noclip ? I18n.t("options.on") : I18n.t("options.off")));
    }
}
