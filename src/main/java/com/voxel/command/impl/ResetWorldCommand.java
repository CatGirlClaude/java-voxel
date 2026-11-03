package com.voxel.command.impl;

import com.voxel.command.Command;
import com.voxel.command.CommandContext;
import com.voxel.i18n.I18n;
import com.voxel.ui.ChatSystem.Suggestion;
import com.voxel.world.World;

import java.util.ArrayList;
import java.util.List;

public class ResetWorldCommand implements Command {

    private static final String[] RESET_OPTS = {"same", "random"};

    @Override
    public String getName() {
        return "reset";
    }

    @Override
    public List<String> getAliases() {
        return List.of("new", "regen", "restart", "resetworld");
    }

    @Override
    public String getDescription() {
        return "Regenerate world with seed or random";
    }

    @Override
    public String getUsage() {
        return "/reset [seed|same|random]";
    }

    @Override
    public void execute(CommandContext ctx) {
        String[] args = ctx.args();
        World world = ctx.world();

        long newSeed;
        if (args.length >= 1) {
            String arg = args[0].trim();
            if (arg.equalsIgnoreCase("same") || arg.equalsIgnoreCase("current")) {
                newSeed = world.getSeed();
            } else {
                try {
                    newSeed = Long.parseLong(arg);
                } catch (NumberFormatException e) {
                    newSeed = arg.hashCode();
                }
            }
        } else {
            newSeed = System.nanoTime() ^ (System.currentTimeMillis() * 6364136223846793005L);
        }

        world.resetWorld(ctx.player(), newSeed);
        ctx.success(I18n.t("chat.reset_done", newSeed, ctx.player().position.y));
    }

    @Override
    public List<Suggestion> suggest(CommandContext ctx, String prefix, String currentArg) {
        List<Suggestion> suggestions = new ArrayList<>();
        for (String r : RESET_OPTS) {
            if (r.startsWith(currentArg.toLowerCase())) {
                suggestions.add(new Suggestion(prefix + r, r, "Seed reset mode"));
            }
        }
        return suggestions;
    }
}
