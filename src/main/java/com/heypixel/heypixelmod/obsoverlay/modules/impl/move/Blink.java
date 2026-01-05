package com.heypixel.heypixelmod.obsoverlay.modules.impl.move;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventAttackSlowdown;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventAttackYaw;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMotion;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventPacket;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRender2D;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.combat.AntiBots;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.misc.Teams;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.render.projectiles.ProjectileData;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.render.projectiles.datas.BasicProjectileData;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.render.projectiles.datas.EntityArrowData;
import com.heypixel.heypixelmod.obsoverlay.utils.BlinkingPlayer;
import com.heypixel.heypixelmod.obsoverlay.utils.FriendManager;
import com.heypixel.heypixelmod.obsoverlay.utils.NetworkUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.RayTraceUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.RenderUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.SmoothAnimationTimer;
import com.heypixel.heypixelmod.obsoverlay.utils.rotation.RotationUtils;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import java.awt.Color;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import net.minecraft.network.protocol.login.ServerboundHelloPacket;
import net.minecraft.network.protocol.login.ServerboundKeyPacket;
import net.minecraft.network.protocol.status.ServerboundPingRequestPacket;
import net.minecraft.network.protocol.status.ServerboundStatusRequestPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.entity.projectile.ThrownEgg;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import com.heypixel.heypixelmod.obsoverlay.ui.LanguageManager;

@ModuleInfo(
   name = "瞬移",
   category = Category.MOVEMENT,
   description = "暂停所有移动数据包以进行传送!"
)
public class Blink extends Module {
   private final EntityArrowData arrowData = new EntityArrowData();
   private final BasicProjectileData eggData = new BasicProjectileData(Collections.singleton(ThrownEgg.class), new Color(255, 238, 154));
   private final BasicProjectileData snowballData = new BasicProjectileData(Collections.singleton(Snowball.class), new Color(255, 255, 255));
   private static final int mainColor = new Color(150, 45, 45, 255).getRGB();
   public static final Set<Class<?>> whitelist = new HashSet<Class<?>>() {
      {
         this.add(ClientIntentionPacket.class);
         this.add(ServerboundStatusRequestPacket.class);
         this.add(ServerboundPingRequestPacket.class);
         this.add(ServerboundHelloPacket.class);
         this.add(ServerboundKeyPacket.class);
      }
   };
   private final Queue<Packet<?>> packets = new ConcurrentLinkedQueue<>();
   private final SmoothAnimationTimer progress = new SmoothAnimationTimer(0.0F, 0.2F);
   public FloatValue releaseOnDamage = ValueBuilder.create(this, "在伤害时释放Ticks")
      .setMinFloatValue(0.0F)
      .setMaxFloatValue(50.0F)
      .setDefaultFloatValue(20.0F)
      .setFloatStep(1.0F)
      .build()
      .getFloatValue();
   public FloatValue releaseSpeed = ValueBuilder.create(this, "释放速度 (Tick)")
      .setMinFloatValue(3.0F)
      .setMaxFloatValue(20.0F)
      .setDefaultFloatValue(10.0F)
      .setFloatStep(1.0F)
      .build()
      .getFloatValue();
   public FloatValue maxTicks = ValueBuilder.create(this, "最大Tick")
      .setMinFloatValue(10.0F)
      .setMaxFloatValue(500.0F)
      .setDefaultFloatValue(200.0F)
      .setFloatStep(1.0F)
      .build()
      .getFloatValue();
   public FloatValue playerDistance = ValueBuilder.create(this, "玩家距离")
      .setMinFloatValue(3.0F)
      .setMaxFloatValue(10.0F)
      .setDefaultFloatValue(4.0F)
      .setFloatStep(0.1F)
      .build()
      .getFloatValue();
   public FloatValue tntDistance = ValueBuilder.create(this, "TNT距离")
      .setMinFloatValue(3.0F)
      .setMaxFloatValue(10.0F)
      .setDefaultFloatValue(5.0F)
      .setFloatStep(0.1F)
      .build()
      .getFloatValue();
   public FloatValue projectilesExpands = ValueBuilder.create(this, "假的玩家碰撞箱")
      .setMinFloatValue(0.0F)
      .setMaxFloatValue(3.0F)
      .setDefaultFloatValue(0.2F)
      .setFloatStep(0.01F)
      .build()
      .getFloatValue();
   public BooleanValue fakeLag = ValueBuilder.create(this, "FakeLag")
      .setDefaultBooleanValue(false)
      .build()
      .getBooleanValue();
   public FloatValue fakeLagCooldown = ValueBuilder.create(this, "FakeLag空档期 (ms)")
      .setMinFloatValue(0.0F)
      .setMaxFloatValue(2000.0F)
      .setDefaultFloatValue(500.0F)
      .setFloatStep(50.0F)
      .build()
      .getFloatValue();
   private boolean disabling = false;
   private RemotePlayer fakePlayer;
   private int shouldReleaseTicks = 0;
   private int releasedTicks = 0;
   private long lastAttackTime = 0;
   private boolean inFakeLagCooldown = false;
   private int movePacketCount = 0; // 移动数据包计数器

   private long getBlinkTicks() {
      return this.movePacketCount;
   }
   
   // 添加getter方法，用于访问private的packets字段
   public Queue<Packet<?>> getPackets() {
      return this.packets;
   }

   private void handleMove(ServerboundMovePlayerPacket packet) {
      this.fakePlayer
         .lerpTo(
            packet.getX(this.fakePlayer.getX()),
            packet.getY(this.fakePlayer.getY()),
            packet.getZ(this.fakePlayer.getZ()),
            packet.getYRot(this.fakePlayer.getYRot()),
            packet.getXRot(this.fakePlayer.getXRot()),
            3,
            false
         );
      if (packet.hasRotation()) {
         this.fakePlayer.setYRot(packet.getYRot(this.fakePlayer.getYRot()));
         this.fakePlayer.setYHeadRot(packet.getYRot(this.fakePlayer.getYRot()));
         this.fakePlayer.setXRot(packet.getXRot(this.fakePlayer.getXRot()));
      }
   }

   private void releaseTick() {
      while (!this.packets.isEmpty()) {
         Packet<?> poll = this.packets.poll();
         NetworkUtils.sendPacketNoEvent(poll);
         if (poll instanceof ServerboundMovePlayerPacket) {
            this.releasedTicks++;
            this.movePacketCount--;
            this.handleMove((ServerboundMovePlayerPacket)poll);
            break;
         }
      }
   }
   
   // 添加public方法，用于从外部释放所有数据包
   public void releaseAllPackets() {
      while (!this.packets.isEmpty()) {
         this.releaseTick();
      }
      this.movePacketCount = 0; // 重置移动数据包计数器
   }
   
   // 添加public方法，用于从外部触发FakeLag
   public void triggerFakeLag() {
      if (this.fakeLag.getCurrentValue() && !this.inFakeLagCooldown) {
         this.releaseAllPackets();
         this.lastAttackTime = System.currentTimeMillis();
         this.inFakeLagCooldown = true;
      }
   }

   @Override
   public void onEnable() {
      this.packets.clear();
      this.shouldReleaseTicks = 0;
      this.disabling = false;
      this.fakePlayer = new BlinkingPlayer(mc.player);
      this.fakePlayer.setSprinting(mc.player.isSprinting());
      mc.level.addPlayer(-1337, this.fakePlayer);
   }

   @Override
   public void onDisable() {
      if (this.fakePlayer != null) {
         mc.level.removeEntity(this.fakePlayer.getId(), RemovalReason.DISCARDED);
         this.fakePlayer = null;
      }
   }

   @EventTarget
   public void onRender(EventRender2D e) {
      int x = mc.getWindow().getGuiScaledWidth() / 2 - 50;
      int y = mc.getWindow().getGuiScaledHeight() / 2 + 15;
      this.progress.update(true);
      RenderUtils.drawRoundedRect(e.getStack(), (float)x, (float)y, 100.0F, 5.0F, 2.0F, Integer.MIN_VALUE);
      RenderUtils.drawRoundedRect(e.getStack(), (float)x, (float)y, this.progress.value, 5.0F, 2.0F, mainColor);
   }

   private boolean isPlayerNear(double distance) {
      for (AbstractClientPlayer player : mc.level.players()) {
         // 跳过自己
         if (player == mc.player) {
            continue;
         }
         // 跳过假玩家
         if (this.fakePlayer != null && player == this.fakePlayer) {
            continue;
         }
         // 跳过队友
         if (Teams.isSameTeam(player)) {
            continue;
         }
         // 跳过好友
         if (FriendManager.isFriend(player)) {
            continue;
         }
         // 跳过机器人
         if (AntiBots.isBot(player)) {
            continue;
         }
         // 检查距离
         Vec3 eyePosition = player.getEyePosition();
         Vec3 closestPoint = RotationUtils.getClosestPoint(eyePosition, this.fakePlayer.getBoundingBox());
         if (eyePosition.distanceTo(closestPoint) < distance) {
            return true; // 找到一个即可返回
         }
      }
      return false;
   }

   private boolean isTNTNear(double distance) {
      for (Entity entity : mc.level.entitiesForRendering()) {
         if (entity instanceof PrimedTnt && this.fakePlayer.distanceTo(entity) <= distance) {
            return true; // 找到一个TNT即可返回
         }
      }
      return false;
   }

   private boolean isArrowNear(double expands) {
      // 只检查距离假玩家10格以内的投射物
      double maxDistanceCheck = 10.0;
      for (Entity entity : mc.level.entitiesForRendering()) {
         // 先快速检查距离，过滤掉远处的投射物
         if (this.fakePlayer.distanceTo(entity) > maxDistanceCheck) {
            continue;
         }
         
         ProjectileData data;
         if (entity instanceof Arrow) {
            data = this.arrowData;
         } else if (entity instanceof ThrownEgg) {
            data = this.eggData;
         } else {
            if (!(entity instanceof Snowball)) {
               continue;
            }
            data = this.snowballData;
         }

         if (data != null && this.checkProjectile(entity, data, expands)) {
            return true;
         }
      }
      return false;
   }

   private boolean checkProjectile(Entity entity, ProjectileData projectileInfo, double expands) {
      LocalPlayer thePlayer = mc.player;
      ClientLevel theWorld = mc.level;
      double posX = entity.getX();
      double posY = entity.getY();
      double posZ = entity.getZ();
      double motionX = entity.getDeltaMovement().x;
      double motionY = entity.getDeltaMovement().y;
      double motionZ = entity.getDeltaMovement().z;
      
      // 限制最大模拟步数，避免无限循环
      int maxSimSteps = 20;
      int simSteps = 0;

      while (simSteps < maxSimSteps) {
         simSteps++;
         
         // 获取投射物数据
         float data1 = projectileInfo.getData1();
         float data2 = projectileInfo.getData2();
         
         // 快速检查：如果投射物已经离假玩家太远，直接返回false
         if (this.fakePlayer.distanceToSqr(posX, posY, posZ) > 100.0) {
            return false;
         }
         
         // 创建AABB
         AABB aabb = new AABB(posX - data1, posY, posZ - data1, posX + data1, posY + data2, posZ + data1);
         
         // 检查是否与假玩家碰撞
         AABB fakePlayerAABB = this.fakePlayer.getBoundingBox().inflate(expands);
         if (aabb.intersects(fakePlayerAABB)) {
            return true;
         }
         
         // 计算下一个位置
         Vec3 vec3 = new Vec3(posX, posY, posZ);
         Vec3 vec3WithMotion = new Vec3(posX + motionX, posY + motionY, posZ + motionZ);
         
         // 射线追踪
         HitResult movingObj = RayTraceUtils.rayTraceBlocks(vec3, vec3WithMotion, false, entity instanceof Arrow, false, entity);
         
         // 更新位置
         posX += motionX;
         posY += motionY;
         posZ += motionZ;
         
         // 检查是否命中方块或超出世界边界
         if (!movingObj.getType().equals(Type.MISS) || posY < -128.0) {
            return false;
         }
         
         // 更新速度（应用阻力和重力）
         double drag = entity.isInWater() ? 0.8 : 0.99;
         motionX *= drag;
         motionY = motionY * drag - projectileInfo.getGravity();
         motionZ *= drag;
      }
      
      return false;
   }

   private boolean isPlayerInDanger() {
      return this.isTNTNear((double)this.tntDistance.getCurrentValue())
         || this.isPlayerNear((double)this.playerDistance.getCurrentValue())
         || this.isArrowNear((double)this.projectilesExpands.getCurrentValue());
   }

   @Override
   public void setEnabled(boolean enabled) {
      if (mc.player != null) {
         if (enabled) {
            super.setEnabled(true);
         } else if (!this.disabling) {
            this.disabling = true;
         } else if (this.packets.isEmpty()) {
            super.setEnabled(false);
         }
      }
   }

   @EventTarget
   public void onMotion(EventMotion e) {
      if (e.getType() == EventType.PRE && mc.player != null) {
         this.setSuffix(this.getBlinkTicks() + " Ticks Behind");
         this.progress.target = Mth.clamp((float)this.getBlinkTicks() / this.maxTicks.getCurrentValue() * 100.0F, 0.0F, 100.0F);
         this.releasedTicks = 0;
         if (mc.player.hurtTime == 10) {
            this.shouldReleaseTicks = this.shouldReleaseTicks + (int)this.releaseOnDamage.getCurrentValue();
         }

         while ((float)this.releasedTicks < this.releaseSpeed.getCurrentValue() && this.shouldReleaseTicks > 0 && !this.packets.isEmpty()) {
            this.releaseTick();
            this.shouldReleaseTicks--;
         }

         while ((float)this.releasedTicks < this.releaseSpeed.getCurrentValue() && this.isPlayerInDanger() && !this.packets.isEmpty()) {
            this.releaseTick();
         }

         while (
            (float)this.releasedTicks < this.releaseSpeed.getCurrentValue()
               && (float)this.getBlinkTicks() >= this.maxTicks.getCurrentValue()
               && !this.packets.isEmpty()
         ) {
            this.releaseTick();
         }

         if (this.disabling) {
            while ((float)this.releasedTicks < this.releaseSpeed.getCurrentValue() && !this.packets.isEmpty()) {
               this.releaseTick();
            }

            if (this.packets.isEmpty()) {
               this.setEnabled(false);
            }
         }
      }
   }

   @EventTarget
   public void onAttack(EventAttackSlowdown e) {
      if (this.fakeLag.getCurrentValue() && !this.inFakeLagCooldown) {
         this.releaseAllPackets();
         this.lastAttackTime = System.currentTimeMillis();
         this.inFakeLagCooldown = true;
      }
   }

   @EventTarget(4)
   public void onPacket(EventPacket e) {
      long currentTime = System.currentTimeMillis();
      if (this.inFakeLagCooldown && currentTime - this.lastAttackTime >= this.fakeLagCooldown.getCurrentValue()) {
         this.inFakeLagCooldown = false;
      }
      
      if (e.getType() == EventType.SEND && mc.player != null && !e.isCancelled()) {
         if (whitelist.contains(e.getPacket().getClass())) {
            return;
         }

         e.setCancelled(true);
         this.packets.offer(e.getPacket());
         if (e.getPacket() instanceof ServerboundMovePlayerPacket) {
            this.movePacketCount++;
         }
      }
   }

   public String getLocalizedName() {
      return LanguageManager.getInstance().getLocalizedString("瞬移", "Blink");
   }

   public String getLocalizedDescription() {
      return LanguageManager.getInstance().getLocalizedString("暂停所有移动数据包以进行传送!", "Pause all movement packets to teleport!");
   }
}
