package com.voxel.command.impl;

import com.voxel.command.Command;
import com.voxel.command.CommandContext;
import com.voxel.entity.Player;
import com.voxel.i18n.I18n;
import com.voxel.ui.ChatSystem.Suggestion;
import com.voxel.world.Biome;
import com.voxel.world.World;

import java.util.ArrayList;
import java.util.List;

public class LocateBiomeCommand implements Command {

    private static final String[] BIOMES = {
            "ocean", "deep_ocean", "frozen_ocean", "beach", "plains", "forest",
            "birch_forest", "desert", "mesa", "taiga", "snowy_tundra", "jungle", "swamp", "mountains"
    };

    @Override
    public String getName() {
        return "locate";
    }

    @Override
    public List<String> getAliases() {
        return List.of("biome", "biom");
    }

    @Override
    public String getDescription() {
        return "Locate nearest biome coordinates";
    }

    @Override
    public String getUsage() {
        return "/locate <biome_name>";
    }

    @Override
    public void execute(CommandContext ctx) {
        String[] args = ctx.args();
        World world = ctx.world();
        Player player = ctx.player();

        long bx = (long) Math.floor(player.position.x);
        long bz = (long) Math.floor(player.position.z);

        if (args.length < 1) {
            Biome cur = world.getBiome(bx, bz);
            ctx.info(I18n.t("chat.locate_current", cur.getDisplayName()));
            return;
        }

        String arg = args[0].toLowerCase();
        Biome target = switch (arg) {
            case "ocean", "morze" -> Biome.OCEAN;
            case "deep_ocean", "deepocean", "gleboki_ocean" -> Biome.DEEP_OCEAN;
            case "frozen_ocean", "frozenocean", "zamarzniety_ocean" -> Biome.FROZEN_OCEAN;
            case "beach", "plaza" -> Biome.BEACH;
            case "desert", "pustynia" -> Biome.DESERT;
            case "mesa", "badlands", "kaniony" -> Biome.MESA;
            case "snow", "tundra", "snieg" -> Biome.SNOWY_TUNDRA;
            case "taiga", "tajga" -> Biome.TAIGA;
            case "jungle", "dzungla" -> Biome.JUNGLE;
            case "birch", "brzoza" -> Biome.BIRCH_FOREST;
            case "forest", "las" -> Biome.FOREST;
            case "plains", "rowniny" -> Biome.PLAINS;
            case "mountains", "gory" -> Biome.MOUNTAINS;
            default -> null;
        };

        if (target == null) {
            ctx.error(I18n.t("chat.locate_unknown", arg));
            return;
        }

        long[] found = world.locateBiome(target, bx, bz);
        if (found != null) {
            long sy = world.findSpawnY(found[0], found[1]);
            ctx.success(I18n.t("chat.locate_found", target.getDisplayName(), found[0], sy, found[1], found[0], sy, found[1]));
        } else {
            ctx.error(I18n.t("chat.locate_not_found", target.getDisplayName()));
        }
    }

    @Override
    public List<Suggestion> suggest(CommandContext ctx, String prefix, String currentArg) {
        List<Suggestion> suggestions = new ArrayList<>();
        for (String b : BIOMES) {
            if (b.startsWith(currentArg.toLowerCase())) {
                suggestions.add(new Suggestion(prefix + b, b, "Biome name"));
            }
        }
        return suggestions;
    }
}
