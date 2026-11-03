package com.voxel.command;

import com.voxel.command.impl.*;
import com.voxel.entity.Player;
import com.voxel.i18n.I18n;
import com.voxel.ui.ChatSystem;
import com.voxel.ui.ChatSystem.Suggestion;
import com.voxel.ui.HudRenderer;
import com.voxel.world.World;

import java.util.*;

public class CommandRegistry {

    private final Map<String, Command> commandsByName = new LinkedHashMap<>();
    private final List<Command> registeredCommands = new ArrayList<>();

    public CommandRegistry() {
        registerDefaults();
    }

    public void registerDefaults() {
        register(new TeleportCommand());
        register(new SpawnCommand());
        register(new FlyCommand());
        register(new NoclipCommand());
        register(new DebugCommand());
        register(new TimeCommand());
        register(new BenchmarkCommand());
        register(new SaveCommand());
        register(new LoadCommand());
        register(new SetBlockCommand());
        register(new ResetWorldCommand());
        register(new ClearCommand());
        register(new PositionCommand());
        register(new LocateBiomeCommand());
        register(new WorldSettingCommand());
        register(new LogsCommand());
        register(new HelpCommand());
    }

    public void register(Command command) {
        String primary = command.getName().toLowerCase();
        commandsByName.put(primary, command);
        for (String alias : command.getAliases()) {
            commandsByName.put(alias.toLowerCase(), command);
        }
        if (!registeredCommands.contains(command)) {
            registeredCommands.add(command);
        }
    }

    public Command get(String name) {
        if (name == null) return null;
        if (name.startsWith("/")) name = name.substring(1);
        return commandsByName.get(name.toLowerCase());
    }

    public List<Command> getAll() {
        return Collections.unmodifiableList(registeredCommands);
    }

    public boolean execute(String cmdLine, World world, Player player, HudRenderer hud, ChatSystem chat) {
        if (cmdLine == null || cmdLine.trim().isEmpty()) return false;
        String trimmed = cmdLine.trim();
        if (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1).trim();
        }

        String[] tokens = trimmed.split("\\s+");
        if (tokens.length == 0) return false;

        String cmdName = tokens[0].toLowerCase();
        Command cmd = commandsByName.get(cmdName);
        if (cmd == null) {
            if (chat != null) {
                chat.addMessage(I18n.t("chat.unknown_cmd", cmdName), ChatSystem.COLOR_ERROR);
            }
            return false;
        }

        String[] args = new String[tokens.length - 1];
        System.arraycopy(tokens, 1, args, 0, args.length);

        CommandContext ctx = new CommandContext(trimmed, cmdName, args, world, player, hud, chat);
        try {
            cmd.execute(ctx);
            return true;
        } catch (Exception e) {
            if (chat != null) {
                chat.addMessage("Error executing /" + cmdName + ": " + e.getMessage(), ChatSystem.COLOR_ERROR);
            }
            return false;
        }
    }

    public List<Suggestion> getSuggestions(String fullText, World world, Player player, HudRenderer hud, ChatSystem chat) {
        List<Suggestion> suggestions = new ArrayList<>();
        if (fullText == null || fullText.isEmpty()) return suggestions;

        String[] parts = fullText.split("\\s+", -1);
        String cmdToken = parts[0].toLowerCase();

        if (parts.length <= 1) {
            // Suggest matching commands
            for (Command cmd : registeredCommands) {
                String cmdName = "/" + cmd.getName();
                if (cmdName.toLowerCase().startsWith(cmdToken)) {
                    boolean hasArgs = cmd.getUsage().contains("<") || cmd.getUsage().contains("[");
                    suggestions.add(new Suggestion(hasArgs ? cmdName + " " : cmdName, cmdName, cmd.getDescription()));
                }
                for (String alias : cmd.getAliases()) {
                    String aliasName = "/" + alias;
                    if (aliasName.toLowerCase().startsWith(cmdToken) && !aliasName.equalsIgnoreCase(cmdName)) {
                        boolean hasArgs = cmd.getUsage().contains("<") || cmd.getUsage().contains("[");
                        suggestions.add(new Suggestion(hasArgs ? aliasName + " " : aliasName, aliasName, cmd.getDescription()));
                    }
                }
            }
        } else {
            String commandName = cmdToken.startsWith("/") ? cmdToken.substring(1) : cmdToken;
            Command cmd = commandsByName.get(commandName);
            if (cmd != null) {
                String argToken = parts[parts.length - 1].toLowerCase();
                String prefix = fullText.substring(0, fullText.lastIndexOf(parts[parts.length - 1]));
                String[] args = new String[parts.length - 1];
                System.arraycopy(parts, 1, args, 0, args.length);
                CommandContext ctx = new CommandContext(fullText, commandName, args, world, player, hud, chat);
                List<Suggestion> cmdSuggestions = cmd.suggest(ctx, prefix, argToken);
                if (cmdSuggestions != null) {
                    suggestions.addAll(cmdSuggestions);
                }
            }
        }

        return suggestions;
    }
}
