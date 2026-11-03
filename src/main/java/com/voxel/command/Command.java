package com.voxel.command;

import com.voxel.ui.ChatSystem.Suggestion;

import java.util.Collections;
import java.util.List;

public interface Command {

    String getName();

    default List<String> getAliases() {
        return Collections.emptyList();
    }

    String getDescription();

    String getUsage();

    void execute(CommandContext ctx) throws Exception;

    default List<Suggestion> suggest(CommandContext ctx, String prefix, String currentArg) {
        return Collections.emptyList();
    }
}
