package com.heypixel.heypixelmod.modules.impl.combat;

import com.heypixel.heypixelmod.events.api.EventTarget;
import com.heypixel.heypixelmod.events.impl.EventPacket;
import com.heypixel.heypixelmod.events.impl.EventUpdate;
import com.heypixel.heypixelmod.events.impl.EventMoveInput;
import com.heypixel.heypixelmod.modules.Category;
import com.heypixel.heypixelmod.modules.Module;
import com.heypixel.heypixelmod.modules.ModuleInfo;
import com.heypixel.heypixelmod.values.ValueBuilder;
import com.heypixel.heypixelmod.values.impl.BooleanValue;
import com.heypixel.heypixelmod.values.impl.FloatValue;
import com.heypixel.heypixelmod.values.impl.ModeValue;
import com.heypixel.heypixelmod.utils.ChatUtils;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;

//原alpha版本的velocity无法public 因此替换为open-xd-release的


@ModuleInfo(
        name = "Velocity",
        description = "Reduces Knock Back.",
        category = Category.COMBAT
)

public class Velocity extends Module {
    private final ModeValue mode = ValueBuilder.create(this, "Mode")
            .setDefaultModeIndex(0)
            .setModes("NoXZ", "JumpReset")
            .build()
            .getModeValue();

    private final ModeValue noXZMode = ValueBuilder.create(this, "NoXZ Mode")
            .setDefaultModeIndex(0)
            .setModes("OneTime", "PerTick")
            .setVisibility(() -> mode.isCurrentMode("NoXZ"))
            .build()
            .getModeValue();

    private final BooleanValue randomAttackCount = ValueBuilder.create(this, "Random AttackCount")
            .setDefaultBooleanValue(false)
            .setVisibility(() -> mode.isCurrentMode("NoXZ"))
            .build()
            .getBooleanValue();

    private final FloatValue maxAttacks = ValueBuilder.create(this, "Max AttackCount")
            .setDefaultFloatValue(5.0F)
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(10.0F)
            .setFloatStep(1.0F)
            .setVisibility(() -> mode.isCurrentMode("NoXZ") && randomAttackCount.getCurrentValue())
            .build()
            .getFloatValue();

    private final FloatValue minAttacks = ValueBuilder.create(this, "Min AttackCount")
            .setDefaultFloatValue(3.0F)
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(10.0F)
            .setFloatStep(1.0F)
            .setVisibility(() -> mode.isCurrentMode("NoXZ") && randomAttackCount.getCurrentValue())
            .build()
            .getFloatValue();

    private final FloatValue attacks = ValueBuilder.create(this, "Attack Count")
            .setDefaultFloatValue(3.0F)
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(5.0F)
            .setFloatStep(1.0F)
            .setVisibility(() -> mode.isCurrentMode("NoXZ") && !randomAttackCount.getCurrentValue())
              .build()
              .getFloatValue();

    private final BooleanValue Logging = ValueBuilder.create(this, "Logging")
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();

    private Entity targetEntity;
    private boolean velocityInput = false;
    private boolean attacked = false;

    private double currentKnockbackSpeed = 0.0;
    private int attackQueue = 0;
    private boolean receiveDamage = false;

    @Override
    public void onDisable() {
        this.velocityInput = false;
        this.attacked = false;

        this.targetEntity = null;
        this.currentKnockbackSpeed = 0.0;
        this.attackQueue = 0;
        this.receiveDamage = false;
    }

    @EventTarget
    public void onPacket(EventPacket event) {
        if (mc.level == null || mc.player == null) return;

        Packet<?> packet = event.getPacket();

        if (packet instanceof ClientboundDamageEventPacket) {
            ClientboundDamageEventPacket damagePacket = (ClientboundDamageEventPacket)packet;
            if (damagePacket.entityId() == mc.player.getId()) {
                this.receiveDamage = true;
            }
        }

        if (packet instanceof ClientboundSetEntityMotionPacket) {
            ClientboundSetEntityMotionPacket velocityPacket = (ClientboundSetEntityMotionPacket)packet;
            if (velocityPacket.getId() != mc.player.getId()) {
                return;
            }

            this.velocityInput = true;
            this.targetEntity = Aura.target;

            if (this.mode.isCurrentMode("NoXZ")) {
                if (this.receiveDamage) {
                    this.receiveDamage = false;
                    this.attackQueue = getAttackCount();

                    if (this.Logging.getCurrentValue()) {
                        ChatUtils.addChatMessage("NoXZ Queue set: " + this.attackQueue + " attacks");
                    }
                }
            }
        }
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        if (mc.player == null) return;

        if (mc.player.hurtTime == 0) {
            this.velocityInput = false;
            this.currentKnockbackSpeed = 0.0;
        }

        if (this.mode.isCurrentMode("JumpReset")) {
            if (this.velocityInput && mc.player.onGround()) {
                mc.player.setDeltaMovement(mc.player.getDeltaMovement().x, 0.42, mc.player.getDeltaMovement().z);
                this.velocityInput = false;
            }
        }

        if (this.mode.isCurrentMode("NoXZ") && this.targetEntity != null && this.attackQueue > 0) {
            if (this.noXZMode.isCurrentMode("OneTime")) {
                for (; this.attackQueue >= 1; this.attackQueue--) {
                    mc.getConnection().send(ServerboundInteractPacket.createAttackPacket(this.targetEntity, false));
                    mc.player.setDeltaMovement(mc.player.getDeltaMovement().multiply(0.6, 1, 0.6));
                    mc.player.setSprinting(false);
                    mc.player.swing(InteractionHand.MAIN_HAND);
                }
                if (this.Logging.getCurrentValue()) {
                    ChatUtils.addChatMessage("NoXZ OneTime attacks executed");
                }
            } else if (this.noXZMode.isCurrentMode("PerTick")) {
                if (this.attackQueue >= 1) {
                    mc.getConnection().send(ServerboundInteractPacket.createAttackPacket(this.targetEntity, false));
                    mc.player.setDeltaMovement(mc.player.getDeltaMovement().multiply(0.6, 1, 0.6));
                    mc.player.setSprinting(false);
                    mc.player.swing(InteractionHand.MAIN_HAND);

                    if (this.Logging.getCurrentValue()) {
                        ChatUtils.addChatMessage("NoXZ PerTick attack executed, remaining: " + (this.attackQueue - 1));
                    }
                }
                this.attackQueue--;
            }
        }
    }

    private int getAttackCount() {
        if (randomAttackCount.getCurrentValue()) {
            int min = (int) minAttacks.getCurrentValue();
            int max = (int) maxAttacks.getCurrentValue();
            if (min > max) {
                int temp = min;
                min = max;
                max = temp;
            }
            if (min == max) {
                return min;
            }
            return new java.util.Random().nextInt(max - min + 1) + min;
        } else {
            return (int) attacks.getCurrentValue();
        }
    }
}