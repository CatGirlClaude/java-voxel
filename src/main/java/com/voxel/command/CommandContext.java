package com.voxel.command;

import com.voxel.entity.Player;
import com.voxel.ui.ChatSystem;
import com.voxel.ui.HudRenderer;
import com.voxel.world.World;
import org.joml.Vector4f;

public record CommandContext(
        String commandLine,
        String aliasUsed,
        String[] args,
        World world,
        Player player,
        HudRenderer hud,
        ChatSystem chat
) {
    public void reply(String message, Vector4f color) {
        if (chat != null) {
            chat.addMessage(message, color);
        }
    }

    public void success(String message) {
        reply(message, ChatSystem.COLOR_SUCCESS);
    }

    public void error(String message) {
        reply(message, ChatSystem.COLOR_ERROR);
    }

    public void info(String message) {
        reply(message, ChatSystem.COLOR_INFO);
    }

    public void warn(String message) {
        reply(message, ChatSystem.COLOR_WARN);
    }

    public static double parseCoordinate(String token, double currentVal) throws NumberFormatException {
        if (token.equals("~")) {
            return currentVal;
        } else if (token.startsWith("~")) {
            double offset = Double.parseDouble(token.substring(1));
            return currentVal + offset;
        } else {
            return Double.parseDouble(token);
        }
    }
}
