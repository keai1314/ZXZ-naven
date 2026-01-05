package com.heypixel.heypixelmod.obsoverlay.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

import java.awt.Color;

public class TeamUtil {
    private static final Minecraft mc = Minecraft.getInstance();

    public static Color getTeamColor(Player player, float alpha) {
        Integer teamColor = player.getTeamColor();
        if (teamColor != null) {
            float r = (float) (teamColor >> 16 & 0xFF) / 255.0f;
            float g = (float) (teamColor >> 8 & 0xFF) / 255.0f;
            float b = (float) (teamColor & 0xFF) / 255.0f;
            return new Color(r, g, b, alpha);
        }
        return new Color(255, 255, 255, (int)(alpha * 255));
    }

    public static boolean isSameTeam(Player player) {
        return com.heypixel.heypixelmod.obsoverlay.modules.impl.misc.Teams.isSameTeam(player);
    }

    public static boolean isFriend(Player player) {
        return com.heypixel.heypixelmod.obsoverlay.utils.FriendManager.isFriend(player);
    }

    public static boolean isBot(Player player) {
        if (player == mc.player) {
            return false;
        }
        return false;
    }
}
