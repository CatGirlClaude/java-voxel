package com.voxel.command.impl;

import com.voxel.command.Command;
import com.voxel.command.CommandContext;
import com.voxel.i18n.I18n;
import com.voxel.ui.ChatSystem;

import java.util.List;

public class HelpCommand implements Command {

    @Override
    public String getName() {
        return "help";
    }

    @Override
    public List<String> getAliases() {
        return List.of("pomoc", "?");
    }

    @Override
    public String getDescription() {
        return "List all available chat commands and shortcuts";
    }

    @Override
    public String getUsage() {
        return "/help";
    }

    @Override
    public void execute(CommandContext ctx) {
        ctx.reply(I18n.t("chat.help_header"), ChatSystem.COLOR_HELP);
        ctx.reply(I18n.t("chat.help_setblock"), ChatSystem.COLOR_HELP);
        ctx.reply(I18n.t("chat.help_tp_xyz"), ChatSystem.COLOR_HELP);
        ctx.reply(I18n.t("chat.help_tp_xz"), ChatSystem.COLOR_HELP);
        ctx.reply(I18n.t("chat.help_locate"), ChatSystem.COLOR_HELP);
        ctx.reply(I18n.t("chat.help_reset"), ChatSystem.COLOR_HELP);
        ctx.reply(I18n.t("chat.help_noclip"), ChatSystem.COLOR_HELP);
        ctx.reply(I18n.t("chat.help_fly"), ChatSystem.COLOR_HELP);
        ctx.reply(I18n.t("chat.help_spawn"), ChatSystem.COLOR_HELP);
        ctx.reply(I18n.t("chat.help_time"), ChatSystem.COLOR_HELP);
        ctx.reply(I18n.t("chat.help_debug"), ChatSystem.COLOR_HELP);
        ctx.reply(I18n.t("chat.help_saveload"), ChatSystem.COLOR_HELP);
        ctx.reply(I18n.t("chat.help_worldsetting"), ChatSystem.COLOR_HELP);
        ctx.reply(I18n.t("chat.help_bench"), ChatSystem.COLOR_HELP);
        ctx.reply(I18n.t("chat.help_logs"), ChatSystem.COLOR_HELP);
    }
}
