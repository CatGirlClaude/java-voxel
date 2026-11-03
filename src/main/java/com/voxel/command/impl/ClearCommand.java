package com.voxel.command.impl;

import com.voxel.command.Command;
import com.voxel.command.CommandContext;

import java.util.List;

public class ClearCommand implements Command {

    @Override
    public String getName() {
        return "clear";
    }

    @Override
    public List<String> getAliases() {
        return List.of("cls");
    }

    @Override
    public String getDescription() {
        return "Clear chat messages";
    }

    @Override
    public String getUsage() {
        return "/clear";
    }

    @Override
    public void execute(CommandContext ctx) {
        if (ctx.chat() != null) {
            ctx.chat().clearMessages();
        }
    }
}
