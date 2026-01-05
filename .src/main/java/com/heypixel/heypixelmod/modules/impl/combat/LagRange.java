package com.heypixel.heypixelmod.modules.impl.combat;

import com.heypixel.heypixelmod.events.api.EventTarget;
import com.heypixel.heypixelmod.events.api.types.EventType;
import com.heypixel.heypixelmod.events.impl.EventRender;
import com.heypixel.heypixelmod.events.impl.EventRunTicks;
import com.heypixel.heypixelmod.modules.Category;
import com.heypixel.heypixelmod.modules.Module;
import com.heypixel.heypixelmod.modules.ModuleInfo;
import com.heypixel.heypixelmod.utils.RenderUtils;
import com.heypixel.heypixelmod.values.ValueBuilder;
import com.heypixel.heypixelmod.values.impl.BooleanValue;
import com.heypixel.heypixelmod.values.impl.FloatValue;
import com.heypixel.heypixelmod.values.impl.ModeValue;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.Entity;

import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;

//from OpenMyau


@ModuleInfo(
        name = "LagRange",
        description = "Lag-based extended reach visualization",
        category = Category.COMBAT
)
public class LagRange extends Module {
    private int tickIndex = -1;
    private long delayCounter = 0L;
    private boolean hasTarget = false;
    private Vec3 lastPosition = null;
    private Vec3 currentPosition = null;

    public FloatValue delayMs = ValueBuilder.create(this, "Delay (ms)")
            .setDefaultFloatValue(150F)
            .setMinFloatValue(0F)
            .setMaxFloatValue(1000F)
            .setFloatStep(10F)
            .build()
            .getFloatValue();

    public FloatValue range = ValueBuilder.create(this, "Range")
            .setDefaultFloatValue(10.0F)
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(100.0F)
            .setFloatStep(0.5F)
            .build()
            .getFloatValue();

    public BooleanValue weaponsOnly = ValueBuilder.create(this, "Weapons Only")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();

    public BooleanValue allowTools = ValueBuilder.create(this, "Allow Tools")
            .setDefaultBooleanValue(false)
            .setVisibility(() -> this.weaponsOnly.getCurrentValue())
            .build()
            .getBooleanValue();

    public BooleanValue botCheck = ValueBuilder.create(this, "Bot Check")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();

    public BooleanValue teams = ValueBuilder.create(this, "Teams")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();

    public ModeValue showPosition = ValueBuilder.create(this, "Show Position")
            .setDefaultModeIndex(1)
            .setModes("NONE", "DEFAULT", "HUD")
            .build()
            .getModeValue();

    public BooleanValue renderSelfBox = ValueBuilder.create(this, "Render Self Box")
            .setDefaultBooleanValue(false)
            .setVisibility(() -> !this.showPosition.isCurrentMode("NONE"))
            .build()
            .getBooleanValue();

    private boolean isValidTarget(Player player) {
        if (player == null || mc.player == null) return false;
        if (player == mc.player) return false;
        if (!player.isAlive()) return false;
        return true;
    }

    @EventTarget(1)
    public void onTick(EventRunTicks event) {
        if (event.getType() != EventType.PRE || mc.player == null || mc.level == null) return;

        this.hasTarget = false;

        List<Player> candidates = mc.level.players().stream()
                .filter(this::isValidTarget)
                .collect(Collectors.toList());

        if (candidates.isEmpty()) {
            this.tickIndex = -1;
        } else {
            Vec3 playerEye = mc.player.getEyePosition();
            for (Player p : candidates) {
                double distance = p.getBoundingBox().distanceToSqr(playerEye);
                if (distance <= (double) (this.range.getCurrentValue() * this.range.getCurrentValue())) {
                    if (this.tickIndex < 0) {
                        this.tickIndex = 0;
                        this.delayCounter += (long) this.delayMs.getCurrentValue();
                        while (this.delayCounter > 0L) {
                            this.tickIndex++;
                            this.delayCounter -= 50L;
                        }
                    }
                    this.hasTarget = true;
                    break;
                }
            }
        }

        Vec3 saved = mc.player.position();
        if (this.currentPosition == null) {
            this.lastPosition = saved;
        } else {
            this.lastPosition = this.currentPosition;
        }
        this.currentPosition = saved;
    }

    @EventTarget
    public void onRender3D(EventRender event) {
        if (!this.isEnabled() || mc.player == null || mc.gameRenderer == null) return;
        if (this.showPosition.isCurrentMode("NONE") || !this.hasTarget || this.lastPosition == null || this.currentPosition == null) return;
        if (!this.renderSelfBox.getCurrentValue()) return;

        Vec3 cameraPos = mc.gameRenderer.getMainCamera().getPosition();
        PoseStack poseStack = event.getPMatrixStack();

        double x = this.currentPosition.x;
        double y = this.currentPosition.y;
        double z = this.currentPosition.z;

        double width = mc.player.getBbWidth();
        double height = mc.player.getBbHeight();

        AABB box = new AABB(
                x - width / 2.0 - cameraPos.x,
                y - cameraPos.y,
                z - width / 2.0 - cameraPos.z,
                x + width / 2.0 - cameraPos.x,
                y + height - cameraPos.y,
                z + width / 2.0 - cameraPos.z
        );
        RenderUtils.drawSolidBox(box, poseStack);
    }

    @Override
    public void onDisable() {
        this.tickIndex = -1;
        this.delayCounter = 0L;
        this.hasTarget = false;
        this.lastPosition = null;
        this.currentPosition = null;
    }
}
