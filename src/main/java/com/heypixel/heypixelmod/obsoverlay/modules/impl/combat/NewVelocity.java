package com.heypixel.heypixelmod.obsoverlay.modules.impl.combat;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventPacket;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventUpdate;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMoveInput;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.ui.LanguageManager;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.ModeValue;
import com.heypixel.heypixelmod.obsoverlay.utils.ChatUtils;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;

@ModuleInfo(
        name = "新反击退",
        description = "减少击退.",
        category = Category.COMBAT
)
public class NewVelocity extends Module {
   private final ModeValue mode = ValueBuilder.create(this, "模式")
           .setDefaultModeIndex(0)
           .setModes("NoXZ", "跳跃重置")
           .build()
           .getModeValue();

   private final ModeValue noXZMode = ValueBuilder.create(this, "NoXZ模式")
           .setDefaultModeIndex(0)
           .setModes("一次", "每次")
           .setVisibility(() -> mode.isCurrentMode("NoXZ"))
           .build()
           .getModeValue();

   private final FloatValue attacks = ValueBuilder.create(this, "攻击次数")
           .setDefaultFloatValue(2.0F)
           .setMinFloatValue(1.0F)
           .setMaxFloatValue(5.0F)
           .setFloatStep(1.0F)
           .setVisibility(() -> mode.isCurrentMode("NoXZ"))
           .build()
           .getFloatValue();

   private final FloatValue jumpTick = ValueBuilder.create(this, "跳跃重置Tick")
           .setDefaultFloatValue(1.0F)
           .setMinFloatValue(0.0F)
           .setMaxFloatValue(9.0F)
           .setFloatStep(1.0F)
           .setVisibility(() -> mode.isCurrentMode("跳跃重置"))
           .build()
           .getFloatValue();

   private final BooleanValue Logging = ValueBuilder.create(this, "日志记录")
           .setDefaultBooleanValue(false)
           .build()
           .getBooleanValue();

   private final FloatValue rangeLimit = ValueBuilder.create(this, "范围")
           .setDefaultFloatValue(3.0F)
           .setMinFloatValue(1.0F)
           .setMaxFloatValue(6.0F)
           .setFloatStep(0.1F)
           .setVisibility(() -> mode.isCurrentMode("NoXZ"))
           .build()
           .getFloatValue();

   private final BooleanValue onlyInCombat = ValueBuilder.create(this, "仅在战斗期间")
           .setDefaultBooleanValue(false)
           .build()
           .getBooleanValue();

   private Entity targetEntity;
   private boolean velocityInput = false;
   private boolean attacked = false;
   private int jumpResetTicks = 0;
   private double currentKnockbackSpeed = 0.0;
   private int attackQueue = 0;
   private boolean receiveDamage = false;
   private BackTrack backTrackModule;

   @Override
   public void onEnable() {
      backTrackModule = (BackTrack) Naven.getInstance().getModuleManager().getModule(BackTrack.class);
   }

   @Override
   public void onDisable() {
      this.velocityInput = false;
      this.attacked = false;
      this.jumpResetTicks = 0;
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

         if (this.onlyInCombat.getCurrentValue()) {
            Aura auraModule = (Aura) Naven.getInstance().getModuleManager().getModule(Aura.class);
            if (auraModule == null || !auraModule.isEnabled() || Aura.target == null) {
               if (this.Logging.getCurrentValue()) {
                  ChatUtils.addChatMessage("Velocity cancelled: Not in combat");
               }
               double distance = distanceTo(Aura.target);
               if (distance > this.rangeLimit.getCurrentValue()) {
                  if (this.Logging.getCurrentValue()) {
                     ChatUtils.addChatMessage("Velocity cancelled: Target out of range (" + String.format("%.1f", distance) + " blocks)");
                  }
                  return;
               }
               return;
            }
         }

         if (this.mode.isCurrentMode("NoXZ")) {
            if (this.receiveDamage) {
               this.receiveDamage = false;
               this.attackQueue = (int)this.attacks.getCurrentValue();

               if (this.Logging.getCurrentValue()) {
                  ChatUtils.addChatMessage("NoXZ Queue set: " + this.attackQueue + " attacks");
               }
            }
         } else if (this.mode.isCurrentMode("跳跃重置")) {
            this.jumpResetTicks = (int)this.jumpTick.getCurrentValue();
            if (this.Logging.getCurrentValue()) {
               ChatUtils.addChatMessage("JumpReset scheduled in " + this.jumpResetTicks + " ticks");
            }
         }
      }
   }
   private double distanceTo(Entity entity) {
      if (mc.player == null || entity == null) return Double.MAX_VALUE;
      return mc.player.position().distanceTo(entity.position());
   }
   private boolean isBackTrackWorking() {
      if (backTrackModule == null) {
         backTrackModule = (BackTrack) Naven.getInstance().getModuleManager().getModule(BackTrack.class);
      }
      return backTrackModule != null && backTrackModule.isEnabled() && backTrackModule.btwork;
   }

   @EventTarget
   public void onUpdate(EventUpdate event) {
      if (mc.player == null) return;

      if (mc.player.hurtTime == 0) {
         this.velocityInput = false;
         this.currentKnockbackSpeed = 0.0;
      }

      if (this.jumpResetTicks > 0) {
         this.jumpResetTicks--;
      }

      // 如果BackTrack正在工作，暂停Velocity的攻击逻辑以避免冲突
      if (isBackTrackWorking()) {
         if (this.Logging.getCurrentValue()) {
            ChatUtils.addChatMessage("Velocity paused: BackTrack is working");
         }
         return;
      }

      if (this.mode.isCurrentMode("NoXZ") && this.targetEntity != null && this.attackQueue > 0) {
         if (this.noXZMode.isCurrentMode("一次")) {
            for (; this.attackQueue >= 1; this.attackQueue--) {
               mc.getConnection().send(ServerboundInteractPacket.createAttackPacket(this.targetEntity, false));
               mc.player.setDeltaMovement(mc.player.getDeltaMovement().multiply(0.6, 1, 0.6));
               mc.player.setSprinting(false);
               mc.player.swing(InteractionHand.MAIN_HAND);
            }
            if (this.Logging.getCurrentValue()) {
               ChatUtils.addChatMessage("NoXZ OneTime attacks executed");
            }
         } else if (this.noXZMode.isCurrentMode("每次")) {
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

   @EventTarget
   public void onMoveInput(EventMoveInput event) {
      if (mc.player != null && this.mode.isCurrentMode("跳跃重置") &&
              mc.player.onGround() && this.jumpResetTicks == 1) {
         event.setJump(true);
         this.jumpResetTicks = 0;
         if (this.Logging.getCurrentValue()) {
            ChatUtils.addChatMessage("Jump reset activated");
         }
      }
   }

    public String getLocalizedName() {
        return LanguageManager.getInstance().getLocalizedString("新反击退", "New Velocity");
    }

    public String getLocalizedDescription() {
        return LanguageManager.getInstance().getLocalizedString("减少击退", "Reduce knockback");
    }
}