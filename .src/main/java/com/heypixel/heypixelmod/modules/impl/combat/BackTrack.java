package com.heypixel.heypixelmod.modules.impl.combat;

import com.heypixel.heypixelmod.Naven;
import com.heypixel.heypixelmod.events.api.EventTarget;
import com.heypixel.heypixelmod.events.api.types.EventType;
import com.heypixel.heypixelmod.events.impl.EventPacket;
import com.heypixel.heypixelmod.events.impl.EventRender;
import com.heypixel.heypixelmod.events.impl.EventRender2D;
import com.heypixel.heypixelmod.events.impl.EventRunTicks;
import com.heypixel.heypixelmod.modules.Category;
import com.heypixel.heypixelmod.modules.Module;
import com.heypixel.heypixelmod.modules.ModuleInfo;
import com.heypixel.heypixelmod.utils.ChatUtils;
import com.heypixel.heypixelmod.utils.RenderUtils;
import com.heypixel.heypixelmod.utils.SmoothAnimationTimer;
import com.heypixel.heypixelmod.utils.renderer.Fonts;
import com.heypixel.heypixelmod.values.ValueBuilder;
import com.heypixel.heypixelmod.values.impl.BooleanValue;
import com.heypixel.heypixelmod.values.impl.FloatValue;
import com.heypixel.heypixelmod.values.impl.ModeValue;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;

//Alpha BackTrack已被害死 因此替换为之前提交的

@ModuleInfo(
        name = "BackTrack",
        description = "Stuck Network,but adversaries",
        category = Category.COMBAT
)
public class BackTrack extends Module {

    private static class TrackedPlayer {
        Vec3 serverPosition;
        Vec3 frozenPosition;
        long lastUpdateTime;

        TrackedPlayer(Vec3 initialPosition) {
            this.serverPosition = initialPosition;
            this.frozenPosition = initialPosition;
            this.lastUpdateTime = System.currentTimeMillis();
        }

        void updateServerPosition(Vec3 newPosition) {
            this.serverPosition = newPosition;
            this.lastUpdateTime = System.currentTimeMillis();
        }
    }

    public BooleanValue log = ValueBuilder.create(this, "Logging")
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();

    public BooleanValue OnGroundStop = ValueBuilder.create(this, "OnGroundRelease")
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();

    public BooleanValue onVelocityRelease = ValueBuilder.create(this, "OnVelocityRelease")
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();

    public BooleanValue randomPacket = ValueBuilder.create(this, "RandomPacket")
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();

    public FloatValue randomMaxPacket = ValueBuilder.create(this, "RandomMaxPacket")
            .setVisibility(this.randomPacket::getCurrentValue)
            .setDefaultFloatValue(60F)
            .setFloatStep(5F)
            .setMinFloatValue(1F)
            .setMaxFloatValue(450F)
            .build()
            .getFloatValue();

    public FloatValue randomMinPacket = ValueBuilder.create(this, "RandomMinPacket")
            .setVisibility(this.randomPacket::getCurrentValue)
            .setDefaultFloatValue(30F)
            .setFloatStep(5F)
            .setMinFloatValue(1F)
            .setMaxFloatValue(450F)
            .build()
            .getFloatValue();

    public FloatValue maxpacket = ValueBuilder.create(this, "Max Packet number")
            .setVisibility(() -> !this.randomPacket.getCurrentValue())
            .setDefaultFloatValue(45F)
            .setFloatStep(5F)
            .setMinFloatValue(1F)
            .setMaxFloatValue(450F)
            .build()
            .getFloatValue();

    FloatValue range = ValueBuilder.create(this, "Range")
            .setDefaultFloatValue(3F)
            .setFloatStep(0.5F)
            .setMinFloatValue(1F)
            .setMaxFloatValue(6F)
            .build()
            .getFloatValue();

    public BooleanValue randomDelay = ValueBuilder.create(this, "RandomDelay")
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();

    public FloatValue randomMaxDelay = ValueBuilder.create(this, "RandomMaxDelay")
            .setVisibility(this.randomDelay::getCurrentValue)
            .setDefaultFloatValue(30F)
            .setFloatStep(1F)
            .setMinFloatValue(1F)
            .setMaxFloatValue(200F)
            .build()
            .getFloatValue();

    public FloatValue randomMinDelay = ValueBuilder.create(this, "RandomMinDelay")
            .setVisibility(this.randomDelay::getCurrentValue)
            .setDefaultFloatValue(10F)
            .setFloatStep(1F)
            .setMinFloatValue(1F)
            .setMaxFloatValue(200F)
            .build()
            .getFloatValue();

    FloatValue delay = ValueBuilder.create(this, "Delay(Tick)")
            .setVisibility(() -> !this.randomDelay.getCurrentValue())
            .setDefaultFloatValue(20F)
            .setFloatStep(1F)
            .setMinFloatValue(1F)
            .setMaxFloatValue(200F)
            .build()
            .getFloatValue();

    public BooleanValue btrender = ValueBuilder.create(this, "Render")
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();

    public FloatValue boxRed = ValueBuilder.create(this, "Box Red")
            .setDefaultFloatValue(0F)
            .setFloatStep(5F)
            .setMinFloatValue(0F)
            .setMaxFloatValue(255F)
            .build()
            .getFloatValue();

    public FloatValue boxGreen = ValueBuilder.create(this, "Box Green")
            .setDefaultFloatValue(150F)
            .setFloatStep(5F)
            .setMinFloatValue(0F)
            .setMaxFloatValue(255F)
            .build()
            .getFloatValue();

    public FloatValue boxBlue = ValueBuilder.create(this, "Box Blue")
            .setDefaultFloatValue(255F)
            .setFloatStep(5F)
            .setMinFloatValue(0F)
            .setMaxFloatValue(255F)
            .build()
            .getFloatValue();

    public ModeValue btrendermode = ValueBuilder.create(this, "Render Mode")
            .setVisibility(this.btrender::getCurrentValue)
            .setDefaultModeIndex(0)
            .setModes("Normal","Naven")
            .build()
            .getModeValue();

    public BooleanValue onlyWhenAuraTarget = ValueBuilder.create(this, "Only When Aura Target")
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();

    public ModeValue packetMode = ValueBuilder.create(this, "Mode")
            .setDefaultModeIndex(0)
            .setModes("CancelPacketSend")
            .build()
            .getModeValue();

    public BooleanValue movePacketOnly = ValueBuilder.create(this, "MovePacketOnly")
            .setVisibility(() -> this.packetMode.isCurrentMode("CancelPacketSend"))
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();

    private final LinkedBlockingDeque<Packet<?>> airKBQueue = new LinkedBlockingDeque<>();
    private final List<Integer> knockbackPositions = new ArrayList<>();
    private boolean isInterceptingAirKB = false;
    private int interceptedPacketCount = 0;
    private int delayTicks = 0;
    private boolean shouldCheckGround = false;

    private final Map<Integer, TrackedPlayer> trackedEnemies = new ConcurrentHashMap<>();

    private static final float PROGRESS_BAR_WIDTH = 200.0f;
    private static final float PROGRESS_BAR_HEIGHT = 10.0f;
    private static final float PROGRESS_BAR_Y_OFFSET = 65.0f;
    private static final int BACKGROUND_COLOR = 0x80000000;
    private static final int PROGRESS_COLOR = 0xFF66CCFF;
    private static final int OVERFLOW_COLOR = 0xFFFF6B6B;
    private static final float CORNER_RADIUS = 5.0f;

    private final SmoothAnimationTimer navenProgress = new SmoothAnimationTimer(0.0F, 0.2F);
    private static final int navenMainColor = new Color(150, 45, 45, 255).getRGB();
    private final Random random = new Random();

    private float getRandomPacketCount() {
        if (randomPacket.getCurrentValue()) {
            float min = randomMinPacket.getCurrentValue();
            float max = randomMaxPacket.getCurrentValue();
            return min + random.nextFloat() * (max - min);
        }
        return maxpacket.getCurrentValue();
    }

    private float getRandomDelay() {
        if (randomDelay.getCurrentValue()) {
            float min = randomMinDelay.getCurrentValue();
            float max = randomMaxDelay.getCurrentValue();
            return min + random.nextFloat() * (max - min);
        }
        return delay.getCurrentValue();
    }

    @Override
    public void onEnable() {
        reset();
    }

    @Override
    public void onDisable() {
        releaseAllPacketQueue();
        reset();
    }

    public void releaseAllPacketQueue() {
        releaseAirKBQueue();
    }

    public int getPacketCount() {
        return airKBQueue.size();
    }

    public void reset() {
        releaseAirKBQueue();
        isInterceptingAirKB = false;
        interceptedPacketCount = 0;
        delayTicks = 0;
        shouldCheckGround = false;
        knockbackPositions.clear();
        trackedEnemies.clear();
    }

    private void releaseAirKBQueue() {
        int packetCount = airKBQueue.size();
        while (!this.airKBQueue.isEmpty()) {
            try {
                Packet<?> packet = this.airKBQueue.poll();
                if (packet != null && mc.getConnection() != null) {
                    ((Packet<ClientGamePacketListener>) packet).handle(mc.getConnection());
                }
            } catch (Exception var3) {
                var3.printStackTrace();
            }
        }
        if (packetCount > 0) {
            log("Release " + packetCount + " Packets");
        }
        interceptedPacketCount = 0;
        knockbackPositions.clear();
    }

    private boolean hasNearbyPlayers(float range) {
        if (mc.level == null || mc.player == null) return false;
        for (Player player : mc.level.players()) {
            if (player == mc.player) continue;
            if (player.isAlive() && mc.player.distanceTo(player) <= range) {
                return true;
            }
        }
        return false;
    }

    private void log(String message) {
        if (this.log.getCurrentValue()) {
            ChatUtils.addChatMessage("[Backtrack] " + message);
        }
    }

    private boolean isKillAuraTargeting() {
        Module killAura = Naven.getInstance().getModuleManager().getModule(Aura.class);
        return killAura != null && killAura.isEnabled() && Aura.target != null;
    }

    @EventTarget
    public void onTick(EventRunTicks event) {
        if (mc.player == null || mc.level == null) return;

        if (isInterceptingAirKB) {
            for (Map.Entry<Integer, TrackedPlayer> entry : trackedEnemies.entrySet()) {
                Entity enemy = mc.level.getEntity(entry.getKey());
                if (enemy != null) {
                    Vec3 frozenPos = entry.getValue().frozenPosition;
                    enemy.setPos(frozenPos.x, frozenPos.y, frozenPos.z);
                }
            }
        }

        if (delayTicks > 0) {
            delayTicks--;
            return;
        }

        boolean shouldIntercept = true;
        if (onlyWhenAuraTarget.getCurrentValue()) {
            shouldIntercept = isKillAuraTargeting();
        }

        if (!isInterceptingAirKB && hasNearbyPlayers(range.getCurrentValue()) && shouldIntercept) {
            isInterceptingAirKB = true;
            shouldCheckGround = false;
            interceptedPacketCount = 0;
            airKBQueue.clear();
            knockbackPositions.clear();
            trackedEnemies.clear();
            log("Checked nearby players, start intercepting packets and freezing enemies.");

            for (Player player : mc.level.players()) {
                if (player != mc.player && player.isAlive() && mc.player.distanceTo(player) <= range.getCurrentValue()) {
                    trackedEnemies.put(player.getId(), new TrackedPlayer(player.position()));
                }
            }
        }

        if (isInterceptingAirKB && interceptedPacketCount >= getRandomPacketCount()) {
            if (OnGroundStop.getCurrentValue()) {
                shouldCheckGround = true;
                log("Max Packet number reached, waiting to land before releasing packets");
            } else {
                log("Release Packet");
                releaseAirKBQueue();
                resetAfterRelease();
            }
        }

        if (shouldCheckGround && mc.player.onGround()) {
            log("Release Packet");
            releaseAirKBQueue();
            resetAfterRelease();
        }

        if (!trackedEnemies.isEmpty()) {
            long currentTime = System.currentTimeMillis();
            trackedEnemies.keySet().removeIf(entityId -> {
                Entity entity = mc.level.getEntity(entityId);
                TrackedPlayer trackedPlayer = trackedEnemies.get(entityId);
                return entity == null || !entity.isAlive() || entity.distanceTo(mc.player) > range.getCurrentValue() * 2.0f
                        || (trackedPlayer != null && (currentTime - trackedPlayer.lastUpdateTime) > 500);
            });
        }
    }

    @EventTarget
    public void onRender2D(EventRender2D event) {
        if (this.isEnabled()) {
            this.render(event.getGuiGraphics());
        }
    }

@EventTarget
public void onRender3D(EventRender event) {
    if (mc.player == null || mc.gameRenderer == null || trackedEnemies.isEmpty() || !isInterceptingAirKB) {
        return;
    }

    Vec3 cameraPos = mc.gameRenderer.getMainCamera().getPosition();
    PoseStack poseStack = event.getPMatrixStack();
    float width = mc.player.getBbWidth();
    float height = mc.player.getBbHeight();
    AABB playerBoxAtOrigin = new AABB(-width / 2.0, 0, -width / 2.0, width / 2.0, height, width / 2.0);

    RenderSystem.enableBlend();
    RenderSystem.defaultBlendFunc();
    RenderSystem.disableCull();
    RenderSystem.disableDepthTest();
    RenderSystem.setShader(GameRenderer::getPositionShader);

    float red = boxRed.getCurrentValue() / 255.0f;
    float green = boxGreen.getCurrentValue() / 255.0f;
    float blue = boxBlue.getCurrentValue() / 255.0f;
    RenderSystem.setShaderColor(red, green, blue, 0.3f);

    for (TrackedPlayer trackedPlayer : trackedEnemies.values()) {
        poseStack.pushPose();
        Vec3 pos = isInterceptingAirKB ? trackedPlayer.frozenPosition : trackedPlayer.serverPosition;
        double renderX = pos.x() - cameraPos.x();
        double renderY = pos.y() - cameraPos.y();
        double renderZ = pos.z() - cameraPos.z();
        poseStack.translate(renderX, renderY, renderZ);
        RenderUtils.drawSolidBox(playerBoxAtOrigin, poseStack);
        poseStack.popPose();
    }

    RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    RenderSystem.enableDepthTest();
    RenderSystem.enableCull();
    RenderSystem.disableBlend();
}
    private void resetAfterRelease() {
        isInterceptingAirKB = false;
        shouldCheckGround = false;
        delayTicks = (int) getRandomDelay();
        log("Delay: " + delayTicks + " ticks");
        trackedEnemies.clear();
    }

    @EventTarget
    public void onPacket(EventPacket event) {
        if (mc.player == null || mc.level == null || event.getType() != EventType.RECEIVE) {
            return;
        }

        Packet<?> packet = event.getPacket();

        if (onVelocityRelease.getCurrentValue() && isInterceptingAirKB && packet instanceof ClientboundSetEntityMotionPacket) {
            ClientboundSetEntityMotionPacket motionPacket = (ClientboundSetEntityMotionPacket) packet;
            if (motionPacket.getId() == mc.player.getId()) {
                log("Velocity packet received, releasing queue due to OnVelocityRelease.");
                releaseAirKBQueue();
                resetAfterRelease();
                return;
            }
        }

        if (!isInterceptingAirKB) {
            return;
        }

        if (packet instanceof ClientboundTeleportEntityPacket teleportPacket) {
            if (trackedEnemies.containsKey(teleportPacket.getId())) {
        event.setCancelled(true);
                Vec3 newPos = new Vec3(teleportPacket.getX(), teleportPacket.getY(), teleportPacket.getZ());
                trackedEnemies.get(teleportPacket.getId()).updateServerPosition(newPos);
        airKBQueue.add(packet);
        interceptedPacketCount++;
                return;
            }
        } else if (packet instanceof ClientboundMoveEntityPacket movePacket) {
            Entity entity = movePacket.getEntity(mc.level);
            if (entity != null && trackedEnemies.containsKey(entity.getId())) {
                event.setCancelled(true);
                TrackedPlayer trackedPlayer = trackedEnemies.get(entity.getId());
                Vec3 lastKnownPos = trackedPlayer.serverPosition;
                double newX = lastKnownPos.x + (double)movePacket.getXa() / 4096.0;
                double newY = lastKnownPos.y + (double)movePacket.getYa() / 4096.0;
                double newZ = lastKnownPos.z + (double)movePacket.getZa() / 4096.0;
                trackedPlayer.updateServerPosition(new Vec3(newX, newY, newZ));
                airKBQueue.add(packet);
                interceptedPacketCount++;
                return;
            }
        }

        if (packet instanceof ClientboundPlayerPositionPacket) {
            return;
        }
        if (packet instanceof ClientboundSetEntityMotionPacket motionPacket) {
            if (motionPacket.getId() == mc.player.getId()) {
                event.setCancelled(true);
                airKBQueue.add(packet);
                interceptedPacketCount++;
                knockbackPositions.add(airKBQueue.size() - 1);
            }
        } else {
            if (movePacketOnly.getCurrentValue()) {
                return;
            }
            event.setCancelled(true);
            airKBQueue.add(packet);
            interceptedPacketCount++;
        }
    }

public void render(GuiGraphics guiGraphics) {
    if (!btrender.getCurrentValue()) return;
    Minecraft mc = Minecraft.getInstance();
    if (mc.player == null || mc.level == null) return;
    if (!(isInterceptingAirKB || shouldCheckGround || getPacketCount() > 0)) return;

    if (btrendermode.isCurrentMode("Normal")) {
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        float x = (screenWidth - PROGRESS_BAR_WIDTH) / 2.0f;
        float y = screenHeight / 2.0f + PROGRESS_BAR_Y_OFFSET;
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        float maxPacketValue = Math.max(1.0f, getRandomPacketCount());
        float progress = Math.min(1.0f, (float) getPacketCount() / maxPacketValue);
        float progressWidth = PROGRESS_BAR_WIDTH * progress;
        RenderUtils.drawRoundedRect(poseStack, x, y, PROGRESS_BAR_WIDTH, PROGRESS_BAR_HEIGHT, CORNER_RADIUS, BACKGROUND_COLOR);
        if (progressWidth > 0) {
            RenderUtils.drawRoundedRect(poseStack, x, y, progressWidth, PROGRESS_BAR_HEIGHT, CORNER_RADIUS, PROGRESS_COLOR);
        }
        if (OnGroundStop.getCurrentValue() && getPacketCount() > getRandomPacketCount()) {
            float overflowProgress = Math.min(1.0f, (getPacketCount() - getRandomPacketCount()) / maxPacketValue);
            float overflowWidth = PROGRESS_BAR_WIDTH * overflowProgress;
            RenderUtils.drawRoundedRect(poseStack, x + PROGRESS_BAR_WIDTH - overflowWidth, y, overflowWidth, PROGRESS_BAR_HEIGHT, CORNER_RADIUS, OVERFLOW_COLOR);
        }
        String trackingText = "Tracking...";
        float textScale = 0.35f;
        float textWidth = Fonts.harmony.getWidth(trackingText, textScale);
        float textY = y + (PROGRESS_BAR_HEIGHT - (float) Fonts.harmony.getHeight(false, textScale)) / 2.0f;
        float textX = x + (PROGRESS_BAR_WIDTH - textWidth) / 2.0f;
        Fonts.harmony.drawString(poseStack, trackingText, (double) textX, (double) textY, Color.WHITE, false, textScale);
        poseStack.popPose();
    } else if (btrendermode.isCurrentMode("Naven")) {
        this.navenProgress.target = Mth.clamp((float) this.getPacketCount() / this.getRandomPacketCount() * 100.0F, 0.0F, 100.0F);
        this.navenProgress.update(true);
        int barX = mc.getWindow().getGuiScaledWidth() / 2 - 50;
        int barY = mc.getWindow().getGuiScaledHeight() / 2 + 15;
        float barWidth = 100.0F;
        String trackingText = "Tracking...";
        float textScale = 0.35f;
        float textWidth = Fonts.harmony.getWidth(trackingText, textScale);
        float textHeight = (float) Fonts.harmony.getHeight(false, textScale);
        float textX = barX + (barWidth - textWidth) / 2.0f;
        float textY = barY - textHeight - 2;
        Fonts.harmony.drawString(guiGraphics.pose(), trackingText, (double) textX, (double) textY, Color.WHITE, false, textScale);
        RenderUtils.drawRoundedRect(guiGraphics.pose(), (float) barX, (float) barY, barWidth, 5.0F, 2.0F, Integer.MIN_VALUE);
        RenderUtils.drawRoundedRect(guiGraphics.pose(), (float) barX, (float) barY, this.navenProgress.value, 5.0F, 2.0F, navenMainColor);
    }
  }
}
