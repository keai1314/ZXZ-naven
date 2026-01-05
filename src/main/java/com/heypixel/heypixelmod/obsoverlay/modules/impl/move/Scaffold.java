package com.heypixel.heypixelmod.obsoverlay.modules.impl.move;

import com.heypixel.heypixelmod.obsoverlay.annotations.FlowExclude;
import com.heypixel.heypixelmod.obsoverlay.annotations.ParameterObfuscationExclude;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventClick;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRunTicks;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRender2D;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventUpdateFoV;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventUpdateHeldItem;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.utils.FallingPlayer;
import com.heypixel.heypixelmod.obsoverlay.utils.RenderUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.InventoryUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.MathUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.MoveUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.NetworkUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.PlayerUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.RayTraceUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.Vector2f;
import com.heypixel.heypixelmod.obsoverlay.utils.rotation.RotationUtils;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.ModeValue;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.Fonts;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.text.CustomTextRenderer;
import com.heypixel.heypixelmod.obsoverlay.ui.LanguageManager;
import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.mojang.blaze3d.platform.InputConstants;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import com.heypixel.heypixelmod.obsoverlay.utils.MathHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.FungusBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import org.apache.commons.lang3.RandomUtils;

@ModuleInfo(
        name = "自动搭路",
        description = "自动在你脚下放置方块",
        category = Category.MOVEMENT
)
public class Scaffold extends Module {
   public static final List<Block> blacklistedBlocks = Arrays.asList(
           Blocks.AIR,
           Blocks.WATER,
           Blocks.LAVA,
           Blocks.ENCHANTING_TABLE,
           Blocks.GLASS_PANE,
           Blocks.GLASS_PANE,
           Blocks.IRON_BARS,
           Blocks.SNOW,
           Blocks.COAL_ORE,
           Blocks.DIAMOND_ORE,
           Blocks.EMERALD_ORE,
           Blocks.CHEST,
           Blocks.TRAPPED_CHEST,
           Blocks.TORCH,
           Blocks.ANVIL,
           Blocks.TRAPPED_CHEST,
           Blocks.NOTE_BLOCK,
           Blocks.JUKEBOX,
           Blocks.TNT,
           Blocks.GOLD_ORE,
           Blocks.IRON_ORE,
           Blocks.LAPIS_ORE,
           Blocks.STONE_PRESSURE_PLATE,
           Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE,
           Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE,
           Blocks.STONE_BUTTON,
           Blocks.LEVER,
           Blocks.TALL_GRASS,
           Blocks.TRIPWIRE,
           Blocks.TRIPWIRE_HOOK,
           Blocks.RAIL,
           Blocks.CORNFLOWER,
           Blocks.RED_MUSHROOM,
           Blocks.BROWN_MUSHROOM,
           Blocks.VINE,
           Blocks.SUNFLOWER,
           Blocks.LADDER,
           Blocks.FURNACE,
           Blocks.SAND,
           Blocks.CACTUS,
           Blocks.DISPENSER,
           Blocks.DROPPER,
           Blocks.CRAFTING_TABLE,
           Blocks.COBWEB,
           Blocks.PUMPKIN,
           Blocks.COBBLESTONE_WALL,
           Blocks.OAK_FENCE,
           Blocks.REDSTONE_TORCH,
           Blocks.FLOWER_POT
   );
   public Vector2f correctRotation = new Vector2f();
   public Vector2f rots = new Vector2f();
   public Vector2f lastRots = new Vector2f();
   private int offGroundTicks = 0;
   public ModeValue mode = ValueBuilder.create(this, "模式").setDefaultModeIndex(0).setModes("普通", "Telly", "保持Y").build().getModeValue();
   // Rotation speed in degrees per tick for yaw interpolation (0~180). 180 equals previous behavior.
   public FloatValue rotationSpeed = ValueBuilder.create(this, "转头速度")
           .setDefaultFloatValue(180.0F)
           .setMinFloatValue(0.0F)
           .setMaxFloatValue(180.0F)
           .setFloatStep(1.0F)
           .build()
           .getFloatValue();
   
   // Yaw转动速度（度/刻），用于精确控制Yaw转动
   public FloatValue yawSpeed = ValueBuilder.create(this, "Yaw速度")
           .setDefaultFloatValue(90.0F)
           .setMinFloatValue(0.0F)
           .setMaxFloatValue(180.0F)
           .setFloatStep(1.0F)
           .build()
           .getFloatValue();
   
   // 减少Yaw转动选项
   public BooleanValue reduceYawRotation = ValueBuilder.create(this, "减少Yaw转动")
           .setDefaultBooleanValue(false)
           .build()
           .getBooleanValue();
   
   // Delay before allowing actions after jump-and-aim in Telly Bridge (milliseconds)
   public FloatValue delayPlaceMs = ValueBuilder.create(this, "放置延迟(ms)")
           .setDefaultFloatValue(150.0F)
           .setMinFloatValue(0.0F)
           .setMaxFloatValue(1000.0F)
           .setFloatStep(10.0F)
           .setVisibility(() -> this.mode.isCurrentMode("Telly"))
           .build()
           .getFloatValue();
   
   // Telly Bridge: ticks to wait after leaving ground before placing blocks
   public FloatValue tellyTick = ValueBuilder.create(this, "TellyTick")
           .setDefaultFloatValue(1.0F)
           .setMinFloatValue(0.0F)
           .setMaxFloatValue(5.0F)
           .setFloatStep(0.5F)
           .setVisibility(() -> this.mode.isCurrentMode("Telly"))
           .build()
           .getFloatValue();
   // When enabled, do not perform faceforward rotation while jump key is held (Telly Bridge only)
   public BooleanValue noFlickWhenJump = ValueBuilder.create(this, "在跳跃时不点击")
           .setDefaultBooleanValue(true)
           .setVisibility(() -> this.mode.isCurrentMode("Telly"))
           .build()
           .getBooleanValue();
   public BooleanValue eagle = ValueBuilder.create(this, "自动蹲下")
           .setDefaultBooleanValue(true)
           .setVisibility(() -> this.mode.isCurrentMode("普通"))
           .build()
           .getBooleanValue();
   public BooleanValue sneak = ValueBuilder.create(this, "潜行").setDefaultBooleanValue(true).build().getBooleanValue();
   public BooleanValue snap = ValueBuilder.create(this, "快照")
           .setDefaultBooleanValue(true)
           .setVisibility(() -> this.mode.isCurrentMode("普通"))
           .build()
           .getBooleanValue();
   public BooleanValue hideSnap = ValueBuilder.create(this, "隐藏快照转头")
           .setDefaultBooleanValue(true)
           .setVisibility(() -> this.mode.isCurrentMode("普通") && this.snap.getCurrentValue())
           .build()
           .getBooleanValue();
   public BooleanValue renderItemSpoof = ValueBuilder.create(this, "渲染物品欺骗").setDefaultBooleanValue(true).build().getBooleanValue();
   public BooleanValue keepFoV = ValueBuilder.create(this, "保持Y").setDefaultBooleanValue(true).build().getBooleanValue();
   FloatValue fov = ValueBuilder.create(this, "角度")
           .setDefaultFloatValue(1.15F)
           .setMaxFloatValue(2.0F)
           .setMinFloatValue(1.0F)
           .setFloatStep(0.05F)
           .setVisibility(() -> this.keepFoV.getCurrentValue())
           .build()
           .getFloatValue();
   int oldSlot;
   private Scaffold.BlockPosWithFacing pos;
   private int lastSneakTicks;
   public int baseY = -1;
   private long tellyDelayStartMs = -1L;
   // BlockCount HUD options
   public BooleanValue blockCountDisplay = ValueBuilder.create(this, "方块数量显示").setDefaultBooleanValue(true).build().getBooleanValue();
   public FloatValue blockCountX = ValueBuilder.create(this, "方块数量显示X坐标").setDefaultFloatValue(8.0F).setMinFloatValue(0.0F).setMaxFloatValue(10000.0F).setFloatStep(1.0F).build().getFloatValue();
   public FloatValue blockCountY = ValueBuilder.create(this, "方块数量显示Y坐标").setDefaultFloatValue(45.0F).setMinFloatValue(0.0F).setMaxFloatValue(10000.0F).setFloatStep(1.0F).build().getFloatValue();
   public FloatValue blockCountScale = ValueBuilder.create(this, "方块数量显示大小").setDefaultFloatValue(0.6F).setMinFloatValue(0.4F).setMaxFloatValue(2.0F).setFloatStep(0.05F).build().getFloatValue();
   // 添加透明度设置
   public FloatValue blockCountAlpha = ValueBuilder.create(this, "方块数量显示透明度")
           .setDefaultFloatValue(0.8F)
           .setMinFloatValue(0.0F)
           .setMaxFloatValue(1.0F)
           .setFloatStep(0.05F)
           .build()
           .getFloatValue();
   public BooleanValue showBlockCountBackground = ValueBuilder.create(this, "显示方块数量背景")
           .setDefaultBooleanValue(true)
           .build()
           .getBooleanValue();
   // 添加圆滑度设置
   public FloatValue blockCountRoundness = ValueBuilder.create(this, "方块数量显示圆滑度")
           .setDefaultFloatValue(0.0F)
           .setMinFloatValue(0.0F)
           .setMaxFloatValue(15.0F)
           .setFloatStep(0.5F)
           .build()
           .getFloatValue();
   
   // Debug选项：当玩家3D移动速度超过12Bps时在聊天栏报告
   public BooleanValue debugSpeed = ValueBuilder.create(this, "速度调试")
           .setDefaultBooleanValue(false)
           .build()
           .getBooleanValue();
   
   // 当前玩家3D移动速度（Bps）
   private double currentSpeed = 0.0;
   
   // 上一帧玩家3D移动速度（Bps）
   private double lastSpeed = 0.0;
   
   // 当前玩家加速度（Bps²）
   private double currentAcceleration = 0.0;
   
   // 速度显示大小调整
   public FloatValue speedDisplayScale = ValueBuilder.create(this, "速度显示大小")
           .setDefaultFloatValue(0.8F)
           .setMinFloatValue(0.0F)
           .setMaxFloatValue(1.5F)
           .setFloatStep(0.1F)
           .setVisibility(() -> this.debugSpeed.getCurrentValue())
           .build()
           .getFloatValue();
   
   // 加速度阈值（Bps²），超过此值触发自救
   public FloatValue accelerationThreshold = ValueBuilder.create(this, "加速度阈值")
           .setDefaultFloatValue(20.0F)
           .setMinFloatValue(0.0F)
           .setMaxFloatValue(50.0F)
           .setFloatStep(0.5F)
           .build()
           .getFloatValue();
   
   // 超时检测阈值滑动条（tick）
   public FloatValue stuckTimeoutTicks = ValueBuilder.create(this, "卡住超时(tick)")
           .setDefaultFloatValue(15.0F)
           .setMinFloatValue(1.0F)
           .setMaxFloatValue(30.0F)
           .setFloatStep(1.0F)
           .build()
           .getFloatValue();
   
   // 超时检测触发关闭后的空档期（tick）
   public FloatValue stuckCooldownTicks = ValueBuilder.create(this, "卡住空档期(tick)")
           .setDefaultFloatValue(20.0F)
           .setMinFloatValue(0.0F)
           .setMaxFloatValue(20.0F)
           .setFloatStep(1.0F)
           .build()
           .getFloatValue();
   
   // 上次释放卡住的时间（tick）
   private long lastReleaseTick = 0;
   
   // Blink释放后的空档期（tick）
   public FloatValue blinkReleaseCooldownTicks = ValueBuilder.create(this, "Blink释放空档期(tick)")
           .setDefaultFloatValue(15.0F)
           .setMinFloatValue(0.0F)
           .setMaxFloatValue(50.0F)
           .setFloatStep(5.0F)
           .build()
           .getFloatValue();
   
   // 上次释放Blink包的时间（tick）
   private long lastBlinkReleaseTick = 0;
   
   // 上次选择的方块槽位，用于避免来回切换
   private int lastSelectedSlot = -1;
   
   // 渲染物品欺骗相关：保存开启时玩家手持物品的槽位
   private int spoofItemSlot = -1;
   // 保存开启时玩家手持的物品
   private ItemStack spoofItem = null;

   // 自救时忽略放置条件选项
   public BooleanValue ignoreConditionsOnSelfRescue = ValueBuilder.create(this, "自救时忽略放置条件")
           .setDefaultBooleanValue(false)
           .build()
           .getBooleanValue();
   
   // 自救功能开关
   public BooleanValue selfRescue = ValueBuilder.create(this, "自救")
           .setDefaultBooleanValue(false)
           .build()
           .getBooleanValue();
   
   // SkipTick开关
   public BooleanValue skipTick = ValueBuilder.create(this, "SkipTick")
           .setDefaultBooleanValue(false)
           .build()
           .getBooleanValue();
   

   public static boolean isValidStack(ItemStack stack) {
      if (stack == null || !(stack.getItem() instanceof BlockItem) || stack.getCount() <= 1) {
         return false;
      } else if (!InventoryUtils.isItemValid(stack)) {
         return false;
      } else {
         String string = stack.getDisplayName().getString();
         if (string.contains("Click") || string.contains("点击")) {
            return false;
         } else if (stack.getItem() instanceof ItemNameBlockItem) {
            return false;
         } else {
            Block block = ((BlockItem)stack.getItem()).getBlock();
            if (block instanceof FlowerBlock) {
               return false;
            } else if (block instanceof BushBlock) {
               return false;
            } else if (block instanceof FungusBlock) {
               return false;
            } else if (block instanceof CropBlock) {
               return false;
            } else {
               return block instanceof SlabBlock ? false : !blacklistedBlocks.contains(block);
            }
         }
      }
   }

   @EventTarget
   public void onRender2D(EventRender2D e) {
      // 方块数量显示逻辑
      if (this.blockCountDisplay.getCurrentValue() && mc.player != null && mc.level != null && this.isEnabled()) {
         // 选择显示的方块物品：优先主手可放置方块，否则取热键栏第一个可用方块
         ItemStack displayStack = mc.player.getMainHandItem();
         if (!(displayStack.getItem() instanceof BlockItem) || !isValidStack(displayStack)) {
            displayStack = null;
            for (int i = 0; i < 9; i++) {
               ItemStack s = mc.player.getInventory().getItem(i);
               if (s.getItem() instanceof BlockItem && isValidStack(s)) { displayStack = s; break; }
            }
         }

         int totalBlocks = countPlaceableBlocks();
         float x = this.blockCountX.getCurrentValue();
         float y = this.blockCountY.getCurrentValue();
         float roundnessValue = this.blockCountRoundness.getCurrentValue();

         // 使用与NewHUD中"Naven"水印相同的字体
         CustomTextRenderer font = Fonts.opensans;
         String txt = String.valueOf(totalBlocks);
         double scale = this.blockCountScale.getCurrentValue();
         float textW = font.getWidth(txt, scale);
         float textH = (float) font.getHeight(true, scale);
         float iconSize = 16.0F;
         float padding = 4.0F;
         float width = padding + iconSize + 6.0F + textW + padding;
         float height = padding + Math.max(iconSize, textH) + padding;

         // 绘制背景，使用与Inventory.java类似的透明度系统
         if (showBlockCountBackground.getCurrentValue()) {
            float bgAlpha = blockCountAlpha.getCurrentValue();
            int bgColor = new Color(0, 0, 0, (int) (255 * bgAlpha)).getRGB();
            if (roundnessValue > 0) {
               RenderUtils.drawRoundedRect(e.getStack(), x, y, width, height, roundnessValue, bgColor);
            } else {
               RenderUtils.fillBound(e.getStack(), x, y, width, height, bgColor);
            }
         }

         // 渲染物品图标
         if (displayStack != null) {
            int itemX = (int)(x + padding);
            int itemY = (int)(y + padding + (Math.max(iconSize, textH) - iconSize) / 2.0F);
            e.getGuiGraphics().renderItem(displayStack, itemX, itemY);
            e.getGuiGraphics().renderItemDecorations(mc.font, displayStack, itemX, itemY);
         }

         // 渲染数量文本，使用与NewHUD中"Naven"水印相同的字体和样式
         float textX = x + padding + iconSize + 6.0F;
         float textY = y + padding + (Math.max(iconSize, textH) - textH) / 2.0F + 1.0F;
         font.render(e.getStack(), txt, textX, textY, Color.WHITE, true, scale);
      }
      
      // 速度显示逻辑（仅当debugSpeed启用时显示）
      if (this.debugSpeed.getCurrentValue() && mc.player != null) {
         // 获取屏幕中心（准心位置）
         int screenWidth = mc.getWindow().getGuiScaledWidth();
         int screenHeight = mc.getWindow().getGuiScaledHeight();
         int centerX = screenWidth / 2;
         int centerY = screenHeight / 2;
         
         // 计算速度和加速度文本
         String speedText = String.format("Speed: %.2f Bps", this.currentSpeed);
         String accelerationText = String.format("Accel: %.2f Bps²", this.currentAcceleration);
         
         // 使用与方块数量显示相同的字体
         CustomTextRenderer font = Fonts.opensans;
         float scale = this.speedDisplayScale.getCurrentValue();
         float speedTextWidth = font.getWidth(speedText, scale);
         float accelerationTextWidth = font.getWidth(accelerationText, scale);
         float textWidth = Math.max(speedTextWidth, accelerationTextWidth);
         float textHeight = (float) font.getHeight(true, scale);
         
         // 设置毛玻璃效果的半透明圆角矩形参数
         float padding = 8.0F;
         float rectWidth = textWidth + padding * 2;
         float rectHeight = (textHeight * 2) + padding * 3; // 两个文本行，额外增加padding
         float x = centerX - rectWidth / 2;
         float y = centerY + 20; // 准心下方20像素
         float roundness = 8.0F;
         
         // 绘制毛玻璃效果的半透明圆角矩形
         int bgColor = new Color(0, 0, 0, 128).getRGB(); // 半透明黑色背景
         RenderUtils.drawRoundedRect(e.getStack(), x, y, rectWidth, rectHeight, roundness, bgColor);
         
         // 绘制速度文本
         float textX = x + padding + (textWidth - speedTextWidth) / 2; // 居中显示
         float textY = y + padding + 1;
         font.render(e.getStack(), speedText, textX, textY, Color.WHITE, true, scale);
         
         // 绘制加速度文本
         textX = x + padding + (textWidth - accelerationTextWidth) / 2; // 居中显示
         textY = y + padding + textHeight + 5; // 第二行，增加间距
         font.render(e.getStack(), accelerationText, textX, textY, Color.WHITE, true, scale);
      }
   }

   private int countPlaceableBlocks() {
      int total = 0;
      // 统计背包中可放置的方块数量（包含热键栏与主物品栏）
      for (int i = 0; i < mc.player.getInventory().items.size(); i++) {
         ItemStack s = mc.player.getInventory().items.get(i);
         if (s != null && s.getItem() instanceof BlockItem && isValidStack(s)) {
            total += s.getCount();
         }
      }
      return total;
   }

   public static boolean isOnBlockEdge(float sensitivity) {
      return !mc.level
              .getCollisions(mc.player, mc.player.getBoundingBox().move(0.0, -0.5, 0.0).inflate((double)(-sensitivity), 0.0, (double)(-sensitivity)))
              .iterator()
              .hasNext();
   }

   @EventTarget
   public void onFoV(EventUpdateFoV e) {
      if (this.keepFoV.getCurrentValue() && MoveUtils.isMoving()) {
         e.setFov(this.fov.getCurrentValue() + (float)PlayerUtils.getMoveSpeedEffectAmplifier() * 0.13F);
      }
   }
   
   @EventTarget
   public void onUpdateHeldItem(EventUpdateHeldItem e) {
      // 只有当渲染物品欺骗功能启用时才生效
      if (this.renderItemSpoof.getCurrentValue() && e.getHand() == InteractionHand.MAIN_HAND) {
         // 如果保存了欺骗物品，使用它进行渲染
         if (this.spoofItem != null) {
            e.setItem(this.spoofItem);
         }
      }
   }

   @Override
   public void onEnable() {
      if (mc.player != null) {
         this.oldSlot = mc.player.getInventory().selected;
         this.rots.set(mc.player.getYRot() - 180.0F, mc.player.getXRot());
         this.lastRots.set(mc.player.yRotO - 180.0F, mc.player.xRotO);
         this.pos = null;
         this.baseY = 10000;
         this.tellyDelayStartMs = -1L;
         
         // 保存玩家当前手持物品的槽位和物品用于渲染欺骗
         this.spoofItemSlot = mc.player.getInventory().selected;
         this.spoofItem = mc.player.getInventory().getItem(this.spoofItemSlot).copy();
      }
      

   }

   @Override
   public void onDisable() {
      boolean isHoldingJump = InputConstants.isKeyDown(mc.getWindow().getWindow(), mc.options.keyJump.getKey().getValue());
      boolean isHoldingShift = InputConstants.isKeyDown(mc.getWindow().getWindow(), mc.options.keyShift.getKey().getValue());
      mc.options.keyJump.setDown(isHoldingJump);
      mc.options.keyShift.setDown(isHoldingShift);
      mc.options.keyUse.setDown(false);
      mc.player.getInventory().selected = this.oldSlot;
      this.tellyDelayStartMs = -1L;
      
      // 重置渲染欺骗相关变量
      this.spoofItemSlot = -1;
      this.spoofItem = null;
      
      // 安全释放玩家的卡住状态
      PlayerUtils.playerStuckTicks = 0;
   }

   @EventTarget(1)
   public void onEventEarlyTick(EventRunTicks e) {
      if (e.getType() == EventType.PRE && mc.screen == null && mc.player != null) {
         // 速度调试：检测玩家3D移动速度
         Vec3 deltaMovement = mc.player.getDeltaMovement();
         // 计算3D移动速度（Bps）
         double speed3D = deltaMovement.length() * 20.0; // 转换为每秒方块数
         // 保存当前速度
         this.currentSpeed = speed3D;
         
         // 计算加速度（Bps²）
         this.currentAcceleration = Math.abs(speed3D - this.lastSpeed);
         // 更新上一帧速度
         this.lastSpeed = speed3D;
         
         // 获取当前游戏刻
         long currentTick = mc.level.getGameTime();
         

         // 自救释放逻辑：检查释放条件
         if (PlayerUtils.playerStuckTicks > 0) {
            
            // 检查原始解除卡住条件，使用滑动条配置的超时阈值
            if (PlayerUtils.playerStuckTicks > this.stuckTimeoutTicks.getCurrentValue() || PlayerUtils.hasBlockUnderFeet() || PlayerUtils.isBlockBelowInRange()) {
               // 释放玩家，重置计数器
               PlayerUtils.playerStuckTicks = 0;
               // 记录上次释放时间
               this.lastReleaseTick = currentTick;
            } else {
               // 在Scaffold模块中递增卡住计数，确保它在所有情况下都能被正确处理
               PlayerUtils.playerStuckTicks++;
            }
         } else {
            // 检查空档期：只有超过空档期才允许再次卡住
            long cooldownTicks = (long)this.stuckCooldownTicks.getCurrentValue();
            if (currentTick - this.lastReleaseTick > cooldownTicks) {
               // 自救功能主逻辑
               boolean shouldTriggerSelfRescue = false;
               
               // 检查自救开关是否开启
               if (this.selfRescue.getCurrentValue()) {
                  // 检查是否处于掉落状态且BPS超过13
                  if (this.isFalling() && speed3D > 13.0) {
                     // 强制更新baseY
                     this.baseY = (int)Math.floor(mc.player.getY()) - 1;
                     
                     // 检查是否可以放置方块
                     boolean canPlace = this.canPlaceBlocks();
                     
                     if (canPlace) {
                        shouldTriggerSelfRescue = true;
                     }
                  }
               }
               
               // 原有速度检测逻辑
               boolean shouldTriggerSpeedRescue = speed3D > 14.0 || this.currentAcceleration > this.accelerationThreshold.getCurrentValue();
               
               if (shouldTriggerSelfRescue || shouldTriggerSpeedRescue) {
                  // 强制更新baseY
                  this.baseY = (int)Math.floor(mc.player.getY()) - 1;
                  
                  // 检查是否可以放置方块
                  boolean canPlace = this.canPlaceBlocks();
                  
                  if (canPlace) {
                     this.triggerPlayerStuck();
                  }
               }
            }
         }
         
         // 物品自动切换：从快捷栏中查找合适的方块
         int slotID = -1;
         int originalSlot = mc.player.getInventory().selected;

         // 智能物品选择：优先选择数量较多的方块，相同数量时保持上次选择
         int maxCount = 0;
         for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.getItem() instanceof BlockItem && isValidStack(stack)) {
               int stackCount = stack.getCount();
               // 如果当前方块数量更多，或者数量相同且是上次选择的方块，则更新选择
               if (stackCount > maxCount || 
                   (stackCount == maxCount && i == this.lastSelectedSlot)) {
                  maxCount = stackCount;
                  slotID = i;
               }
            }
         }

         if (mc.player.onGround()) {
            this.offGroundTicks = 0;
         } else {
            this.offGroundTicks++;
         }

         // 切换到找到的方块物品
         if (slotID != -1 && mc.player.getInventory().selected != slotID) {
            mc.player.getInventory().selected = slotID;
            // 记录本次选择的槽位，用于下次选择时保持稳定性
            this.lastSelectedSlot = slotID;
         }

         boolean isHoldingJump = InputConstants.isKeyDown(mc.getWindow().getWindow(), mc.options.keyJump.getKey().getValue());
         if (this.baseY == -1
                 || this.baseY > (int)Math.floor(mc.player.getY()) - 1
                 || mc.player.onGround()
                 || !PlayerUtils.movementInput()
                 || isHoldingJump
                 || this.mode.isCurrentMode("普通")) {
            this.baseY = (int)Math.floor(mc.player.getY()) - 1;
         }

         this.getBlockPos();
        if (this.pos != null) {
           this.correctRotation = this.getPlayerYawRotation();
           
           if (this.mode.isCurrentMode("普通") && this.snap.getCurrentValue()) {
              this.rots.setX(this.correctRotation.getX());
           } else {
              float currentYaw = this.rots.getX();
              float targetYaw = this.correctRotation.getX();
              float yawDiff = Math.abs(MathHelper.wrapDegrees(targetYaw - currentYaw));
              
              // 减少Yaw转动选项：如果启用，只在必要时转动
              if (this.reduceYawRotation.getCurrentValue() && yawDiff < 5.0F) {
                  // 差值很小，保持当前Yaw，不转动
                  return;
              }
              
              // 使用YawSpeed进行转头
              this.rots.setX(RotationUtils.rotateToYaw(this.yawSpeed.getCurrentValue(), this.rots.getX(), this.correctRotation.getX()));
           }

           this.rots.setY(this.correctRotation.getY());
        }

         // Telly Bridge: start delay timer once we jumped and have a target block; reset otherwise
         if (this.mode.isCurrentMode("Telly")) {
            if (this.offGroundTicks >= this.tellyTick.getCurrentValue() && this.pos != null) {
               if (this.tellyDelayStartMs < 0L) {
                  this.tellyDelayStartMs = System.currentTimeMillis();
               }
            } else {
               this.tellyDelayStartMs = -1L;
            }
         } else {
            this.tellyDelayStartMs = -1L;
         }

         if (this.sneak.getCurrentValue()) {
            this.lastSneakTicks++;
            System.out.println(this.lastSneakTicks);
            if (this.lastSneakTicks == 18) {
               if (mc.player.isSprinting()) {
                  mc.options.keySprint.setDown(false);
                  mc.player.setSprinting(false);
               }

               mc.options.keyShift.setDown(true);
            } else if (this.lastSneakTicks >= 21) {
               mc.options.keyShift.setDown(false);
               this.lastSneakTicks = 0;
            }
         }

         if (this.mode.isCurrentMode("Telly")) {
            mc.options.keyJump.setDown(PlayerUtils.movementInput() || isHoldingJump);
            if (this.offGroundTicks < this.tellyTick.getCurrentValue() && PlayerUtils.movementInput() && !(this.noFlickWhenJump.getCurrentValue() && isHoldingJump)) {
               float currentYaw = this.rots.getX();
               float targetYaw = mc.player.getYRot();
               float yawDiff = Math.abs(MathHelper.wrapDegrees(targetYaw - currentYaw));
               
               // 减少Yaw转动选项：如果启用，只在必要时转动
               if (this.reduceYawRotation.getCurrentValue() && yawDiff < 5.0F) {
                   // 差值很小，保持当前Yaw，不转动
                   return;
               }
               
               // 使用YawSpeed进行转头
               this.rots.setX(RotationUtils.rotateToYaw(this.yawSpeed.getCurrentValue(), this.rots.getX(), mc.player.getYRot()));
               this.lastRots.set(this.rots.getX(), this.rots.getY());
               return;
            }
         } else if (this.mode.isCurrentMode("保持Y")) {
            mc.options.keyJump.setDown(PlayerUtils.movementInput() || isHoldingJump);
         } else {
            if (this.eagle.getCurrentValue()) {
               mc.options.keyShift.setDown(mc.player.onGround() && isOnBlockEdge(0.3F));
            }

            if (this.snap.getCurrentValue() && !isHoldingJump) {
               this.doSnap();
            }
         }

         this.lastRots.set(this.rots.getX(), this.rots.getY());
      }
   }

   private void doSnap() {
      boolean shouldPlaceBlock = false;
      HitResult objectPosition = RayTraceUtils.rayCast(1.0F, this.rots);
      if (objectPosition.getType() == Type.BLOCK) {
         BlockHitResult position = (BlockHitResult)objectPosition;
         if (position.getBlockPos().equals(this.pos) && position.getDirection() != Direction.UP) {
            shouldPlaceBlock = true;
         }
      }

      if (!shouldPlaceBlock) {
         // 减少Yaw转动选项：如果启用，只在必要时转动
         if (!this.reduceYawRotation.getCurrentValue()) {
             this.rots.setX(mc.player.getYRot() + RandomUtils.nextFloat(0.0F, 0.5F) - 0.25F);
         }
         // 否则保持当前Yaw，不转动
      }
   }

   @EventTarget
   public void onClick(EventClick e) {
      e.setCancelled(true);
      if (mc.screen == null && mc.player != null && this.pos != null && (!this.mode.isCurrentMode("Telly") || (this.offGroundTicks >= this.tellyTick.getCurrentValue() && this.tellyDelayElapsed()))) {
         if (!this.checkPlace(this.pos)) {
            return;
         }

         this.placeBlock();
      }
   }

   private boolean checkPlace(Scaffold.BlockPosWithFacing data) {
      Vec3 center = new Vec3((double)data.position.getX() + 0.5, (double)((float)data.position.getY() + 0.5F), (double)data.position.getZ() + 0.5);
      Vec3 hit = center.add(
              new Vec3((double)data.facing.getNormal().getX() * 0.5, (double)data.facing.getNormal().getY() * 0.5, (double)data.facing.getNormal().getZ() * 0.5)
      );
      Vec3 relevant = hit.subtract(mc.player.getEyePosition());
      
      // 距离限制：始终保持4.5格的距离限制，确保放置的合理性
      boolean distanceCheck = relevant.lengthSqr() <= 20.25;
      
      // 方向向量点积检查
      boolean directionCheck = relevant.normalize().dot(Vec3.atLowerCornerOf(data.facing.getNormal().multiply(-1)).normalize()) >= 0.0;
      
      // 如果是自救且启用了忽略条件选项，保留距离和方向条件
      if (PlayerUtils.playerStuckTicks > 0 && this.ignoreConditionsOnSelfRescue.getCurrentValue()) {
         return distanceCheck && directionCheck;
      }
      
      return distanceCheck && directionCheck;
   }

   private void placeBlock() {
      if (this.pos != null && isValidStack(mc.player.getMainHandItem())) {
         Direction sbFace = this.pos.facing();
         boolean isHoldingJump = InputConstants.isKeyDown(mc.getWindow().getWindow(), mc.options.keyJump.getKey().getValue());
         
         // 检查放置条件：如果是自救且启用了忽略条件选项，则只保留基础条件和方向向量点积检查
         boolean shouldIgnoreConditions = PlayerUtils.playerStuckTicks > 0 && this.ignoreConditionsOnSelfRescue.getCurrentValue();
         
         // 基础条件：sbFace不为null
         if (sbFace != null) {
            // 始终进行方向向量点积检查，避免触发GrimAC的RotationPlace检查
            boolean directionCheck = this.checkPlace(this.pos);
            
            // 如果勾选了忽略条件选项，只需要方向检查通过即可
            boolean canPlace = (shouldIgnoreConditions && directionCheck) || 
                    (!shouldIgnoreConditions && 
                     (sbFace != Direction.UP || mc.player.onGround() || !PlayerUtils.movementInput() || isHoldingJump || this.mode.isCurrentMode("Normal")) && 
                     this.shouldBuild() && 
                     (!this.mode.isCurrentMode("Telly") || this.tellyDelayElapsed()));
            

                     if (canPlace) {
               // 保存当前状态，用于SkipTick任务
               final Scaffold.BlockPosWithFacing currentPos = this.pos;
               final Direction currentFace = sbFace;
               
               // 检查是否已经在当前tick内添加过放置任务，避免触发MultiPlace检查
               boolean isSkipTickActive = this.skipTick.getCurrentValue() && PlayerUtils.playerStuckTicks > 0;
               
               // 保存当前位置，避免后续逻辑修改导致的问题
               final Scaffold.BlockPosWithFacing originalPos = this.pos;
               
               if (isSkipTickActive && Naven.skipTasks.isEmpty()) {
                  // 只在自救且队列为空时使用SkipTick机制，确保每个tick最多一个任务
                  // 立即清空pos，避免重复触发
                  this.pos = null;
                  
                  Naven.skipTasks.add(() -> {
                     // 执行放置操作
                     InteractionResult result = mc.gameMode
                             .useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(getVec3(originalPos.position(), originalPos.facing()), originalPos.facing(), originalPos.position(), false));
                     
                     if (result == InteractionResult.SUCCESS) {
                        mc.player.swing(InteractionHand.MAIN_HAND);
                     }
                  });
               } else {
                  // 正常执行放置操作
                  InteractionResult result = mc.gameMode
                          .useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(getVec3(this.pos.position(), sbFace), sbFace, this.pos.position(), false));
                  
                  if (result == InteractionResult.SUCCESS) {
                     mc.player.swing(InteractionHand.MAIN_HAND);
                     this.pos = null;
                  }
               }
            }
         }
      }
   }

   private boolean tellyDelayElapsed() {
      if (!this.mode.isCurrentMode("Telly")) return true;
      float needMs = this.delayPlaceMs.getCurrentValue();
      if (needMs <= 0.0F) return true;
      if (this.offGroundTicks < 1) return false;
      if (this.tellyDelayStartMs < 0L) return false;
      return (System.currentTimeMillis() - this.tellyDelayStartMs) >= (long)needMs;
   }

   @FlowExclude
   @ParameterObfuscationExclude
   private Vector2f getPlayerYawRotation() {
      return mc.player != null && this.pos != null
              ? new Vector2f(RotationUtils.getRotations(this.pos.position(), 0.0F).getYaw(), RotationUtils.getRotations(this.pos.position(), 0.0F).getPitch())
              : new Vector2f(0.0F, 0.0F);
   }
   
   @FlowExclude
   @ParameterObfuscationExclude
   private Vector2f getPlayerYawRotation(BlockPos pos) {
      if (mc.player == null || pos == null) {
         return new Vector2f(0.0F, 0.0F);
      }
      
      // 计算准确的旋转角度，确保视线能命中放置位置
      // 使用方块中心作为目标点，确保视线能准确命中
      Vec3 targetVec = new Vec3(
          pos.getX() + 0.5,
          pos.getY() + 0.5,
          pos.getZ() + 0.5
      );
      
      // 使用RotationUtils计算旋转角度
      var rotation = RotationUtils.getRotations(pos, 0.0F);
      return new Vector2f(rotation.getYaw(), rotation.getPitch());
   }

   private boolean shouldBuild() {
      BlockPos playerPos = BlockPos.containing(mc.player.getX(), mc.player.getY() - 0.5, mc.player.getZ());
      return mc.level.isEmptyBlock(playerPos) && isValidStack(mc.player.getMainHandItem());
   }

   public String getLocalizedName() {
      return LanguageManager.getInstance().getLocalizedString("自动搭路", "Scaffold");
   }

   public String getLocalizedDescription() {
      return LanguageManager.getInstance().getLocalizedString("自动在你脚下放置方块", "Automatically place blocks under your feet");
   }

   @FlowExclude
   @ParameterObfuscationExclude
   private void getBlockPos() {
      // 获取玩家当前位置和移动方向
      Vec3 playerPos = mc.player.getEyePosition();
      Vec3 baseVec = playerPos.add(mc.player.getDeltaMovement().multiply(2.0, 2.0, 2.0));
      
      // 处理掉落情况
      if (mc.player.getDeltaMovement().y < 0.01) {
         FallingPlayer fallingPlayer = new FallingPlayer(mc.player);
         fallingPlayer.calculate(2);
         baseVec = new Vec3(baseVec.x, Math.max(fallingPlayer.y + (double)mc.player.getEyeHeight(), baseVec.y), baseVec.z);
      }

      BlockPos base = BlockPos.containing(baseVec.x, (double)((float)this.baseY + 0.1F), baseVec.z);
      int baseX = base.getX();
      int baseZ = base.getZ();
      
      // 检查基础位置是否可站立
      if (!mc.level.getBlockState(base).entityCanStandOn(mc.level, base, mc.player)) {
         // 基础位置不可站立，寻找可放置位置
         if (!this.checkBlock(baseVec, base)) {
            // 扩展检测范围，搜索周围需要放置的位置
            for (int d = 1; d <= 6; d++) {
               // 检测下方d格位置
               if (this.checkBlock(baseVec, new BlockPos(baseX, this.baseY - d, baseZ))) {
                  return;
               }
               
               // 检测周围d格范围内的位置
               for (int x = 1; x <= d; x++) {
                  for (int z = 0; z <= d - x; z++) {
                     int y = d - x - z;
                     
                     // 检测四个方向
                     for (int rev1 = 0; rev1 <= 1; rev1++) {
                        for (int rev2 = 0; rev2 <= 1; rev2++) {
                           BlockPos checkPos = new BlockPos(
                               baseX + (rev1 == 0 ? x : -x),
                               this.baseY - y,
                               baseZ + (rev2 == 0 ? z : -z)
                           );
                           if (this.checkBlock(baseVec, checkPos)) {
                              return;
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private boolean checkBlock(Vec3 baseVec, BlockPos bp) {
      // 检查目标位置是否为空气
      if (!(mc.level.getBlockState(bp).getBlock() instanceof AirBlock)) {
         return false;
      }
      
      // 获取目标位置中心
      Vec3 center = new Vec3((double)bp.getX() + 0.5, (double)((float)bp.getY() + 0.5F), (double)bp.getZ() + 0.5);
      
      // 检查六个方向是否有可附着的方块
      for (Direction sbface : Direction.values()) {
         Vec3 hit = center.add(
                 new Vec3((double)sbface.getNormal().getX() * 0.5, (double)sbface.getNormal().getY() * 0.5, (double)sbface.getNormal().getZ() * 0.5)
         );
         Vec3i baseBlock = bp.offset(sbface.getNormal());
         BlockPos po = new BlockPos(baseBlock.getX(), baseBlock.getY(), baseBlock.getZ());
         
         // 检查附着位置是否为可站立方块
         if (mc.level.getBlockState(po).entityCanStandOnFace(mc.level, po, mc.player, sbface)) {
            // 检查距离是否在可放置范围内
            Vec3 relevant = hit.subtract(baseVec);
            if (relevant.lengthSqr() <= 20.25 && relevant.normalize().dot(Vec3.atLowerCornerOf(sbface.getNormal()).normalize()) >= 0.0) {
               // 记录可放置位置和方向
               this.pos = new Scaffold.BlockPosWithFacing(new BlockPos(baseBlock), sbface.getOpposite());
               return true;
            }
         }
      }
      return false;
   }

   @FlowExclude
   @ParameterObfuscationExclude
   public static Vec3 getVec3(BlockPos pos, Direction face) {
      double x = (double)pos.getX() + 0.5;
      double y = (double)pos.getY() + 0.5;
      double z = (double)pos.getZ() + 0.5;
      
      // 自救时使用精确的中心位置，避免随机偏移导致射线追踪失败
      if (PlayerUtils.playerStuckTicks > 0) {
         // 自救时使用精确的方块中心，确保射线能准确命中
         return new Vec3(x, y, z);
      }
      
      // 普通情况下的随机偏移
      double randomX = MathUtils.getRandomDoubleInRange(0.3, -0.3);
      double randomZ = MathUtils.getRandomDoubleInRange(0.3, -0.3);
      double randomY = MathUtils.getRandomDoubleInRange(0.1, -0.1);
      
      if (face != Direction.UP && face != Direction.DOWN) {
         y += 0.08 + randomY;
      } else {
         x += randomX;
         z += randomZ;
      }

      if (face == Direction.WEST || face == Direction.EAST) {
         z += randomZ;
      }

      if (face == Direction.SOUTH || face == Direction.NORTH) {
         x += randomX;
      }

      return new Vec3(x, y, z);
   }
   
   /**
    * 检查是否可以放置方块，从玩家脚下开始寻找
    */
   private boolean canPlaceBlocks() {
      if (mc.player == null || mc.level == null) return false;
      
      Vec3 baseVec = mc.player.getEyePosition().add(mc.player.getDeltaMovement().multiply(2.0, 2.0, 2.0));
      BlockPos playerPos = BlockPos.containing(mc.player.getX(), mc.player.getY(), mc.player.getZ());
      
      // 从玩家脚下开始寻找，检查是否有可放置的方块
      BlockPos startPos = playerPos.below();
      
      // 搜索范围：玩家周围2.5*2.5*2.5的区域
      int searchRange = 3;
      
      // 使用队列进行广度优先搜索
      java.util.Queue<BlockPos> queue = new java.util.LinkedList<>();
      java.util.Set<BlockPos> visited = new java.util.HashSet<>();
      
      queue.add(startPos);
      visited.add(startPos);
      
      while (!queue.isEmpty()) {
         BlockPos currentPos = queue.poll();
         
         // 检查当前位置是否可以放置方块
         if (this.canPlaceBlockAt(currentPos, baseVec)) {
            return true;
         }
         
         // 搜索相邻位置
         for (Direction direction : Direction.values()) {
            BlockPos neighborPos = currentPos.relative(direction);
            
            // 检查是否在搜索范围内
            int dx = neighborPos.getX() - playerPos.getX();
            int dy = neighborPos.getY() - playerPos.getY();
            int dz = neighborPos.getZ() - playerPos.getZ();
            
            if (Math.abs(dx) <= searchRange && Math.abs(dy) <= searchRange && Math.abs(dz) <= searchRange && !visited.contains(neighborPos)) {
               visited.add(neighborPos);
               queue.add(neighborPos);
            }
         }
      }
      
      return false;
   }
   
   /**
    * 检查单个位置是否可以放置方块
    */
   private boolean canPlaceBlockAt(BlockPos pos, Vec3 baseVec) {
      if (!(mc.level.getBlockState(pos).getBlock() instanceof AirBlock)) {
         return false;
      }
      
      Vec3 center = new Vec3((double)pos.getX() + 0.5, (double)((float)pos.getY() + 0.5F), (double)pos.getZ() + 0.5);
      
      for (Direction sbface : Direction.values()) {
         Vec3 hit = center.add(
                 new Vec3((double)sbface.getNormal().getX() * 0.5, (double)sbface.getNormal().getY() * 0.5, (double)sbface.getNormal().getZ() * 0.5)
         );
         Vec3i baseBlock = pos.offset(sbface.getNormal());
         BlockPos po = new BlockPos(baseBlock.getX(), baseBlock.getY(), baseBlock.getZ());
         if (mc.level.getBlockState(po).entityCanStandOnFace(mc.level, po, mc.player, sbface)) {
            Vec3 relevant = hit.subtract(baseVec);
            if (relevant.lengthSqr() <= 20.25 && relevant.normalize().dot(Vec3.atLowerCornerOf(sbface.getNormal()).normalize()) >= 0.0) {
               return true;
            }
         }
      }
      
      return false;
   }
   

   

   

   
   /**
    * 检查玩家是否处于掉落状态
    */
   private boolean isFalling() {
      // 检查玩家是否在掉落状态
      if (!mc.player.onGround() && mc.player.getDeltaMovement().y < 0) {
         // 使用FallingPlayer类检测是否会摔落
         FallingPlayer fallingPlayer = new FallingPlayer(mc.player);
         fallingPlayer.calculate(2); // 计算2格内的掉落情况
         return fallingPlayer.y < mc.player.getY();
      }
      return false;
   }
   

   
   /**
    * 触发玩家卡住，用于自救或特定场景
    */
   private void triggerPlayerStuck() {
      // 只有当玩家没有处于卡住状态时，才开始新的卡住周期
      // 这样可以避免多次触发自救时计数器累积
      if (PlayerUtils.playerStuckTicks == 0) {
          PlayerUtils.playerStuckTicks = 1;
      }
      
      // 获取当前游戏刻
      long currentTick = mc.level.getGameTime();
      
      // 检查Blink模块是否启用，如果启用且有积攒的包，则释放所有包
      // 使用ModuleManager获取Blink模块实例
      com.heypixel.heypixelmod.obsoverlay.modules.ModuleManager moduleManager = com.heypixel.heypixelmod.obsoverlay.Naven.getInstance().getModuleManager();
      com.heypixel.heypixelmod.obsoverlay.modules.impl.move.Blink blink = 
          (com.heypixel.heypixelmod.obsoverlay.modules.impl.move.Blink) 
          moduleManager.getModule(com.heypixel.heypixelmod.obsoverlay.modules.impl.move.Blink.class);
      
      // 检查Blink释放空档期
      long blinkCooldown = (long)this.blinkReleaseCooldownTicks.getCurrentValue();
      if (blink != null && blink.isEnabled() && currentTick - this.lastBlinkReleaseTick > blinkCooldown) {
          // 释放所有积攒的数据包，使用公共方法
          blink.releaseAllPackets();
          // 记录上次释放Blink包的时间
          this.lastBlinkReleaseTick = currentTick;
      }
   }
   
   public static record BlockPosWithFacing(BlockPos position, Direction facing) {
   }
}
