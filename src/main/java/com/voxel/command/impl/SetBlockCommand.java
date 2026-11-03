package com.voxel.command.impl;

import com.voxel.block.BlockType;
import com.voxel.command.Command;
import com.voxel.command.CommandContext;
import com.voxel.entity.Player;
import com.voxel.i18n.I18n;
import com.voxel.ui.ChatSystem.Suggestion;
import com.voxel.world.World;

import java.util.ArrayList;
import java.util.List;

public class SetBlockCommand implements Command {

    @Override
    public String getName() {
        return "setblock";
    }

    @Override
    public List<String> getAliases() {
        return List.of("set");
    }

    @Override
    public String getDescription() {
        return "Place a block at specific coordinates";
    }

    @Override
    public String getUsage() {
        return "/setblock <x> <y> <z> <block>";
    }

    @Override
    public void execute(CommandContext ctx) {
        String[] args = ctx.args();
        World world = ctx.world();
        Player player = ctx.player();

        if (args.length < 4) {
            ctx.error(I18n.t("chat.setblock_usage"));
            return;
        }

        try {
            double targetX = CommandContext.parseCoordinate(args[0], player.position.x);
            double targetY = CommandContext.parseCoordinate(args[1], player.position.y);
            double targetZ = CommandContext.parseCoordinate(args[2], player.position.z);
            BlockType bt = BlockType.byName(args[3]);
            if (bt == null) {
                ctx.error(I18n.t("chat.setblock_unknown", args[3]));
                return;
            }
            long bx = (long) Math.floor(targetX);
            long by = (long) Math.floor(targetY);
            long bz = (long) Math.floor(targetZ);
            world.setBlock(bx, by, bz, bt.id, true);
            ctx.success(I18n.t("chat.setblock_success", bt.getDisplayName(), bt.id, bx, by, bz));
        } catch (NumberFormatException e) {
            ctx.error(I18n.t("chat.coords_error"));
        }
    }

    @Override
    public List<Suggestion> suggest(CommandContext ctx, String prefix, String currentArg) {
        List<Suggestion> suggestions = new ArrayList<>();
        String[] args = ctx.args();
        if (args.length < 4) {
            if (currentArg.isEmpty()) {
                suggestions.add(new Suggestion(prefix + "~", "~", "Relative coordinate"));
            }
        } else {
            String lower = currentArg.toLowerCase();
            for (BlockType bt : BlockType.values()) {
                String name = bt.name().toLowerCase();
                if (name.startsWith(lower)) {
                    suggestions.add(new Suggestion(prefix + name, name, bt.name));
                }
            }
        }
        return suggestions;
    }
}
