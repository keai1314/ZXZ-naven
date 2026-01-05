package com.heypixel.heypixelmod.obsoverlay.modules.impl.combat;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventAttackSlowdown;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventClick;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRender;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRender2D;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRespawn;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRunTicks;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventShader;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.misc.KillSay;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.misc.Teams;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.move.Blink;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.move.Stuck;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.render.HUD;
import com.heypixel.heypixelmod.obsoverlay.utils.BlinkingPlayer;
import com.heypixel.heypixelmod.obsoverlay.utils.ChatUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.FriendManager;
import com.heypixel.heypixelmod.obsoverlay.utils.InventoryUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.NetworkUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.RenderUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.StencilUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.Vector2f;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.Fonts;
import com.heypixel.heypixelmod.obsoverlay.utils.rotation.RotationManager;
import com.heypixel.heypixelmod.obsoverlay.utils.rotation.RotationUtils;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.ModeValue;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Squid;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import com.heypixel.heypixelmod.obsoverlay.ui.LanguageManager;

@ModuleInfo(
        name = "杀戮光环",
        description = "自动攻击实体",
        category = Category.COMBAT
)
public class Aura extends Module {
   private static final float[] targetColorRed = new float[]{0.78431374F, 0.0F, 0.0F, 0.23529412F};
   private static final float[] targetColorGreen = new float[]{0.0F, 0.78431374F, 0.0F, 0.23529412F};
   public static Entity target;
   public static Entity aimingTarget;
   public static Vector2f rotation;
   public static List<Entity> targets = new ArrayList<>();
   BooleanValue targetHud = ValueBuilder.create(this, "目标信息显示").setDefaultBooleanValue(true).build().getBooleanValue();
   FloatValue targetHudScale = ValueBuilder.create(this, "目标信息缩放")
           .setDefaultFloatValue(1.0F)
           .setFloatStep(0.1F)
           .setMinFloatValue(0.1F)
           .setMaxFloatValue(3.0F)
           .setVisibility(() -> this.targetHud.getCurrentValue())
           .build()
           .getFloatValue();
   FloatValue targetHudX = ValueBuilder.create(this, "目标信息X坐标")
           .setDefaultFloatValue(10.0F)
           .setFloatStep(1.0F)
           .setMinFloatValue(-500.0F)
           .setMaxFloatValue(500.0F)
           .setVisibility(() -> this.targetHud.getCurrentValue())
           .build()
           .getFloatValue();
   FloatValue targetHudY = ValueBuilder.create(this, "目标信息Y坐标")
           .setDefaultFloatValue(10.0F)
           .setFloatStep(1.0F)
           .setMinFloatValue(-500.0F)
           .setMaxFloatValue(500.0F)
           .setVisibility(() -> this.targetHud.getCurrentValue())
           .build()
           .getFloatValue();
   FloatValue targetHudBackgroundAlpha = ValueBuilder.create(this, "目标信息背景透明度")
           .setDefaultFloatValue(0.4F)
           .setFloatStep(0.05F)
           .setMinFloatValue(0.0F)
           .setMaxFloatValue(1.0F)
           .setVisibility(() -> this.targetHud.getCurrentValue())
           .build()
           .getFloatValue();
   // 添加目标显示设置
   BooleanValue targetDisplay = ValueBuilder.create(this, "目标显示").setDefaultBooleanValue(true).build().getBooleanValue();
   FloatValue targetDisplayYSpeed = ValueBuilder.create(this, "目标显示Y移动速度")
           .setDefaultFloatValue(1.0F)
           .setFloatStep(0.1F)
           .setMinFloatValue(0.1F)
           .setMaxFloatValue(5.0F)
           .setVisibility(() -> this.targetDisplay.getCurrentValue())
           .build()
           .getFloatValue();
   FloatValue targetDisplayThickness = ValueBuilder.create(this, "目标显示粗细")
           .setDefaultFloatValue(8.0F)
           .setFloatStep(0.1F)
           .setMinFloatValue(1.0F)
           .setMaxFloatValue(20.0F)
           .setVisibility(() -> this.targetDisplay.getCurrentValue())
           .build()
           .getFloatValue();
   FloatValue targetDisplayRadius = ValueBuilder.create(this, "目标显示半径")
           .setDefaultFloatValue(1.5F)
           .setFloatStep(0.05F)
           .setMinFloatValue(0.1F)
           .setMaxFloatValue(3.0F)
           .setVisibility(() -> this.targetDisplay.getCurrentValue())
           .build()
           .getFloatValue();
   // 添加目标光环颜色设置
   FloatValue targetDisplayRed = ValueBuilder.create(this, "目标显示红色")
           .setDefaultFloatValue(255.0F)
           .setFloatStep(1.0F)
           .setMinFloatValue(0.0F)
           .setMaxFloatValue(255.0F)
           .setVisibility(() -> this.targetDisplay.getCurrentValue())
           .build()
           .getFloatValue();
   FloatValue targetDisplayGreen = ValueBuilder.create(this, "目标显示绿色")
           .setDefaultFloatValue(0.0F)
           .setFloatStep(1.0F)
           .setMinFloatValue(0.0F)
           .setMaxFloatValue(255.0F)
           .setVisibility(() -> this.targetDisplay.getCurrentValue())
           .build()
           .getFloatValue();
   FloatValue targetDisplayBlue = ValueBuilder.create(this, "目标显示蓝色")
           .setDefaultFloatValue(0.0F)
           .setFloatStep(1.0F)
           .setMinFloatValue(0.0F)
           .setMaxFloatValue(255.0F)
           .setVisibility(() -> this.targetDisplay.getCurrentValue())
           .build()
           .getFloatValue();
   // 添加目标光环宽度设置（控制光环的半径大小）
   FloatValue targetDisplayWidth = ValueBuilder.create(this, "目标显示宽度")
           .setDefaultFloatValue(1.0F)
           .setFloatStep(0.05F)
           .setMinFloatValue(0.1F)
           .setMaxFloatValue(2.0F)
           .setVisibility(() -> this.targetDisplay.getCurrentValue())
           .build()
           .getFloatValue();
   BooleanValue targetEsp = ValueBuilder.create(this, "目标透视").setDefaultBooleanValue(true).build().getBooleanValue();
   BooleanValue attackRangeDisplay = ValueBuilder.create(this, "攻击范围显示").setDefaultBooleanValue(false).build().getBooleanValue();
   FloatValue attackRangeYOffset = ValueBuilder.create(this, "攻击范围Y偏移")
           .setDefaultFloatValue(0.0F)
           .setFloatStep(0.1F)
           .setMinFloatValue(-2.0F)
           .setMaxFloatValue(2.0F)
           .setVisibility(() -> this.attackRangeDisplay.getCurrentValue())
           .build()
           .getFloatValue();
   FloatValue attackRangeRed = ValueBuilder.create(this, "攻击范围红色")
           .setDefaultFloatValue(0.0F)
           .setFloatStep(1.0F)
           .setMinFloatValue(0.0F)
           .setMaxFloatValue(255.0F)
           .setVisibility(() -> this.attackRangeDisplay.getCurrentValue())
           .build()
           .getFloatValue();
   FloatValue attackRangeGreen = ValueBuilder.create(this, "攻击范围绿色")
           .setDefaultFloatValue(255.0F)
           .setFloatStep(1.0F)
           .setMinFloatValue(0.0F)
           .setMaxFloatValue(255.0F)
           .setVisibility(() -> this.attackRangeDisplay.getCurrentValue())
           .build()
           .getFloatValue();
   FloatValue attackRangeBlue = ValueBuilder.create(this, "攻击范围蓝色")
           .setDefaultFloatValue(0.0F)
           .setFloatStep(1.0F)
           .setMinFloatValue(0.0F)
           .setMaxFloatValue(255.0F)
           .setVisibility(() -> this.attackRangeDisplay.getCurrentValue())
           .build()
           .getFloatValue();
   FloatValue attackRangeAlpha = ValueBuilder.create(this, "攻击范围透明度")
           .setDefaultFloatValue(150.0F)
           .setFloatStep(1.0F)
           .setMinFloatValue(0.0F)
           .setMaxFloatValue(255.0F)
           .setVisibility(() -> this.attackRangeDisplay.getCurrentValue())
           .build()
           .getFloatValue();
   FloatValue attackRangeThickness = ValueBuilder.create(this, "攻击范围粗细")
           .setDefaultFloatValue(12.0F)
           .setFloatStep(0.1F)
           .setMinFloatValue(1.0F)
           .setMaxFloatValue(20.0F)
           .setVisibility(() -> this.attackRangeDisplay.getCurrentValue())
           .build()
           .getFloatValue();
   BooleanValue attackPlayer = ValueBuilder.create(this, "攻击玩家").setDefaultBooleanValue(true).build().getBooleanValue();
   BooleanValue attackInvisible = ValueBuilder.create(this, "攻击隐形").setDefaultBooleanValue(false).build().getBooleanValue();
   BooleanValue attackAnimals = ValueBuilder.create(this, "攻击动物").setDefaultBooleanValue(false).build().getBooleanValue();
   BooleanValue attackMobs = ValueBuilder.create(this, "攻击生物").setDefaultBooleanValue(false).build().getBooleanValue();
   BooleanValue multi = ValueBuilder.create(this, "多重攻击").setDefaultBooleanValue(false).build().getBooleanValue();
   BooleanValue infSwitch = ValueBuilder.create(this, "无限切换").setDefaultBooleanValue(false).build().getBooleanValue();
   BooleanValue preferBaby = ValueBuilder.create(this, "关爱婴儿").setDefaultBooleanValue(false).build().getBooleanValue();
   BooleanValue moreParticles = ValueBuilder.create(this, "更多的粒子").setDefaultBooleanValue(false).build().getBooleanValue();
   BooleanValue blockAnimation = ValueBuilder.create(this, "格挡动画").setDefaultBooleanValue(false).build().getBooleanValue();
   FloatValue aimRange = ValueBuilder.create(this, "瞄准范围")
           .setDefaultFloatValue(5.0F)
           .setFloatStep(0.1F)
           .setMinFloatValue(1.0F)
           .setMaxFloatValue(6.0F)
           .build()
           .getFloatValue();
   FloatValue aps = ValueBuilder.create(this, "每秒攻击次数")
           .setDefaultFloatValue(10.0F)
           .setFloatStep(1.0F)
           .setMinFloatValue(1.0F)
           .setMaxFloatValue(20.0F)
           .build()
           .getFloatValue();
   FloatValue switchSize = ValueBuilder.create(this, "切换数量")
           .setDefaultFloatValue(1.0F)
           .setFloatStep(1.0F)
           .setMinFloatValue(1.0F)
           .setMaxFloatValue(5.0F)
           .setVisibility(() -> !this.infSwitch.getCurrentValue())
           .build()
           .getFloatValue();
   FloatValue switchAttackTimes = ValueBuilder.create(this, "切换延迟 (攻击次数)")
           .setDefaultFloatValue(1.0F)
           .setFloatStep(1.0F)
           .setMinFloatValue(1.0F)
           .setMaxFloatValue(10.0F)
           .build()
           .getFloatValue();
   FloatValue fov = ValueBuilder.create(this, "角度")
           .setDefaultFloatValue(360.0F)
           .setFloatStep(1.0F)
           .setMinFloatValue(10.0F)
           .setMaxFloatValue(360.0F)
           .build()
           .getFloatValue();
   FloatValue hurtTime = ValueBuilder.create(this, "受伤时间")
           .setDefaultFloatValue(10.0F)
           .setFloatStep(1.0F)
           .setMinFloatValue(0.0F)
           .setMaxFloatValue(10.0F)
           .build()
           .getFloatValue();
   ModeValue priority = ValueBuilder.create(this, "优先权").setModes("生命值", "角度", "范围", "无").build().getModeValue();
   RotationUtils.Data lastRotationData;
   RotationUtils.Data rotationData;
   int attackTimes = 0;
   float attacks = 0.0F;
   private int index;
   private Vector4f blurMatrix;

   // 添加用于格挡动画的变量
   private boolean isBlocking = false;
   private float mainHandHeight = 0.0F;
   private float oMainHandHeight = 0.0F;
   private ItemStack mainHandItem = ItemStack.EMPTY;
   
   // 添加目标显示光环相关变量
   private float targetDisplayYOffset = 0.0F;
   private float targetDisplayYDirection = 1.0F;

   @EventTarget
   public void onShader(EventShader e) {
      if (this.blurMatrix != null && this.targetHud.getCurrentValue()) {
         RenderUtils.drawRoundedRect(e.getStack(), this.blurMatrix.x(), this.blurMatrix.y(), this.blurMatrix.z(), this.blurMatrix.w(), 3.0F, 1073741824);
      }
   }

   @EventTarget
   public void onRender(EventRender2D e) {
      this.blurMatrix = null;
      if (target instanceof LivingEntity && this.targetHud.getCurrentValue()) {
         LivingEntity living = (LivingEntity)target;
         e.getStack().pushPose();
         float scale = this.targetHudScale.getCurrentValue();
         float x = (float)mc.getWindow().getGuiScaledWidth() / 2.0F + this.targetHudX.getCurrentValue();
         float y = (float)mc.getWindow().getGuiScaledHeight() / 2.0F + this.targetHudY.getCurrentValue();
         String targetName = target.getName().getString() + (living.isBaby() ? " (Baby)" : "");
         float width = Math.max(Fonts.harmony.getWidth(targetName, 0.4F * scale) + 10.0F * scale, 60.0F * scale);
         float height = 30.0F * scale;
         this.blurMatrix = new Vector4f(x, y, width, height);
         StencilUtils.write(false);
         RenderUtils.drawRoundedRect(e.getStack(), x, y, width, height, 5.0F * scale, HUD.headerColor);
         StencilUtils.erase(true);
         
         // 使用自定义的背景透明度
         int backgroundColor = (int)(this.targetHudBackgroundAlpha.getCurrentValue() * 255) << 24;
         RenderUtils.fillBound(e.getStack(), x, y, width, height, backgroundColor | 0xFFFFFF);
         
         RenderUtils.fillBound(e.getStack(), x, y, width * (living.getHealth() / living.getMaxHealth()), 3.0F * scale, HUD.headerColor);
         StencilUtils.dispose();
         Fonts.harmony.render(e.getStack(), targetName, (double)(x + 5.0F * scale), (double)(y + 6.0F * scale), Color.WHITE, true, 0.35F * scale);
         Fonts.harmony
                 .render(
                         e.getStack(),
                         "HP: " + Math.round(living.getHealth()) + (living.getAbsorptionAmount() > 0.0F ? "+" + Math.round(living.getAbsorptionAmount()) : ""),
                         (double)(x + 5.0F * scale),
                         (double)(y + 17.0F * scale),
                         Color.WHITE,
                         true,
                         0.35F * scale
                 );
         e.getStack().popPose();
      }
   }

   @EventTarget
   public void onRender(EventRender e) {
      // 简化条件判断，只检查功能是否开启
      if (this.attackRangeDisplay.getCurrentValue()) {
         this.renderAttackCircle(e);
      }

      if (this.targetEsp.getCurrentValue()) {
         PoseStack stack = e.getPMatrixStack();
         float partialTicks = e.getRenderPartialTicks();
         stack.pushPose();
         GL11.glEnable(3042);
         GL11.glBlendFunc(770, 771);
         GL11.glDisable(2929);
         GL11.glDepthMask(false);
         GL11.glEnable(2848);
         RenderSystem.setShader(GameRenderer::getPositionShader);
         RenderUtils.applyRegionalRenderOffset(stack);

         for (Entity entity : targets) {
            if (entity instanceof LivingEntity living) {
               float[] color = target == living ? targetColorRed : targetColorGreen;
               stack.pushPose();
               RenderSystem.setShaderColor(color[0], color[1], color[2], color[3]);
               double motionX = entity.getX() - entity.xo;
               double motionY = entity.getY() - entity.yo;
               double motionZ = entity.getZ() - entity.zo;
               AABB boundingBox = entity.getBoundingBox()
                       .move(-motionX, -motionY, -motionZ)
                       .move((double)partialTicks * motionX, (double)partialTicks * motionY, (double)partialTicks * motionZ);
               RenderUtils.drawSolidBox(boundingBox, stack);
               stack.popPose();
            }
         }

         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         GL11.glDisable(3042);
         GL11.glEnable(2929);
         GL11.glDepthMask(true);
         GL11.glDisable(2848);
         stack.popPose();
      }
      
      // 渲染目标显示光环
      if (this.targetDisplay.getCurrentValue() && target != null) {
         this.renderTargetDisplay(e);
      }
   }

   private void renderTargetDisplay(EventRender e) {
      // 更新Y轴偏移
      targetDisplayYOffset += targetDisplayYDirection * this.targetDisplayYSpeed.getCurrentValue() * 0.05F;
      
      // 如果偏移超过范围则反转方向
      if (targetDisplayYOffset > 1.0F) {
         targetDisplayYOffset = 1.0F;
         targetDisplayYDirection = -1.0F;
      } else if (targetDisplayYOffset < -1.0F) {
         targetDisplayYOffset = -1.0F;
         targetDisplayYDirection = 1.0F;
      }
      
      PoseStack stack = e.getPMatrixStack();
      float partialTicks = e.getRenderPartialTicks();
      
      // 计算实体位置，使用包围盒的中心点
      double motionX = target.getX() - target.xo;
      double motionY = target.getY() - target.yo;
      double motionZ = target.getZ() - target.zo;
      
      double x = target.xo + motionX * partialTicks;
      double y = target.yo + motionY * partialTicks;
      double z = target.zo + motionZ * partialTicks;
      
      // 获取实体的包围盒
      AABB boundingBox = target.getBoundingBox();
      double boxMinX = boundingBox.minX + motionX * partialTicks;
      double boxMinY = boundingBox.minY + motionY * partialTicks;
      double boxMinZ = boundingBox.minZ + motionZ * partialTicks;
      double boxMaxX = boundingBox.maxX + motionX * partialTicks;
      double boxMaxY = boundingBox.maxY + motionY * partialTicks;
      double boxMaxZ = boundingBox.maxZ + motionZ * partialTicks;
      
      // 计算包围盒中心点，并加上Y轴偏移
      Vec3 centerPos = new Vec3(
          (boxMinX + boxMaxX) / 2.0,
          (boxMinY + boxMaxY) / 2.0 + targetDisplayYOffset,
          (boxMinZ + boxMaxZ) / 2.0
      );
      
      // 使用包围盒的最大尺寸作为光环半径，确保能包裹整个实体
      double boxWidth = boxMaxX - boxMinX;
      double boxHeight = boxMaxY - boxMinY;
      double boxDepth = boxMaxZ - boxMinZ;
      float radius = (float) Math.max(Math.max(boxWidth, boxHeight), boxDepth) * this.targetDisplayRadius.getCurrentValue() * this.targetDisplayWidth.getCurrentValue();
      
      // 绘制多个同心圆来增强视觉效果，使用相同的半径但不同的透明度和粗细
      float thickness = this.targetDisplayThickness.getCurrentValue();
      int red = (int) this.targetDisplayRed.getCurrentValue();
      int green = (int) this.targetDisplayGreen.getCurrentValue();
      int blue = (int) this.targetDisplayBlue.getCurrentValue();
      RenderUtils.drawCircle(stack, centerPos, radius, new Color(red, green, blue, 230), thickness);
      RenderUtils.drawCircle(stack, centerPos, radius, new Color(red, green, blue, 200), thickness * 0.8f);
      RenderUtils.drawCircle(stack, centerPos, radius, new Color(red, green, blue, 180), thickness * 0.6f);
   }

   private void renderAttackCircle(EventRender e) {
      // 获取玩家位置并应用Y轴偏移，使用插值使跳跃时更平滑
      double playerX = mc.player.xo + (mc.player.getX() - mc.player.xo) * e.getRenderPartialTicks();
      double playerY = mc.player.yo + (mc.player.getY() - mc.player.yo) * e.getRenderPartialTicks();
      double playerZ = mc.player.zo + (mc.player.getZ() - mc.player.zo) * e.getRenderPartialTicks();
      Vec3 playerPos = new Vec3(playerX, playerY + this.attackRangeYOffset.getCurrentValue(), playerZ);

      // 获取颜色值 (确保值在0-255范围内)
      int red = Math.max(0, Math.min(255, (int) this.attackRangeRed.getCurrentValue()));
      int green = Math.max(0, Math.min(255, (int) this.attackRangeGreen.getCurrentValue()));
      int blue = Math.max(0, Math.min(255, (int) this.attackRangeBlue.getCurrentValue()));
      int alpha = Math.max(0, Math.min(255, (int) this.attackRangeAlpha.getCurrentValue()));
      
      // 获取粗细值
      float thickness = this.attackRangeThickness.getCurrentValue();
      
      // 创建颜色对象
      Color color = new Color(red, green, blue, alpha);
      
      // 使用RenderUtils.drawCircle方法绘制圆环，传递线条粗细参数
      RenderUtils.drawCircle(e.getPMatrixStack(), playerPos, 4.0F, color, thickness);
   }

   @Override
   public void onEnable() {
      rotation = null;
      this.index = 0;
      target = null;
      aimingTarget = null;
      targets.clear();
      // 重置目标显示偏移
      targetDisplayYOffset = 0.0F;
      targetDisplayYDirection = 1.0F;
   }

   @Override
   public void onDisable() {
      target = null;
      aimingTarget = null;
      super.onDisable();
   }

   @EventTarget
   public void onRespawn(EventRespawn e) {
      target = null;
      aimingTarget = null;
      this.toggle();
   }

   @EventTarget
   public void onAttackSlowdown(EventAttackSlowdown e) {
      e.setCancelled(true);
   }

   @EventTarget
   public void onMotion(EventRunTicks event) {
      if (event.getType() == EventType.PRE && mc.player != null) {
         if (mc.screen instanceof AbstractContainerScreen
                 || Naven.getInstance().getModuleManager().getModule(Stuck.class).isEnabled()
                 || InventoryUtils.shouldDisableFeatures()) {
            target = null;
            aimingTarget = null;
            this.rotationData = null;
            rotation = null;
            this.lastRotationData = null;
            targets.clear();
            return;
         }

         boolean isSwitch = this.switchSize.getCurrentValue() > 1.0F;
         this.setSuffix(this.multi.getCurrentValue() ? "Multi" : (isSwitch ? "Switch" : "Single"));
         this.updateAttackTargets();
         aimingTarget = this.shouldPreAim();
         this.lastRotationData = this.rotationData;
         this.rotationData = null;
         if (aimingTarget != null) {
            this.rotationData = RotationUtils.getRotationDataToEntity(aimingTarget);
            if (this.rotationData.getRotation() != null) {
               rotation = this.rotationData.getRotation();
            } else {
               rotation = null;
            }
         }

         if (targets.isEmpty()) {
            target = null;
            return;
         }

         if (this.index > targets.size() - 1) {
            this.index = 0;
         }

         if (targets.size() > 1
                 && ((float)this.attackTimes >= this.switchAttackTimes.getCurrentValue() || this.rotationData != null && this.rotationData.getDistance() > 3.0)) {
            this.attackTimes = 0;

            for (int i = 0; i < targets.size(); i++) {
               this.index++;
               if (this.index > targets.size() - 1) {
                  this.index = 0;
               }

               Entity nextTarget = targets.get(this.index);
               RotationUtils.Data data = RotationUtils.getRotationDataToEntity(nextTarget);
               if (data.getDistance() < 3.0) {
                  break;
               }
            }
         }

         if (this.index > targets.size() - 1 || !isSwitch) {
            this.index = 0;
         }

         target = targets.get(this.index);
         this.attacks = this.attacks + this.aps.getCurrentValue() / 20.0F;

         // 更新格挡动画状态
         updateBlockingAnimation();
      }
   }

   @EventTarget
   public void onClick(EventClick e) {
      if (mc.player.getUseItem().isEmpty()
              && mc.screen == null
              && Naven.skipTasks.isEmpty()
              && !NetworkUtils.isServerLag()
              && !Naven.getInstance().getModuleManager().getModule(Blink.class).isEnabled()) {
         while (this.attacks >= 1.0F) {
            this.doAttack();
            this.attacks--;
         }
      }
   }

   public Entity shouldPreAim() {
      Entity target = Aura.target;
      if (target == null) {
         List<Entity> aimTargets = this.getTargets();
         if (!aimTargets.isEmpty()) {
            target = aimTargets.get(0);
         }
      }

      return target;
   }

   public void doAttack() {
      if (!targets.isEmpty()) {
         HitResult hitResult = mc.hitResult;
         if (hitResult.getType() == Type.ENTITY) {
            EntityHitResult result = (EntityHitResult)hitResult;
            if (AntiBots.isBot(result.getEntity())) {
               ChatUtils.addChatMessage("Attacking Bot!");
               return;
            }
         }

         if (this.multi.getCurrentValue()) {
            int attacked = 0;

            for (Entity entity : targets) {
               if (RotationUtils.getDistance(entity, mc.player.getEyePosition(), RotationManager.rotations) < 3.0) {
                  this.attackEntity(entity);
                  if (++attacked >= 2) {
                     break;
                  }
               }
            }
         } else if (hitResult.getType() == Type.ENTITY) {
            EntityHitResult result = (EntityHitResult)hitResult;
            this.attackEntity(result.getEntity());
         }
      }
   }

   public void updateAttackTargets() {
      targets = this.getTargets();
   }

   public boolean isValidTarget(Entity entity) {
      if (entity == mc.player) {
         return false;
      } else if (entity instanceof LivingEntity living) {
         if (living instanceof BlinkingPlayer) {
            return false;
         } else {
            AntiBots module = (AntiBots)Naven.getInstance().getModuleManager().getModule(AntiBots.class);
            if (module == null || !module.isEnabled() || !AntiBots.isBot(entity) && !AntiBots.isBedWarsBot(entity)) {
               if (Teams.isSameTeam(living)) {
                  return false;
               } else if (FriendManager.isFriend(living)) {
                  return false;
               } else if (living.isDeadOrDying() || living.getHealth() <= 0.0F) {
                  return false;
               } else if (entity instanceof ArmorStand) {
                  return false;
               } else if (entity.isInvisible() && !this.attackInvisible.getCurrentValue()) {
                  return false;
               } else if (entity instanceof Player && !this.attackPlayer.getCurrentValue()) {
                  return false;
               } else if (!(entity instanceof Player) || !((double)entity.getBbWidth() < 0.5) && !living.isSleeping()) {
                  if ((entity instanceof Mob || entity instanceof Slime || entity instanceof Bat || entity instanceof AbstractGolem)
                          && !this.attackMobs.getCurrentValue()) {
                     return false;
                  } else if ((entity instanceof Animal || entity instanceof Squid) && !this.attackAnimals.getCurrentValue()) {
                     return false;
                  } else {
                     return entity instanceof Villager && !this.attackAnimals.getCurrentValue() ? false : !(entity instanceof Player) || !entity.isSpectator();
                  }
               } else {
                  return false;
               }
            } else {
               return false;
            }
         }
      } else {
         return false;
      }
   }

   public boolean isValidAttack(Entity entity) {
      if (!this.isValidTarget(entity)) {
         return false;
      } else if (entity instanceof LivingEntity && (float)((LivingEntity)entity).hurtTime > this.hurtTime.getCurrentValue()) {
         return false;
      } else {
         Vec3 closestPoint = RotationUtils.getClosestPoint(mc.player.getEyePosition(), entity.getBoundingBox());
         return closestPoint.distanceTo(mc.player.getEyePosition()) > (double)this.aimRange.getCurrentValue()
                 ? false
                 : RotationUtils.inFoV(entity, this.fov.getCurrentValue() / 2.0F);
      }
   }

   public void attackEntity(Entity entity) {
      this.attackTimes++;
      float currentYaw = mc.player.getYRot();
      float currentPitch = mc.player.getXRot();
      mc.player.setYRot(RotationManager.rotations.x);
      mc.player.setXRot(RotationManager.rotations.y);
      if (entity instanceof Player && !AntiBots.isBot(entity)) {
         KillSay.attackedPlayers.add(entity.getName().getString());
      }

      mc.gameMode.attack(mc.player, entity);
      mc.player.swing(InteractionHand.MAIN_HAND);

      // 添加格挡动画效果
      if (this.blockAnimation.getCurrentValue() && mc.player.getMainHandItem().getItem() instanceof SwordItem) {
         triggerBlockingAnimation();
      }

      if (this.moreParticles.getCurrentValue()) {
         mc.player.magicCrit(entity);
         mc.player.crit(entity);
      }

      // 触发Blink的FakeLag功能
      Module module = Naven.getInstance().getModuleManager().getModule(Blink.class);
      if (module != null && module.isEnabled() && module instanceof Blink) {
         Blink blinkModule = (Blink) module;
         blinkModule.triggerFakeLag();
      }

      mc.player.setYRot(currentYaw);
      mc.player.setXRot(currentPitch);
   }

   private void updateBlockingAnimation() {
      oMainHandHeight = mainHandHeight;

      ItemStack itemstack = mc.player.getMainHandItem();

      if (isBlocking) {
         mainHandHeight = 1.0F;
         if (ItemStack.matches(mainHandItem, itemstack)) {
            mainHandItem = itemstack;
         }
         return;
      }

      float f = mc.player.getAttackStrengthScale(1.0F);
      boolean flag = net.minecraftforge.client.ForgeHooksClient.shouldCauseReequipAnimation(mainHandItem, itemstack, mc.player.getInventory().selected);

      if (!flag && mainHandItem != itemstack) {
         mainHandItem = itemstack;
      }

      float targetMainHeight = !flag ? f * f * f : 0.0F;
      mainHandHeight += Mth.clamp(targetMainHeight - mainHandHeight, -0.2F, 0.2F);

      if (mainHandHeight < 0.1F) {
         mainHandItem = itemstack;
      }
   }

   private void triggerBlockingAnimation() {
      // 触发格挡动画
      isBlocking = true;

      // 设置动画持续时间（短暂的格挡效果）
      new Thread(() -> {
         try {
            Thread.sleep(100); // 持续100毫秒
            isBlocking = false;
         } catch (InterruptedException e) {
            isBlocking = false;
         }
      }).start();
   }

   // 应用1.7版本的格挡动画变换
   public void apply17BlockingTransform(PoseStack poseStack, float swingProgress) {
      int i = 1; // 右手
      poseStack.translate((double) (i * 0.56F), (double) (-0.52F), -0.72F);
      float f = Mth.sin(swingProgress * swingProgress * (float) Math.PI);
      float f1 = Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);
      poseStack.mulPose(Axis.YP.rotation((float) i * (45.0F + f * -20.0F) * (float) Math.PI / 180.0F));
      poseStack.mulPose(Axis.ZP.rotation((float) i * f1 * -20.0F * (float) Math.PI / 180.0F));
      poseStack.mulPose(Axis.XP.rotation(f1 * -80.0F * (float) Math.PI / 180.0F));
      poseStack.mulPose(Axis.YP.rotation((float) i * -45.0F * (float) Math.PI / 180.0F));
      poseStack.scale(0.9F, 0.9F, 0.9F);
      poseStack.translate(-0.2F, 0.126F, 0.2F);
      poseStack.mulPose(Axis.XP.rotation(-102.25F * (float) Math.PI / 180.0F));
      poseStack.mulPose(Axis.YP.rotation((float) i * 15.0F * (float) Math.PI / 180.0F));
      poseStack.mulPose(Axis.ZP.rotation((float) i * 80.0F * (float) Math.PI / 180.0F));
   }

   public String getLocalizedName() {
      return LanguageManager.getInstance().getLocalizedString("杀戮光环", "Kill Aura");
   }

   public String getLocalizedDescription() {
      return LanguageManager.getInstance().getLocalizedString("自动攻击实体", "Automatically attack entities");
   }
   
   private List<Entity> getTargets() {
      Stream<Entity> stream = StreamSupport.<Entity>stream(mc.level.entitiesForRendering().spliterator(), true)
              .filter(entity -> entity instanceof Entity)
              .filter(this::isValidAttack);
      List<Entity> possibleTargets = stream.collect(Collectors.toList());
      if (this.priority.isCurrentMode("范围")) {
         possibleTargets.sort(Comparator.comparingDouble(o -> (double)o.distanceTo(mc.player)));
      } else if (this.priority.isCurrentMode("角度")) {
         possibleTargets.sort(
                 Comparator.comparingDouble(o -> (double)RotationUtils.getDistanceBetweenAngles(RotationManager.rotations.x, RotationUtils.getRotations(o).x))
         );
      } else if (this.priority.isCurrentMode("生命值")) {
         possibleTargets.sort(Comparator.comparingDouble(o -> o instanceof LivingEntity living ? (double)living.getHealth() : 0.0));
      }

      if (this.preferBaby.getCurrentValue() && possibleTargets.stream().anyMatch(entity -> entity instanceof LivingEntity && ((LivingEntity)entity).isBaby())) {
         possibleTargets.removeIf(entity -> !(entity instanceof LivingEntity) || !((LivingEntity)entity).isBaby());
      }

      possibleTargets.sort(Comparator.comparing(o -> o instanceof EndCrystal ? 0 : 1));
      return this.infSwitch.getCurrentValue()
              ? possibleTargets
              : possibleTargets.subList(0, (int)Math.min((float)possibleTargets.size(), this.switchSize.getCurrentValue()));
   }
}