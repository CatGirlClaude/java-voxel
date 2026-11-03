package com.voxel.command.impl;

import com.voxel.command.Command;
import com.voxel.command.CommandContext;
import com.voxel.i18n.I18n;
import com.voxel.ui.HudRenderer;

public class DebugCommand implements Command {

    @Override
    public String getName() {
        return "debug";
    }

    @Override
    public String getDescription() {
        return "Toggle F3 debug overlay";
    }

    @Override
    public String getUsage() {
        return "/debug";
    }

    @Override
    public void execute(CommandContext ctx) {
        HudRenderer hud = ctx.hud();
        if (hud != null) {
            hud.showDebug = !hud.showDebug;
            ctx.info(I18n.t("chat.debug_panel", hud.showDebug ? I18n.t("options.on") : I18n.t("options.off")));
        }
    }
}
