package com.voxel.command.impl;

import com.voxel.command.Command;
import com.voxel.command.CommandContext;
import com.voxel.ui.ChatSystem;
import com.voxel.ui.ChatSystem.Suggestion;
import com.voxel.util.Logger;
import org.joml.Vector4f;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LogsCommand implements Command {

    private static final String[] LOG_OPTS = {"10", "30", "50", "dump"};

    @Override
    public String getName() {
        return "logs";
    }

    @Override
    public List<String> getAliases() {
        return List.of("log");
    }

    @Override
    public String getDescription() {
        return "Inspect runtime logs or dump crash report";
    }

    @Override
    public String getUsage() {
        return "/logs [count|dump]";
    }

    @Override
    public void execute(CommandContext ctx) {
        String[] args = ctx.args();

        if (args.length > 0 && args[0].equalsIgnoreCase("dump")) {
            Map<String, String> diag = new HashMap<>();
            diag.put("Player Position", String.format("%.2f, %.2f, %.2f", ctx.player().position.x, ctx.player().position.y, ctx.player().position.z));
            diag.put("World Seed", String.valueOf(ctx.world().getSeed()));
            Path p = Logger.dumpCrashReport("Dump from command /logs dump", null, diag);
            ctx.success("Saved diagnostic report: " + p.getFileName());
        } else {
            int count = 6;
            if (args.length > 0) {
                try {
                    count = Integer.parseInt(args[0]);
                } catch (Exception ignored) {}
            }
            ctx.reply("=== Recent logs (logs/latest.log) ===", ChatSystem.COLOR_HELP);
            List<String> list = Logger.getRecentLogs(Math.min(15, count));
            for (String l : list) {
                Vector4f col = l.contains("/ERROR]") ? ChatSystem.COLOR_ERROR : (l.contains("/WARN]") ? ChatSystem.COLOR_HELP : ChatSystem.COLOR_INFO);
                ctx.reply(l.length() > 65 ? l.substring(0, 62) + "..." : l, col);
            }
        }
    }

    @Override
    public List<Suggestion> suggest(CommandContext ctx, String prefix, String currentArg) {
        List<Suggestion> suggestions = new ArrayList<>();
        for (String l : LOG_OPTS) {
            if (l.startsWith(currentArg.toLowerCase())) {
                suggestions.add(new Suggestion(prefix + l, l, "Log action"));
            }
        }
        return suggestions;
    }
}
