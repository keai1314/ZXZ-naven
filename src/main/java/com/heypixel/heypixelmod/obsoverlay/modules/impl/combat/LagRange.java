package com.heypixel.heypixelmod.obsoverlay.modules.impl.combat;

import com.heypixel.heypixelmod.obsoverlay.managers.LagManager;
import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMotion;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventPacket;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRender;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.utils.FriendManager;
import com.heypixel.heypixelmod.obsoverlay.utils.RenderUtils;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.ModeValue;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

@ModuleInfo(
   name = "LagRange",
   description = "通过延迟数据包来扩展有效攻击距离",
   category = Category.COMBAT
)
public class LagRange extends Module {
   private static final Minecraft mc = Minecraft.getInstance();
   private int tickIndex = -1;
   private long delayCounter = 0L;
   private boolean hasTarget = false;
   private net.minecraft.world.phys.Vec3 lastPosition = null;
   private net.minecraft.world.phys.Vec3 currentPosition = null;
   
   private final FloatValue delayValue = ValueBuilder.create(this, "延迟").setMinFloatValue(0.0F).setMaxFloatValue(1000.0F).setDefaultFloatValue(150.0F).setFloatStep(1.0F).build().getFloatValue();
   private final FloatValue rangeValue = ValueBuilder.create(this, "范围").setMinFloatValue(3.0F).setMaxFloatValue(100.0F).setDefaultFloatValue(10.0F).setFloatStep(0.1F).build().getFloatValue();
   private final BooleanValue weaponsOnlyValue = ValueBuilder.create(this, "仅武器").setDefaultBooleanValue(true).build().getBooleanValue();
   private final BooleanValue allowToolsValue = ValueBuilder.create(this, "允许工具").setDefaultBooleanValue(false).build().getBooleanValue();
   private final BooleanValue botCheckValue = ValueBuilder.create(this, "机器人检测").setDefaultBooleanValue(true).build().getBooleanValue();
   private final BooleanValue teamsValue = ValueBuilder.create(this, "队伍检测").setDefaultBooleanValue(true).build().getBooleanValue();
   private final ModeValue showPositionValue = ValueBuilder.create(this, "显示位置").setModes(new String[]{"NONE", "DEFAULT", "HUD"}).setDefaultModeIndex(0).build().getModeValue();

   public LagRange() {
      super("LagRange", "通过延迟数据包来扩展有效攻击距离", Category.COMBAT);
   }

   private boolean isValidTarget(Player entityPlayer) {
      if (mc.player == null) {
         return false;
      } else if (entityPlayer != mc.player && entityPlayer != mc.player.getVehicle()) {
         if (entityPlayer != mc.getCameraEntity() && entityPlayer != mc.getCameraEntity().getVehicle()) {
            if (entityPlayer.getHealth() <= 0) {
               return false;
            } else if (FriendManager.isFriend(entityPlayer.getName().getString())) {
               return false;
            } else {
               return (!this.teamsValue.getCurrentValue() || !this.isSameTeam(entityPlayer)) && (!this.botCheckValue.getCurrentValue() || !this.isBot(entityPlayer));
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private boolean isSameTeam(Player player) {
      if (mc.player == null) {
         return false;
      } else {
         String playerTeam = mc.player.getTeam() != null ? mc.player.getTeam().getName() : "";
         String targetTeam = player.getTeam() != null ? player.getTeam().getName() : "";
         return !playerTeam.isEmpty() && playerTeam.equals(targetTeam);
      }
   }

   private boolean isBot(Player player) {
      return mc.player == null ? false : player.getName().getString().equals(mc.player.getName().getString());
   }

   private boolean shouldResetOnPacket(Packet<?> packet) {
      if (packet instanceof ServerboundInteractPacket) {
         return true;
      } else if (packet instanceof ServerboundPlayerActionPacket) {
         ServerboundPlayerActionPacket actionPacket = (ServerboundPlayerActionPacket)packet;
         return actionPacket.getAction() != Action.RELEASE_USE_ITEM;
      } else if (!(packet instanceof ServerboundUseItemOnPacket)) {
         return false;
      } else {
         ServerboundUseItemOnPacket useItemPacket = (ServerboundUseItemOnPacket)packet;
         ItemStack item = mc.player.getItemInHand(useItemPacket.getHand());
         return item.isEmpty() || !(item.getItem() instanceof SwordItem);
      }
   }

   private double calculateDistanceToBox(Player player, net.minecraft.world.phys.Vec3 eyePosition) {
      AABB bb = player.getBoundingBox();
      if (bb == null) {
         return Double.MAX_VALUE;
      } else {
         double closestX = Math.max(bb.minX, Math.min(eyePosition.x, bb.maxX));
         double closestY = Math.max(bb.minY, Math.min(eyePosition.y, bb.maxY));
         double closestZ = Math.max(bb.minZ, Math.min(eyePosition.z, bb.maxZ));
         double deltaX = eyePosition.x - closestX;
         double deltaY = eyePosition.y - closestY;
         double deltaZ = eyePosition.z - closestZ;
         return Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
      }
   }

   @EventTarget
   public void onMotion(EventMotion event) {
      if (this.isEnabled()) {
         if (event.getType() == EventType.PRE) {
            if (mc.player == null) {
               return;
            }

            Naven.getInstance().getLagManager().setDelay(0);
            this.hasTarget = false;
            
            boolean usingItem = mc.player.isUsingItem();
            boolean blocking = mc.player.isBlocking();
            boolean weaponCheck = !this.weaponsOnlyValue.getCurrentValue() || this.hasUnbreakingEnchant() || this.allowToolsValue.getCurrentValue() && this.isHoldingTool();
            if ((!usingItem || blocking) && weaponCheck) {
               double height = (double)mc.player.getEyeHeight();
               LagManager.Vec3 lagPos = getLagPosition();
               net.minecraft.world.phys.Vec3 eyePosition = new net.minecraft.world.phys.Vec3(lagPos.x, lagPos.y + height, lagPos.z);
               net.minecraft.world.phys.Vec3 targetEyePosition = new net.minecraft.world.phys.Vec3(mc.player.xOld, mc.player.yOld + height, mc.player.zOld);
               net.minecraft.world.phys.Vec3 playerEyePosition = new net.minecraft.world.phys.Vec3(mc.player.getX(), mc.player.getY() + height, mc.player.getZ());
               List<Player> players = mc.level != null ? mc.level.players().stream().filter(this::isValidTarget).collect(Collectors.toList()) : List.of();
               if (players.isEmpty()) {
                  this.tickIndex = -1;
               } else {
                  Iterator<Player> var15 = players.iterator();

                  double distance;
                  double targetDist;
                  double eyeDist;
                  do {
                     Player player;
                     do {
                        if (!var15.hasNext()) {
                           return;
                        }

                        player = var15.next();
                        distance = this.calculateDistanceToBox(player, playerEyePosition);
                     } while(distance > (double)this.rangeValue.getCurrentValue());

                     targetDist = this.calculateDistanceToBox(player, targetEyePosition);
                     eyeDist = this.calculateDistanceToBox(player, eyePosition);
                  } while(!(distance < targetDist) && !(distance < eyeDist));

                  if (this.tickIndex < 0) {
                     this.tickIndex = 0;

                     for(this.delayCounter += (long)this.delayValue.getCurrentValue(); this.delayCounter > 0L; this.delayCounter -= 50L) {
                        ++this.tickIndex;
                     }
                  }

                  Naven.getInstance().getLagManager().setDelay(this.tickIndex);
                  this.hasTarget = true;
                  return;
               }
            } else {
               this.tickIndex = -1;
            }
         } else if (event.getType() == EventType.POST) {
            LagManager.Vec3 savedPosition = getLagPosition();
            if (this.currentPosition == null) {
               this.lastPosition = new net.minecraft.world.phys.Vec3(savedPosition.x, savedPosition.y, savedPosition.z);
            } else {
               this.lastPosition = this.currentPosition;
            }

            this.currentPosition = new net.minecraft.world.phys.Vec3(savedPosition.x, savedPosition.y, savedPosition.z);
         }
      }
   }

   private LagManager.Vec3 getLagPosition() {
      return Naven.getInstance().getLagManager().getLastPosition();
   }

   private boolean hasUnbreakingEnchant() {
      if (mc.player == null) {
         return false;
      } else {
         ItemStack stack = mc.player.getMainHandItem();
         if (stack.isEmpty()) {
            return false;
         } else {
            CompoundTag tag = stack.getTag();
            if (tag == null) {
               return false;
            } else {
               ListTag ench = tag.getList("ench", 10);
               if (ench.isEmpty()) {
                  return false;
               } else {
                  for(int i = 0; i < ench.size(); ++i) {
                     CompoundTag compound = ench.getCompound(i);
                     int id = compound.getInt("id");
                     if (id == 70) {
                        return true;
                     }
                  }

                  return false;
               }
            }
         }
      }
   }

   private boolean isHoldingTool() {
      if (mc.player == null) {
         return false;
      } else {
         ItemStack stack = mc.player.getMainHandItem();
         if (stack.isEmpty()) {
            return false;
         } else {
            return stack.getItem() instanceof AxeItem || stack.getItem() instanceof PickaxeItem || stack.getItem() instanceof ShovelItem;
         }
      }
   }

   @EventTarget
   public void onPacket(EventPacket event) {
      if (this.isEnabled()) {
         if (event.getType() == EventType.SEND && this.shouldResetOnPacket(event.getPacket())) {
            Naven.getInstance().getLagManager().setDelay(0);
            this.tickIndex = -1;
            this.hasTarget = false;
         }
      }
   }

   @EventTarget
   public void onRender(EventRender event) {
      if (this.isEnabled()) {
         if (this.showPositionValue.getCurrentValue() != 0 && mc.options.getCameraType() != CameraType.FIRST_PERSON && this.hasTarget && this.lastPosition != null && this.currentPosition != null) {
            double x = this.lerp(this.currentPosition.x, this.lastPosition.x, event.getRenderPartialTicks());
            double y = this.lerp(this.currentPosition.y, this.lastPosition.y, event.getRenderPartialTicks());
            double z = this.lerp(this.currentPosition.z, this.lastPosition.z, event.getRenderPartialTicks());
            double playerWidth = mc.player != null ? (double)mc.player.getBbWidth() : 0.6;
            double playerHeight = mc.player != null ? (double)mc.player.getBbHeight() : 1.8;
            net.minecraft.world.phys.Vec3 cameraPos = RenderUtils.getCameraPos();
            double renderX = x - cameraPos.x;
            double renderY = y - cameraPos.y;
            double renderZ = z - cameraPos.z;
            AABB aabb = new AABB(renderX - playerWidth / 2.0, renderY, renderZ - playerWidth / 2.0, renderX + playerWidth / 2.0, renderY + playerHeight, renderZ + playerWidth / 2.0);
            AABB expandedAabb = aabb.inflate(0.1, 0.1, 0.1);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableDepthTest();
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.5F);
            
            PoseStack poseStack = event.getPMatrixStack();
            RenderUtils.drawSolidBox(expandedAabb, poseStack);
            
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.enableDepthTest();
            RenderSystem.disableBlend();
         }
      }
   }

   private double lerp(double current, double last, float delta) {
      return current + (last - current) * (double)delta;
   }

   public void onDisable() {
      Naven.getInstance().getLagManager().setDelay(0);
      this.tickIndex = -1;
      this.delayCounter = 0L;
      this.hasTarget = false;
      this.lastPosition = null;
      this.currentPosition = null;
   }

   public String getSuffix() {
      return (int)this.delayValue.getCurrentValue() + "ms";
   }
}
