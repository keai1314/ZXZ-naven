package com.heypixel.heypixelmod.modules.impl.render;

import com.heypixel.heypixelmod.events.api.EventTarget;
import com.heypixel.heypixelmod.events.api.types.EventType;
import com.heypixel.heypixelmod.events.impl.EventPacket;
import com.heypixel.heypixelmod.events.impl.EventRender2D;
import com.heypixel.heypixelmod.events.impl.EventRespawn;
import com.heypixel.heypixelmod.events.impl.EventShader;
import com.heypixel.heypixelmod.modules.Category;
import com.heypixel.heypixelmod.modules.Module;
import com.heypixel.heypixelmod.modules.ModuleInfo;
import com.heypixel.heypixelmod.modules.impl.misc.KillSay;
import com.heypixel.heypixelmod.utils.RenderUtils;
import com.heypixel.heypixelmod.utils.renderer.Fonts;
import com.heypixel.heypixelmod.utils.renderer.text.CustomTextRenderer;
import com.heypixel.heypixelmod.values.ValueBuilder;
import com.heypixel.heypixelmod.values.impl.FloatValue;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.resources.ResourceLocation;

import java.awt.Color;
import java.util.UUID;

@ModuleInfo(
        name = "SessionInfo",
        description = "SessionInfo for your session.",
        category = Category.RENDER
)
public class SessionInfo extends Module {

    private final FloatValue x = ValueBuilder.create(this, "X")
            .setDefaultFloatValue(10.0F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(2000.0F)
            .setFloatStep(1.0F)
            .build()
            .getFloatValue();

    private final FloatValue y = ValueBuilder.create(this, "Y")
            .setDefaultFloatValue(100.0F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(2000.0F)
            .setFloatStep(1.0F)
            .build()
            .getFloatValue();

    private final FloatValue scale = ValueBuilder.create(this, "Scale")
            .setDefaultFloatValue(0.4F)
            .setMinFloatValue(0.2F)
            .setMaxFloatValue(1.0F)
            .setFloatStep(0.01F)
            .build()
            .getFloatValue();

    private final FloatValue backgroundAlpha = ValueBuilder.create(this, "Background Alpha")
            .setDefaultFloatValue(120.0F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(255.0F)
            .setFloatStep(1.0F)
            .build()
            .getFloatValue();

    private final FloatValue radius = ValueBuilder.create(this, "Radius")
            .setDefaultFloatValue(5.0F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(10.0F)
            .setFloatStep(0.5F)
            .build()
            .getFloatValue();
    private long sessionStartTime = 0;
    private int killCount = 0;

    @Override
    public void onEnable() {
        super.onEnable();
        sessionStartTime = System.currentTimeMillis();
        killCount = 0;
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }

    @EventTarget
    public void onPacket(EventPacket e) {
        if (e.getPacket() instanceof ClientboundPlayerInfoRemovePacket &&
            e.getType() == EventType.RECEIVE &&
            mc.getConnection() != null) {
            ClientboundPlayerInfoRemovePacket packet = (ClientboundPlayerInfoRemovePacket) e.getPacket();
            for (UUID entry : packet.profileIds()) {
                PlayerInfo playerInfo = mc.getConnection().getPlayerInfo(entry);
                if (playerInfo != null) {
                    String playerName = playerInfo.getProfile().getName();
                    if (KillSay.attackedPlayers.contains(playerName)) {
                        killCount++;
                    }
                }
            }
        }
    }

    @EventTarget
    public void onShader(EventShader e) {
        if (mc.player == null) return;
        CustomTextRenderer font = Fonts.opensans;
        double textScale = this.scale.getCurrentValue();
        float avatarSize = (float) (32.0F * textScale);
        float padding = (float) (8.0F * textScale);
        float textSpacing = (float) (4.0F * textScale);
        String playTimeText = "Play Time: " + getPlayTime();
        String killsText = "Kills: " + killCount;
        float textWidth = Math.max(
                font.getWidth(playTimeText, textScale),
                font.getWidth(killsText, textScale)
        );
        float textHeight = (float) font.getHeight(true, textScale);
        float shadowExtra = (float) Math.ceil(1.0F * textScale);
        float totalWidthF = avatarSize + padding * 3 + textWidth + shadowExtra;
        float totalHeightF = Math.max(avatarSize, textHeight * 2 + textSpacing + shadowExtra) + padding * 2;
        float totalWidth = (float) Math.ceil(totalWidthF);
        float totalHeight = (float) Math.ceil(totalHeightF);
        float posX = (float) Math.floor(this.x.getCurrentValue());
        float posY = (float) Math.floor(this.y.getCurrentValue());
        float r = this.radius.getCurrentValue();
        if (e.getType() == EventType.BLUR) {
            RenderUtils.drawRoundedRect(e.getStack(), posX, posY, totalWidth, totalHeight, r, Integer.MIN_VALUE);
        }
        if (e.getType() == EventType.SHADOW) {
            int bgA = (int) Math.max(0.0F, Math.min(255.0F, this.backgroundAlpha.getCurrentValue()));
            if (bgA > 0) {
                RenderUtils.drawRoundedRect(e.getStack(), posX, posY, totalWidth, totalHeight, r, new Color(0, 0, 0, Math.min(bgA, 180)).getRGB());
            }
        }
    }

    @EventTarget
    public void onRender2D(EventRender2D e) {
        if (mc.player == null) return;
        CustomTextRenderer font = Fonts.opensans;
        double textScale = this.scale.getCurrentValue();
        float avatarSize = (float) (32.0F * textScale);
        float padding = (float) (8.0F * textScale);
        float textSpacing = (float) (4.0F * textScale);
        String playTimeText = "Play Time: " + getPlayTime();
        String killsText = "Kills: " + killCount;
        float textWidth = Math.max(
                font.getWidth(playTimeText, textScale),
                font.getWidth(killsText, textScale)
        );
        float textHeight = (float) font.getHeight(true, textScale);
        float shadowExtra = (float) Math.ceil(1.0F * textScale);
        float totalWidthF = avatarSize + padding * 3 + textWidth + shadowExtra;
        float totalHeightF = Math.max(avatarSize, textHeight * 2 + textSpacing + shadowExtra) + padding * 2;
        float totalWidth = (float) Math.ceil(totalWidthF);
        float totalHeight = (float) Math.ceil(totalHeightF);
        float posX = (float) Math.floor(this.x.getCurrentValue());
        float posY = (float) Math.floor(this.y.getCurrentValue());
        float r = this.radius.getCurrentValue();
        int bgA = (int) Math.max(0.0F, Math.min(255.0F, this.backgroundAlpha.getCurrentValue()));
        int bgColor = (bgA << 24);
        RenderUtils.drawRoundedRect(e.getStack(), posX, posY, totalWidth, totalHeight, r, bgColor);
        float avatarX = posX + padding;
        float avatarY = posY + padding;
        ResourceLocation skinLocation = null;
        PlayerInfo playerInfo = mc.getConnection().getPlayerInfo(mc.player.getUUID());
        if (playerInfo != null) {
            skinLocation = playerInfo.getSkinLocation();
        }
        if (skinLocation != null) {
            e.getGuiGraphics().blit(skinLocation, (int) avatarX, (int) avatarY, (int) avatarSize, (int) avatarSize, 8, 8, 8, 8, 64, 64);
            e.getGuiGraphics().blit(skinLocation, (int) avatarX, (int) avatarY, (int) avatarSize, (int) avatarSize, 40, 8, 8, 8, 64, 64);
        } else {
            RenderUtils.drawRoundedRect(e.getStack(), avatarX, avatarY, avatarSize, avatarSize, 2.0F, new Color(100, 100, 100, 150).getRGB());
        }
        float textX = avatarX + avatarSize + padding;
        float textY = avatarY + (Math.max(avatarSize, textHeight * 2 + textSpacing + shadowExtra) - (textHeight * 2 + textSpacing)) / 2.0F;
        textX = (float) Math.ceil(textX);
        textY = (float) Math.ceil(textY);
        font.drawString(e.getStack(), playTimeText, textX, textY, Color.WHITE, true, textScale);
        textY += textHeight + textSpacing;
        font.drawString(e.getStack(), killsText, textX, textY, Color.WHITE, true, textScale);
    }
    private String getPlayTime() {
        if (sessionStartTime == 0) {
            return "00:00:00";
        }

        long elapsedMillis = System.currentTimeMillis() - sessionStartTime;
        long seconds = elapsedMillis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;

        seconds = seconds % 60;
        minutes = minutes % 60;

        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}