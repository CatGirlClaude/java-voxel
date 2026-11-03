package com.voxel.command.impl;

import com.voxel.command.Command;
import com.voxel.command.CommandContext;
import com.voxel.i18n.I18n;
import com.voxel.ui.ChatSystem;
import com.voxel.ui.ChatSystem.Suggestion;
import com.voxel.ui.HudRenderer;
import com.voxel.world.World;

import java.util.ArrayList;
import java.util.List;

public class BenchmarkCommand implements Command {

    private static final String[] BENCH_OPTS = {"16", "32", "64", "128", "live"};

    @Override
    public String getName() {
        return "bench";
    }

    @Override
    public List<String> getAliases() {
        return List.of("benchmark", "perf");
    }

    @Override
    public String getDescription() {
        return "Measure terrain generation performance";
    }

    @Override
    public String getUsage() {
        return "/bench <chunks|live>";
    }

    @Override
    public void execute(CommandContext ctx) {
        String[] args = ctx.args();
        World world = ctx.world();
        HudRenderer hud = ctx.hud();

        int count = 512;
        if (args.length > 0) {
            String arg = args[0].toLowerCase();
            if (arg.equals("live") || arg.equals("hud") || arg.equals("toggle")) {
                if (hud != null) {
                    hud.showBenchmarkOverlay = !hud.showBenchmarkOverlay;
                    ctx.info(I18n.t("chat.bench_hud", hud.showBenchmarkOverlay ? I18n.t("options.on") : I18n.t("options.off")));
                }
                return;
            }
            try {
                count = Math.max(32, Math.min(4096, Integer.parseInt(args[0])));
            } catch (NumberFormatException ignored) {}
        }

        ctx.info(I18n.t("chat.bench_running", count, world.getWorkerThreadCount()));
        final int finalCount = count;
        world.runBenchmarkAsync(finalCount, result -> {
            if (hud != null) {
                hud.setBenchmarkResult(result);
            }
            ctx.reply(I18n.t("chat.bench_complete"), ChatSystem.COLOR_HELP);
            ctx.info(I18n.t("chat.bench_tested", result.chunkCount(), result.workerThreads()));
            ctx.success(I18n.t("chat.bench_gen", result.terrainGenMs(), result.terrainGenRate(), result.avgTerrainGenMs()));
            ctx.success(I18n.t("chat.bench_mesh", result.meshingMs(), result.meshingRate(), result.avgMeshingMs()));
            ctx.info(I18n.t("chat.bench_total", result.totalRate(), result.totalTimeMs()));
            ctx.reply(I18n.t("chat.bench_rating", result.performanceRating()), ChatSystem.COLOR_HELP);
        });
    }

    @Override
    public List<Suggestion> suggest(CommandContext ctx, String prefix, String currentArg) {
        List<Suggestion> suggestions = new ArrayList<>();
        for (String b : BENCH_OPTS) {
            if (b.startsWith(currentArg.toLowerCase())) {
                suggestions.add(new Suggestion(prefix + b, b, "Benchmark target"));
            }
        }
        return suggestions;
    }
}
