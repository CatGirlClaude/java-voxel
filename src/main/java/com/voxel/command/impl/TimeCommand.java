package com.voxel.command.impl;

import com.voxel.command.Command;
import com.voxel.command.CommandContext;
import com.voxel.i18n.I18n;
import com.voxel.ui.ChatSystem.Suggestion;
import com.voxel.world.World;

import java.util.ArrayList;
import java.util.List;

public class TimeCommand implements Command {

    private static final String[] TIME_OPTIONS = {"day", "noon", "sunset", "night", "pause", "resume", "set"};

    @Override
    public String getName() {
        return "time";
    }

    @Override
    public String getDescription() {
        return "Set or pause day/night cycle";
    }

    @Override
    public String getUsage() {
        return "/time <day|noon|sunset|night|pause|resume|0..24>";
    }

    @Override
    public void execute(CommandContext ctx) {
        String[] args = ctx.args();
        World world = ctx.world();

        if (args.length < 1) {
            ctx.error(I18n.t("chat.time_usage"));
            return;
        }

        String arg = args[0].toLowerCase();
        if (arg.equals("set") && args.length >= 2) {
            arg = args[1].toLowerCase();
        }

        switch (arg) {
            case "day", "morning", "rano", "dzien" -> {
                world.timeOfDay = 0.30f;
                ctx.success(I18n.t("chat.time_morning"));
            }
            case "noon", "poludnie" -> {
                world.timeOfDay = 0.50f;
                ctx.success(I18n.t("chat.time_noon"));
            }
            case "sunset", "zachod" -> {
                world.timeOfDay = 0.77f;
                ctx.success(I18n.t("chat.time_sunset"));
            }
            case "night", "noc", "midnight" -> {
                world.timeOfDay = 0.0f;
                ctx.success(I18n.t("chat.time_night"));
            }
            case "pause", "stop", "pauza" -> {
                world.dayCyclePaused = true;
                ctx.info(I18n.t("chat.time_paused"));
            }
            case "resume", "start", "wznow" -> {
                world.dayCyclePaused = false;
                ctx.info(I18n.t("chat.time_resumed"));
            }
            default -> {
                try {
                    float hour = Float.parseFloat(arg);
                    world.timeOfDay = ((hour % 24.0f) + 24.0f) % 24.0f / 24.0f;
                    ctx.success(I18n.t("chat.time_hour", hour));
                } catch (NumberFormatException e) {
                    ctx.error(I18n.t("chat.time_unknown", arg));
                }
            }
        }
    }

    @Override
    public List<Suggestion> suggest(CommandContext ctx, String prefix, String currentArg) {
        List<Suggestion> suggestions = new ArrayList<>();
        for (String opt : TIME_OPTIONS) {
            if (opt.startsWith(currentArg.toLowerCase())) {
                suggestions.add(new Suggestion(prefix + opt, opt, "Time parameter"));
            }
        }
        return suggestions;
    }
}
