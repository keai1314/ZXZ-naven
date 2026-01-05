package com.heypixel.heypixelmod.obsoverlay.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult;

public class FallingPlayer {
   public double x;
   public double y;
   public double z;
   private double motionX;
   private double motionY;
   private double motionZ;
   private final float yaw;
   private final float strafe;
   private final float forward;
   private float jumpMovementFactor;
   private Minecraft mc = Minecraft.getInstance();

   public FallingPlayer(double x, double y, double z, double motionX, double motionY, double motionZ, float yaw, float strafe, float forward, float jumpMovementFactor) {
      this.x = x;
      this.y = y;
      this.z = z;
      this.motionX = motionX;
      this.motionY = motionY;
      this.motionZ = motionZ;
      this.yaw = yaw;
      this.strafe = strafe;
      this.forward = forward;
      this.jumpMovementFactor = jumpMovementFactor;
   }

   public FallingPlayer(Player player) {
      this(
         player.getX(),
         player.getY(),
         player.getZ(),
         player.getDeltaMovement().x,
         player.getDeltaMovement().y,
         player.getDeltaMovement().z,
         player.getYRot(),
         player.xxa,
         player.zza,
         0.42F
      );
      float f = player.level().getBlockState(player.blockPosition()).getBlock().getJumpFactor();
      float f1 = player.level().getBlockState(player.getOnPos()).getBlock().getJumpFactor();
      float jumpingVelocity = 0.42F * ((double)f == 1.0 ? f1 : f) + player.getJumpBoostPower();
      this.jumpMovementFactor = jumpingVelocity;
   }

   private void calculateForTick2() {
      float sr = this.strafe;
      float fw = this.forward;
      float v = sr * sr + fw * fw;
      if (v >= 1.0E-4F) {
         v = Mth.sqrt(v);
         if (v < 1.0F) {
            v = 1.0F;
         }

         float fixedJumpFactor = this.jumpMovementFactor;
         if (this.mc.player != null && this.mc.player.isSprinting()) {
            fixedJumpFactor *= 1.3F;
         }

         v = fixedJumpFactor / v;
         sr *= v;
         fw *= v;
         float f1 = Mth.sin(this.yaw * (float) Math.PI / 180.0F);
         float f2 = Mth.cos(this.yaw * (float) Math.PI / 180.0F);
         this.motionX += (double)(sr * f2 - fw * f1);
         this.motionZ += (double)(fw * f2 + sr * f1);
      }

      this.motionY -= 0.08;
      this.motionY *= 0.98F;
      this.x = this.x + this.motionX;
      this.y = this.y + this.motionY;
      this.z = this.z + this.motionZ;
   }

   private void calculateForTick() {
      float sr = this.strafe * 0.9800000190734863f;
      float fw = this.forward * 0.9800000190734863f;
      float v = sr * sr + fw * fw;
      if (v >= 0.0001f) {
         v = Mth.sqrt(v);
         if (v < 1.0f) {
            v = 1.0f;
         }

         float fixedJumpFactor = this.jumpMovementFactor;
         if (this.mc.player != null && this.mc.player.isSprinting()) {
            fixedJumpFactor = fixedJumpFactor * 1.3f;
         }
         v = fixedJumpFactor / v;
         sr *= v;
         fw *= v;
         float f1 = Mth.sin(this.yaw * (float) Math.PI / 180.0f);
         float f2 = Mth.cos(this.yaw * (float) Math.PI / 180.0f);
         this.motionX += (double)(sr * f2 - fw * f1);
         this.motionZ += (double)(fw * f2 + sr * f1);
      }
      this.motionY -= 0.08;
      this.motionY *= 0.9800000190734863;
      this.x += this.motionX;
      this.y += this.motionY;
      this.z += this.motionZ;
      this.motionX *= 0.91;
      this.motionZ *= 0.91;
   }

   public void calculateMLG(int ticks) {
      for (int i = 0; i < ticks; i++) {
         this.calculateForTick2();
      }
   }

   public void calculate(int ticks) {
      for (int i = 0; i < ticks; i++) {
         this.calculateForTick();
      }
   }
   
   public BlockPos findCollision(int ticks) {
      for (int i = 0; i < ticks; i++) {
         Vec3 start = new Vec3(this.x, this.y, this.z);
         calculateForTick();
         Vec3 end = new Vec3(this.x, this.y, this.z);
         BlockPos raytracedBlock;
         float w = this.mc.player != null ? this.mc.player.getBbWidth() / 2f : 0.3f;
         if ((raytracedBlock = rayTrace(start, end)) != null) return raytracedBlock;
         if ((raytracedBlock = rayTrace(start.add(w, 0.0, w), end)) != null) return raytracedBlock;
         if ((raytracedBlock = rayTrace(start.add(-w, 0.0, w), end)) != null) return raytracedBlock;
         if ((raytracedBlock = rayTrace(start.add(w, 0.0, -w), end)) != null) return raytracedBlock;
         if ((raytracedBlock = rayTrace(start.add(-w, 0.0, -w), end)) != null) return raytracedBlock;
         if ((raytracedBlock = rayTrace(start.add(w, 0.0, w / 2f), end)) != null) return raytracedBlock;
         if ((raytracedBlock = rayTrace(start.add(-w, 0.0, w / 2f), end)) != null) return raytracedBlock;
         if ((raytracedBlock = rayTrace(start.add(w / 2f, 0.0, w), end)) != null) return raytracedBlock;
         if ((raytracedBlock = rayTrace(start.add(w / 2f, 0.0, -w), end)) != null) return raytracedBlock;
      }
      return null;
   }
   
   private BlockPos rayTrace(Vec3 start, Vec3 end) {
      if (this.mc.level == null) return null;
      // 使用正确的ClipContext参数调用clip方法
      net.minecraft.world.level.ClipContext clipContext = new net.minecraft.world.level.ClipContext(
              start,
              end,
              net.minecraft.world.level.ClipContext.Block.COLLIDER,
              net.minecraft.world.level.ClipContext.Fluid.NONE,
              mc.player
      );
      HitResult result = this.mc.level.clip(clipContext);
      if (result.getType() == HitResult.Type.BLOCK && result instanceof net.minecraft.world.phys.BlockHitResult blockHitResult) {
         if (blockHitResult.getDirection() == Direction.UP) {
            return blockHitResult.getBlockPos();
         }
      }
      return null;
   }
   
   public double getX() {
      return x;
   }
   
   public double getY() {
      return y;
   }
   
   public double getZ() {
      return z;
   }
}
