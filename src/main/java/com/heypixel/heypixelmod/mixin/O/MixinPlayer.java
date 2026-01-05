package com.heypixel.heypixelmod.mixin.O;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventAttackSlowdown;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventAttackYaw;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventStayingOnGroundSurface;
import com.heypixel.heypixelmod.obsoverlay.utils.PlayerUtils;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Player.class})
public abstract class MixinPlayer extends LivingEntity {
   protected MixinPlayer(EntityType<? extends LivingEntity> pEntityType, Level pLevel) {
      super(pEntityType, pLevel);
   }

   @Redirect(
      method = {"attack"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/player/Player;getYRot()F"
      )
   )
   private float hookFixRotation(Player instance) {
      EventAttackYaw event = new EventAttackYaw(instance.getYRot());
      Naven.getInstance().getEventManager().call(event);
      return event.getYaw();
   }

   @Redirect(
      method = {"attack"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/player/Player;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V"
      )
   )
   private void hookSetDeltaMovement(Player instance, Vec3 vec3) {
      EventAttackSlowdown event = new EventAttackSlowdown();
      Naven.getInstance().getEventManager().call(event);
      if (!event.isCancelled()) {
         instance.setDeltaMovement(vec3);
      }
   }

   @Redirect(
      method = {"attack"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/player/Player;setSprinting(Z)V"
      )
   )
   private void hookSetSprinting(Player instance, boolean sprinting) {
      EventAttackSlowdown event = new EventAttackSlowdown();
      Naven.getInstance().getEventManager().call(event);
      if (!event.isCancelled()) {
         instance.setSprinting(sprinting);
      }
   }

   @Inject(
      method = {"isStayingOnGroundSurface"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void isStayingOnGroundSurface(CallbackInfoReturnable<Boolean> info) {
      EventStayingOnGroundSurface event = new EventStayingOnGroundSurface((Boolean)info.getReturnValue());
      Naven.getInstance().getEventManager().call(event);
      info.setReturnValue(event.isStay());
   }
   
   @Inject(
      method = {"tick"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onTick(CallbackInfo ci) {
      // 当卡住计数器大于0时
      if (PlayerUtils.playerStuckTicks > 0) {
         // 检查是否使用自动递减关闭逻辑
         if (PlayerUtils.useAutoDecrementClose) {
            // 使用自动递减关闭逻辑：每刻减1，直到归零
            PlayerUtils.playerStuckTicks--;
            ci.cancel(); // 取消玩家Tick事件，保持卡住状态
         } else {
            // 使用传统关闭逻辑：卡住超过15tick或者脚下有方块或者正脚下2格内有方块
            if (PlayerUtils.playerStuckTicks > 15 || PlayerUtils.hasBlockUnderFeet() || PlayerUtils.isBlockBelowInRange()) {
               // 释放玩家，重置计数器
               PlayerUtils.playerStuckTicks = 0;
            } else {
               // 继续卡住，计数递增逻辑已经在Scaffold模块中实现
               ci.cancel(); // 取消玩家Tick事件，冻结玩家行为
            }
         }
      }
   }
   
   // 确保playerStuckTicks在游戏主循环中被正确递增
   @Inject(
      method = {"tick"},
      at = {@At("TAIL")}
   )
   private void onTickTail(CallbackInfo ci) {
      // 这个方法会在玩家Tick事件的末尾被调用，确保playerStuckTicks能被正确递增
      // 但只有当玩家没有被卡住时才会执行，因为被卡住时head注入会取消事件
   }
}
