package com.heypixel.heypixelmod.obsoverlay.modules.impl.combat;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventPacket;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRender2D;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRunTicks;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRender;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.mojang.blaze3d.vertex.PoseStack;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingDeque;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.network.protocol.game.ClientboundHurtAnimationPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.ui.notification.Notification;
import com.heypixel.heypixelmod.obsoverlay.ui.notification.NotificationLevel;
import com.heypixel.heypixelmod.obsoverlay.utils.ChatUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.RenderUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.Fonts;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.ModeValue;

// 新增导入 - 3D渲染相关
import com.mojang.blaze3d.systems.RenderSystem;



@ModuleInfo(
        name = "BackTrack",
        description = "SSS",
        category = Category.COMBAT
)
public class BackTrack extends Module {

    public final BooleanValue log =
            ValueBuilder.create(this, "Logging").setDefaultBooleanValue(false).build().getBooleanValue();
    public final BooleanValue OnGroundStop =
            ValueBuilder.create(this, "OnGroundStop").setDefaultBooleanValue(false).build().getBooleanValue();
    public final FloatValue maxpacket =
            ValueBuilder.create(this, "Max Packet number").setDefaultFloatValue(1000.0F).setFloatStep(5.0F)
                    .setMinFloatValue(1.0F).setMaxFloatValue(5000.0F).build().getFloatValue();
    final FloatValue range =
            ValueBuilder.create(this, "Range").setDefaultFloatValue(3.0F).setFloatStep(0.5F)
                    .setMinFloatValue(1.0F).setMaxFloatValue(6.0F).build().getFloatValue();
    final FloatValue delay =
            ValueBuilder.create(this, "Delay(Tick)").setDefaultFloatValue(20.0F).setFloatStep(1.0F)
                    .setMinFloatValue(1.0F).setMaxFloatValue(200.0F).build().getFloatValue();
    public final BooleanValue btrender =
            ValueBuilder.create(this, "Render").setDefaultBooleanValue(false).build().getBooleanValue();
    public final BooleanValue onlyAura =
            ValueBuilder.create(this, "Only Aura").setDefaultBooleanValue(false).build().getBooleanValue();
    public final BooleanValue releaseOnHurt =
            ValueBuilder.create(this, "Release On Hurt").setDefaultBooleanValue(false).build().getBooleanValue();
    public final ModeValue btrendermode;

    // 新增：敌人位置渲染配置
    public final BooleanValue renderEnemyPos =
            ValueBuilder.create(this, "Render Enemy Position").setDefaultBooleanValue(true).build().getBooleanValue();
    public final FloatValue espWidth = ValueBuilder.create(this, "ESP Width")
            .setDefaultFloatValue(2.0F)
            .setFloatStep(0.5F)
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(5.0F)
            .setVisibility(() -> this.renderEnemyPos.getCurrentValue())
            .build()
            .getFloatValue();

    public boolean btwork;
    private final LinkedBlockingDeque<Packet<?>> packetQueue;
    private final List<Integer> knockbackIndices;
    private boolean intercepting;
    private int interceptedCount;
    private int delayTicks;
    private boolean waitingForGround;

    // 新增：独立的敌人位置追踪系统（用于渲染）
    private Vec3 enemyBacktrackPosition = null;
    private long lastPositionUpdate = 0;
    private Player currentEnemy = null;

    // 新增：回溯起始位置记录（用于固定ESP框）
    private Vec3 backtrackStartPosition = null;
    private long backtrackStartTime = 0;
    private boolean shouldRenderBacktrackPos = false;

    private static final float PROGRESS_BAR_WIDTH = 84.0F;
    private static final float PROGRESS_BAR_HEIGHT = 4.0F;
    private static final float PROGRESS_BAR_Y_OFFSET = -18.0F;
    private static final int BACKGROUND_COLOR = 0x66000000;
    private static final int PROGRESS_COLOR = 0xFFE6E6E5;
    private static final int OVERFLOW_COLOR = 0xFFFFA3C3;
    private static final float CORNER_RADIUS = 2.0F;

    public BackTrack() {
        ValueBuilder builder = ValueBuilder.create(this, "Render Mode");
        BooleanValue renderToggle = this.btrender;
        Objects.requireNonNull(renderToggle);
        this.btrendermode = builder
                .setVisibility(renderToggle::getCurrentValue)
                .setDefaultModeIndex(0)
                .setModes("Normal", "LingDong")
                .build()
                .getModeValue();

        this.btwork = false;
        this.packetQueue = new LinkedBlockingDeque<>();
        this.knockbackIndices = new ArrayList<>();
        this.intercepting = false;
        this.interceptedCount = 0;
        this.delayTicks = 0;
        this.waitingForGround = false;
        this.enemyBacktrackPosition = null;
        this.lastPositionUpdate = 0;
        this.currentEnemy = null;
        this.backtrackStartPosition = null;
        this.backtrackStartTime = 0;
        this.shouldRenderBacktrackPos = false;
    }

    private static boolean isBacktracking = false;



    @Override
    public void onDisable() {
        reset();
    }

    public int getPacketCount() {
        return packetQueue.size();
    }

    public void reset() {
        releaseQueuedPackets();
        this.intercepting = false;
        this.interceptedCount = 0;
        this.delayTicks = 0;
        this.waitingForGround = false;
        this.btwork = false;
        this.knockbackIndices.clear();
        // 重置渲染相关变量
        this.enemyBacktrackPosition = null;
        this.currentEnemy = null;
        this.backtrackStartPosition = null;
        this.shouldRenderBacktrackPos = false;
        this.lastPositionUpdate = 0;
        this.backtrackStartTime = 0;
    }

    private void releaseQueuedPackets() {
        final Minecraft mc = Minecraft.getInstance();
        final int count = packetQueue.size();

        while (!packetQueue.isEmpty()) {
            try {
                Packet<?> packet = packetQueue.poll();
                if (packet != null && mc.getConnection() != null) {
                    ((Packet<ClientGamePacketListener>) packet).handle(mc.getConnection());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (count > 0) {
            log("释放了 " + count + " 个拦截的包");
        }

        this.interceptedCount = 0;
        this.knockbackIndices.clear();
    }

    /**
     * 检查并处理玩家或敌人死亡情况（新增方法）
     */
    private void checkDeathAndReset() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // 1. 检查自己是否死亡
        if (!mc.player.isAlive()) {
            if (this.intercepting || this.waitingForGround) {
                log("玩家死亡，强制重置回溯");
                releaseQueuedPackets();
                reset();
            }
            return;
        }

        // 2. 检查记录的敌人是否死亡
        if (this.currentEnemy != null && !this.currentEnemy.isAlive()) {
            log("追踪的敌人死亡，停止回溯");
            releaseQueuedPackets();
            resetAfterRelease();
            return;
        }

        // 3. 检查是否在拦截状态下失去了所有敌人
        if (this.intercepting && !hasNearbyPlayers(this.range.getCurrentValue(), true)) {
            log("失去所有敌人目标，停止回溯");
            releaseQueuedPackets();
            resetAfterRelease();
        }
    }

    private boolean hasNearbyPlayers(float range) {
        return hasNearbyPlayers(range, false);
    }

    private boolean hasNearbyPlayers(float range, boolean skipUpdateRecord) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.player != null) {
            // 先清理已死亡的敌人记录
            if (this.currentEnemy != null && !this.currentEnemy.isAlive()) {
                this.currentEnemy = null;
                this.enemyBacktrackPosition = null;
                this.backtrackStartPosition = null;
                this.shouldRenderBacktrackPos = false;
                log("检测到记录敌人已死亡");
            }

            for (Player player : mc.level.players()) {
                if (player != mc.player && player.isAlive() && mc.player.distanceTo(player) <= range) {
                    // 记录敌人位置（仅用于渲染，不影响拦截）
                    if (!skipUpdateRecord) {
                        this.currentEnemy = player;

                        // 关键修改：只在开始拦截时记录一次回溯位置
                        if (this.intercepting && this.backtrackStartPosition == null) {
                            this.backtrackStartPosition = player.position();
                            this.backtrackStartTime = System.currentTimeMillis();
                            this.shouldRenderBacktrackPos = true;
                            log("记录回溯起始位置: " + this.backtrackStartPosition);
                        }

                        // 实时更新敌人当前位置（用于其他逻辑）
                        this.enemyBacktrackPosition = player.position();
                        this.lastPositionUpdate = System.currentTimeMillis();
                    }
                    return true;
                }
            }
        }
        // 没有找到敌人时清理渲染数据
        if (!skipUpdateRecord) {
            this.currentEnemy = null;
            this.enemyBacktrackPosition = null;
            this.shouldRenderBacktrackPos = false;
            this.backtrackStartPosition = null;
        }
        return false;
    }

    private void log(String message) {
        if (this.log.getCurrentValue()) {
            ChatUtils.addChatMessage("[Backtrack] " + message);
        }
    }

    @EventTarget
    public void onTick(EventRunTicks event) {
        final Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // === 新增：优先处理死亡检测 ===
        checkDeathAndReset();

        // 玩家死亡后不执行后续逻辑
        if (!mc.player.isAlive()) {
            this.btwork = false;
            return;
        }

        this.btwork = this.intercepting || this.waitingForGround;

        // 更新敌人实时位置（不影响ESP渲染）
        if (this.currentEnemy != null && this.currentEnemy.isAlive()) {
            this.enemyBacktrackPosition = this.currentEnemy.position();
            this.lastPositionUpdate = System.currentTimeMillis();

            // 关键：只在拦截开始时记录一次位置
            if (this.intercepting && this.backtrackStartPosition == null) {
                this.backtrackStartPosition = this.currentEnemy.position();
                this.backtrackStartTime = System.currentTimeMillis();
                this.shouldRenderBacktrackPos = true;
                log("开始回溯，记录位置: " + this.backtrackStartPosition);
            }
        } else {
            // 敌人死亡或不存在时清理
            this.currentEnemy = null;
            this.enemyBacktrackPosition = null;
            this.backtrackStartPosition = null;
            this.shouldRenderBacktrackPos = false;
        }

        // 清理过期的敌人位置数据
        if (this.enemyBacktrackPosition != null &&
                System.currentTimeMillis() - this.lastPositionUpdate > 1000) {
            this.enemyBacktrackPosition = null;
            this.currentEnemy = null;
            this.backtrackStartPosition = null;
            this.shouldRenderBacktrackPos = false;
        }

        // 原有的拦截逻辑完全不变
        if (this.releaseOnHurt.getCurrentValue()) {
            Player player = mc.player;
            if (player != null && player.hurtTime > 0 && (this.intercepting || this.waitingForGround)) {
                log("受伤触发：立即释放包并停止拦截");
                releaseQueuedPackets();
                resetAfterRelease();
                return;
            }
        }

        if (this.delayTicks > 0) {
            this.delayTicks--;
            return;
        }

        boolean auraAiming = false;
        try {
            Aura aura = (Aura) Naven.getInstance().getModuleManager().getModule(Aura.class);
            auraAiming = aura != null && aura.isEnabled() && (Aura.aimingTarget != null || Aura.target != null);
        } catch (Throwable ignored) {}

        if (this.onlyAura.getCurrentValue() && !auraAiming) {
            if (this.intercepting || this.waitingForGround) {
                log("OnlyAura 启用但 Aura 未瞄准，停止回溯");
                releaseQueuedPackets();
                reset();
            }
            return;
        }

        if (!this.intercepting && hasNearbyPlayers(this.range.getCurrentValue())) {
            this.intercepting = true;
            this.waitingForGround = false;
            this.interceptedCount = 0;
            this.packetQueue.clear();
            this.knockbackIndices.clear();
            log("检测到附近玩家，开始拦截包");
        }

        if (this.intercepting && this.interceptedCount >= (int) this.maxpacket.getCurrentValue()) {
            if (this.OnGroundStop.getCurrentValue()) {
                this.waitingForGround = true;
                log("达到最大包数量，等待玩家落地");
            } else {
                log("达到最大包数量，立即释放包");
                releaseQueuedPackets();
                resetAfterRelease();
            }
        }

        if (this.waitingForGround && mc.player.onGround()) {
            log("玩家已落地，释放拦截的包");
            releaseQueuedPackets();
            resetAfterRelease();
        }
    }

    @EventTarget
    public void onRender2D(EventRender2D event) {
        if (this.isEnabled()) {
            render(event.getGuiGraphics());
        }
    }

    // 新增：3D渲染事件 - 仅用于渲染敌人回溯起始位置
    @EventTarget
    public void onRender3D(EventRender event) {
        if (!this.isEnabled() || !this.renderEnemyPos.getCurrentValue()) return;
        if (!(this.intercepting || this.waitingForGround)) return;
        if (!this.shouldRenderBacktrackPos || this.backtrackStartPosition == null) return;

        // 新增死亡检查
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !mc.player.isAlive()) return;
        if (this.currentEnemy != null && !this.currentEnemy.isAlive()) return;

        renderEnemyPosition(event, this.backtrackStartPosition);
    }

    // 新增：监听血量包检测死亡
    @EventTarget
    public void onHealthPacket(EventPacket event) {
        if (event.getType() != EventType.RECEIVE) return;

        Packet<?> packet = event.getPacket();

        // 检测玩家死亡（收到零血量包）
        if (packet instanceof ClientboundSetHealthPacket healthPacket) {
            float health = healthPacket.getHealth();
            if (health <= 0.0F && (this.intercepting || this.waitingForGround)) {
                log("收到死亡血量包，立即重置");
                releaseQueuedPackets();
                reset();
                event.setCancelled(false); // 不取消事件，让死亡正常处理
            }
        }
    }

    private void resetAfterRelease() {
        this.intercepting = false;
        this.waitingForGround = false;
        this.delayTicks = (int) this.delay.getCurrentValue();
        this.backtrackStartPosition = null;
        this.shouldRenderBacktrackPos = false;
        log("进入冷却延迟: " + this.delayTicks + " ticks");
    }

    @EventTarget
    public void onPacket(EventPacket event) {
        final Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null || !this.intercepting) return;
        if (event.getType() != EventType.RECEIVE) return;

        Packet<?> packet = event.getPacket();

        // === 修复：立即放行音效包，不拦截 ===
        if (packet instanceof ClientboundSoundPacket) {
            // 不取消事件，让音效正常播放
            return;
        }

        // === 修复：立即放行血量更新包 ===
        if (packet instanceof ClientboundSetHealthPacket) {
            // 不取消事件，让血量正常更新
            return;
        }

        // === 修复：立即放行受伤动画包 ===
        if (packet instanceof ClientboundHurtAnimationPacket) {
            // 不取消事件，让受伤动画正常播放
            return;
        }

        // === 修复：立即放行药水效果包 ===
        if (packet instanceof ClientboundUpdateMobEffectPacket) {
            // 不取消事件，让药水效果正常更新
            return;
        }

        // 服务器位置修正包 -> 停止并释放所有包
        if (packet instanceof ClientboundPlayerPositionPacket) {
            event.setCancelled(true);          // ✅ 先取消
            this.packetQueue.add(packet);      // ✅ 塞进队列
            this.intercepting = false;
            releaseQueuedPackets();            // ✅ 保证 S08 会被重新 handle
            resetAfterRelease();
            return;
        }

        // 击退包处理
        if (packet instanceof ClientboundSetEntityMotionPacket motionPacket) {
            if (motionPacket.getId() == mc.player.getId()) {
                event.setCancelled(true);
                this.packetQueue.add(packet);
                this.interceptedCount++;
                this.knockbackIndices.add(this.packetQueue.size() - 1);
                log("拦截击退包 #" + this.interceptedCount);
                return;
            }
        }

        // 其他关键包继续拦截
        event.setCancelled(true);
        this.packetQueue.add(packet);
        this.interceptedCount++;
        log("拦截普通包 #" + this.interceptedCount);
    }

    public void render(GuiGraphics guiGraphics) {
        if (!(this.intercepting || this.waitingForGround)) return;
        if (!this.btrendermode.isCurrentMode("Normal")) return;
        if (!this.btrender.getCurrentValue()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || !mc.player.isAlive()) return;

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        float x = (screenWidth - PROGRESS_BAR_WIDTH) / 2.0F;
        float y = screenHeight / 2.0F + PROGRESS_BAR_Y_OFFSET;

        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();

        float maxPacketValue = Math.max(1.0F, this.maxpacket.getCurrentValue());
        float progress = Math.min(1.0F, this.interceptedCount / maxPacketValue);
        float progressWidth = PROGRESS_BAR_WIDTH * progress;

        RenderUtils.drawRoundedRect(poseStack, x, y, PROGRESS_BAR_WIDTH, PROGRESS_BAR_HEIGHT, CORNER_RADIUS, BACKGROUND_COLOR);

        if (progressWidth > 0.0F) {
            RenderUtils.drawRoundedRect(poseStack, x, y, progressWidth, PROGRESS_BAR_HEIGHT, CORNER_RADIUS, PROGRESS_COLOR);
        }

        if (this.OnGroundStop.getCurrentValue() && this.interceptedCount > this.maxpacket.getCurrentValue()) {
            float overflowProgress = (this.interceptedCount - this.maxpacket.getCurrentValue()) / maxPacketValue;
            float overflowWidth = Math.min(PROGRESS_BAR_WIDTH * overflowProgress, PROGRESS_BAR_WIDTH);
            RenderUtils.drawRoundedRect(
                    poseStack, x + PROGRESS_BAR_WIDTH - overflowWidth, y,
                    overflowWidth, PROGRESS_BAR_HEIGHT, CORNER_RADIUS, OVERFLOW_COLOR
            );
        }

        String trackingText = "Tracking";
        float textScale = 0.35F;
        float textWidth = Fonts.harmony.getWidth(trackingText, textScale);
        float textX = (screenWidth - textWidth) / 2.0F;
        float textY = y - 10.0F;
        Fonts.harmony.render(poseStack, trackingText, textX, textY, Color.WHITE, true, textScale);

        poseStack.popPose();
    }

    // 修改：渲染敌人回溯起始位置（固定不动）
    private void renderEnemyPosition(EventRender event, Vec3 renderPos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null || mc.gameMode == null || mc.level == null) {
            return;
        }
        if (mc.screen != null) return;

        Vec3 cameraPos = mc.gameRenderer.getMainCamera().getPosition();
        PoseStack poseStack = event.getPMatrixStack();

        // 计算渲染位置（相对于相机）
        double renderX = renderPos.x() - cameraPos.x();
        double renderY = renderPos.y() - cameraPos.y();
        double renderZ = renderPos.z() - cameraPos.z();
        
        // 创建敌人边界框
        AABB enemyBox = new AABB(renderX - 0.3, renderY, renderZ - 0.3, renderX + 0.3, renderY + 1.8, renderZ + 0.3);
        AABB expandedBox = enemyBox.inflate(0.1, 0.1, 0.1);

        // 保存当前渲染状态
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        // 使用半透明蓝色渲染填充盒子
        RenderSystem.setShaderColor(0.0F, 0.5F, 1.0F, 0.3F);
        RenderUtils.drawSolidBox(expandedBox, poseStack);
        
        // 使用不透明蓝色渲染盒子轮廓
        RenderSystem.setShaderColor(0.0F, 0.7F, 1.0F, 1.0F);
        RenderSystem.lineWidth(this.espWidth.getCurrentValue());
        RenderUtils.drawOutlinedBox(expandedBox, poseStack);
        RenderSystem.lineWidth(1.0F);

        // 恢复渲染状态
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }
}
