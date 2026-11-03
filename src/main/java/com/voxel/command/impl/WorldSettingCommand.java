package com.voxel.command.impl;

import com.voxel.command.Command;
import com.voxel.command.CommandContext;
import com.voxel.config.GameSettings;
import com.voxel.ui.ChatSystem;
import com.voxel.ui.ChatSystem.Suggestion;

import java.util.ArrayList;
import java.util.List;

public class WorldSettingCommand implements Command {

    @Override
    public String getName() {
        return "worldsetting";
    }

    @Override
    public List<String> getAliases() {
        return List.of("ws");
    }

    @Override
    public String getDescription() {
        return "Configure world simulation and physics settings";
    }

    @Override
    public String getUsage() {
        return "/worldsetting <water|physics> [value]";
    }

    @Override
    public void execute(CommandContext ctx) {
        String[] args = ctx.args();

        if (args.length == 0) {
            ctx.reply("=== World Settings (/worldsetting) ===", ChatSystem.COLOR_HELP);
            ctx.info(String.format("Water physics mode: %s (modern / preclassic / terraria)", GameSettings.waterPhysicsMode));
            ctx.info(String.format("Block gravity physics: %s", GameSettings.blockPhysicsEnabled ? "ENABLED" : "DISABLED"));
            ctx.reply("Usage: /worldsetting water <modern|preclassic|terraria> or /worldsetting physics <on|off>", ChatSystem.COLOR_HELP);
            return;
        }

        String sub = args[0].toLowerCase();
        if (sub.equals("water") || sub.equals("woda")) {
            if (args.length > 1) {
                String mode = args[1].toLowerCase();
                if (mode.equals("modern")) {
                    GameSettings.waterPhysicsMode = "modern";
                    GameSettings.save();
                    ctx.success("Water physics mode set to: MODERN MINECRAFT (Flow levels 1..7 & sources).");
                } else if (mode.equals("preclassic") || mode.equals("classic")) {
                    GameSettings.waterPhysicsMode = "preclassic";
                    GameSettings.save();
                    ctx.success("Water physics mode set to: PRE-CLASSIC (Infinite source spreading).");
                } else if (mode.equals("terraria") || mode.equals("ter")) {
                    GameSettings.waterPhysicsMode = "terraria";
                    GameSettings.save();
                    ctx.success("Water physics mode set to: TERRARIA (Finite volume liquid conservation).");
                } else {
                    ctx.error("Unknown water mode: " + mode + ". Choose 'modern', 'preclassic', or 'terraria'.");
                }
            } else {
                ctx.info("Current water physics mode: " + GameSettings.waterPhysicsMode + ". Usage: /worldsetting water <modern|preclassic|terraria>");
            }
        } else if (sub.equals("physics") || sub.equals("fizyka")) {
            if (args.length > 1) {
                String val = args[1].toLowerCase();
                if (val.equals("on") || val.equals("true") || val.equals("wlacz")) {
                    GameSettings.blockPhysicsEnabled = true;
                    GameSettings.save();
                    ctx.success("Block physics: ENABLED.");
                } else if (val.equals("off") || val.equals("false") || val.equals("wylacz")) {
                    GameSettings.blockPhysicsEnabled = false;
                    GameSettings.save();
                    ctx.info("Block physics: DISABLED.");
                } else {
                    ctx.error("Usage: /worldsetting physics <on|off>");
                }
            } else {
                GameSettings.blockPhysicsEnabled = !GameSettings.blockPhysicsEnabled;
                GameSettings.save();
                ctx.info("Block physics: " + (GameSettings.blockPhysicsEnabled ? "ENABLED" : "DISABLED"));
            }
        } else {
            ctx.error("Unknown option: " + sub + ". Usage: /worldsetting water <modern|preclassic|terraria> or /worldsetting physics <on|off>");
        }
    }

    @Override
    public List<Suggestion> suggest(CommandContext ctx, String prefix, String currentArg) {
        List<Suggestion> suggestions = new ArrayList<>();
        String[] args = ctx.args();
        if (args.length <= 1) {
            String[] wsOptions = {"water", "physics"};
            for (String opt : wsOptions) {
                if (opt.startsWith(currentArg.toLowerCase())) {
                    suggestions.add(new Suggestion(prefix + opt + " ", opt, "World setting category"));
                }
            }
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("water") || args[0].equalsIgnoreCase("woda"))) {
            String[] waterModes = {"modern", "preclassic", "terraria"};
            for (String m : waterModes) {
                if (m.startsWith(currentArg.toLowerCase())) {
                    suggestions.add(new Suggestion(prefix + m, m, "Water simulation mode"));
                }
            }
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("physics") || args[0].equalsIgnoreCase("fizyka"))) {
            String[] physModes = {"on", "off"};
            for (String p : physModes) {
                if (p.startsWith(currentArg.toLowerCase())) {
                    suggestions.add(new Suggestion(prefix + p, p, "Toggle physics"));
                }
            }
        }
        return suggestions;
    }
}
