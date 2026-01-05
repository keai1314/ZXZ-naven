package com.heypixel.heypixelmod.obsoverlay.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class PlayerUtils {
   private static final Minecraft mc = Minecraft.getInstance();
   
   // 玩家卡住计数器，初始值为0
   public static int playerStuckTicks = 0;
   
   // 卡住计数器的最大安全值，防止整数溢出
   private static final int MAX_STUCK_TICKS = 10000;
   
   // 是否使用自动递减关闭逻辑，默认为false
   public static boolean useAutoDecrementClose = false;
   
   /**
    * 安全地增加卡住计数器，防止整数溢出
    */
   public static void incrementPlayerStuckTicks() {
       if (playerStuckTicks < MAX_STUCK_TICKS) {
           playerStuckTicks++;
       }
   }
   
   /**
    * 重置卡住计数器
    */
   public static void resetPlayerStuckTicks() {
       playerStuckTicks = 0;
   }

   public static boolean movementInput() {
      return mc.options.keyUp.isDown() || mc.options.keyDown.isDown() || mc.options.keyLeft.isDown() || mc.options.keyRight.isDown();
   }

   public static int getMoveSpeedEffectAmplifier() {
      return mc.player.hasEffect(MobEffects.MOVEMENT_SPEED) ? mc.player.getEffect(MobEffects.MOVEMENT_SPEED).getAmplifier() + 1 : 0;
   }
   
   /**
    * 检查玩家脚下是否有方块，使用与GrimAC类似的检测逻辑
    */
   public static boolean hasBlockUnderFeet() {
      if (mc.player == null || mc.level == null) {
         return false;
      }
      
      // 使用与GrimAC类似的碰撞箱检查
      BlockPos playerPos = BlockPos.containing(mc.player.getX(), mc.player.getY() - 0.5, mc.player.getZ());
      
      // 检查玩家脚下及其周围的方块，与GrimAC的检测范围一致
      for (int x = -1; x <= 1; x++) {
         for (int z = -1; z <= 1; z++) {
             BlockPos checkPos = playerPos.offset(x, 0, z);
             if (!mc.level.isEmptyBlock(checkPos)) {
                 return true;
             }
         }
      }
      
      return false;
   }
   
   /**
    * 向正脚下(pitch90)发射射线，检测2格内是否有方块
    */
   public static boolean isBlockBelowInRange() {
      if (mc.player == null || mc.level == null) {
         return false;
      }
      
      // 从玩家脚的位置向正下方发射射线，而不是眼睛位置
      Vec3 playerPos = mc.player.position();
      Vec3 startPos = new Vec3(playerPos.x, playerPos.y, playerPos.z);
      Vec3 endPos = startPos.add(0, -2, 0); // 向下2格
      
      // 执行射线检测，使用BLOCK.OUTLINE确保检测所有方块
      HitResult hitResult = mc.level.clip(new ClipContext(
          startPos,
          endPos,
          ClipContext.Block.OUTLINE, // 使用OUTLINE确保检测所有方块
          ClipContext.Fluid.NONE,
          mc.player
      ));
      
      // 检查是否命中方块，且距离在2格内
      if (hitResult.getType() == HitResult.Type.BLOCK) {
          // 额外检查命中的方块是否在玩家正下方
          BlockPos hitPos = ((BlockHitResult) hitResult).getBlockPos();
          BlockPos playerBlockPos = BlockPos.containing(playerPos);
          
          // 检查命中的方块是否在玩家正下方1格或2格
          return (hitPos.getY() == playerBlockPos.getY() - 1 || hitPos.getY() == playerBlockPos.getY() - 2) &&
                 Math.abs(hitPos.getX() - playerBlockPos.getX()) <= 1 &&
                 Math.abs(hitPos.getZ() - playerBlockPos.getZ()) <= 1;
      }
      
      return false;
   }

   public static Vec3 getVectorForRotation(Vector2f rotation) {
      float yawCos = (float)Math.cos((double)(-rotation.getX() * (float) (Math.PI / 180.0) - (float) Math.PI));
      float yawSin = (float)Math.sin((double)(-rotation.getX() * (float) (Math.PI / 180.0) - (float) Math.PI));
      float pitchCos = (float)(-Math.cos((double)(-rotation.getY() * (float) (Math.PI / 180.0))));
      float pitchSin = (float)Math.sin((double)(-rotation.getY() * (float) (Math.PI / 180.0)));
      return new Vec3((double)(yawSin * pitchCos), (double)pitchSin, (double)(yawCos * pitchCos));
   }

   public static HitResult pickCustom(double blockReachDistance, float yaw, float pitch) {
      if (mc.player != null && mc.level != null) {
         Vec3 vec3 = mc.player.getEyePosition(1.0F);
         Vec3 vec31 = getVectorForRotation(new Vector2f(yaw, pitch));
         Vec3 vec32 = vec3.add(vec31.x * blockReachDistance, vec31.y * blockReachDistance, vec31.z * blockReachDistance);
         return mc.level.clip(new ClipContext(vec3, vec32, Block.OUTLINE, Fluid.NONE, mc.player));
      } else {
         return null;
      }
   }
}
