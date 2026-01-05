package com.heypixel.heypixelmod.obsoverlay.modules.impl.misc;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMotion;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventPacket;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.move.Scaffold;
import com.heypixel.heypixelmod.obsoverlay.ui.ClickGUI;
import com.heypixel.heypixelmod.obsoverlay.ui.notification.Notification;
import com.heypixel.heypixelmod.obsoverlay.ui.notification.NotificationLevel;
import com.heypixel.heypixelmod.obsoverlay.utils.InventoryUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.MoveUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.TickTimeHelper;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.ModeValue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import org.apache.commons.lang3.tuple.Pair;
import net.minecraft.world.item.HoeItem;
import com.heypixel.heypixelmod.obsoverlay.ui.LanguageManager;

@ModuleInfo(
   name = "背包管理",
   description = "自动管理你的背包",
   category = Category.MISC
)
public class InventoryCleaner extends Module {
   private static final TickTimeHelper timer = new TickTimeHelper();
   private final FloatValue delay = ValueBuilder.create(this, "延迟 (Ticks)")
      .setDefaultFloatValue(3.0F)
      .setFloatStep(1.0F)
      .setMinFloatValue(3.0F)
      .setMaxFloatValue(10.0F)
      .build()
      .getFloatValue();
   ModeValue offhandItems = ValueBuilder.create(this, "副手物品")
      .setModes("无", "金苹果", "投掷物", "鱼竿", "方块")
      .build()
      .getModeValue();
   BooleanValue autoArmor = ValueBuilder.create(this, "自动穿装备").setDefaultBooleanValue(true).build().getBooleanValue();
   BooleanValue inventoryOnly = ValueBuilder.create(this, "只有背包").setDefaultBooleanValue(true).build().getBooleanValue();
   BooleanValue switchSword = ValueBuilder.create(this, "切换剑").setDefaultBooleanValue(true).build().getBooleanValue();
   FloatValue swordSlot = ValueBuilder.create(this, "剑位置")
      .setDefaultFloatValue(1.0F)
      .setFloatStep(1.0F)
      .setMinFloatValue(1.0F)
      .setMaxFloatValue(9.0F)
      .setVisibility(() -> this.switchSword.getCurrentValue())
      .build()
      .getFloatValue();
   BooleanValue switchBlock = ValueBuilder.create(this, "切换方块")
      .setVisibility(() -> !this.offhandItems.isCurrentMode("方块"))
      .setDefaultBooleanValue(true)
      .build()
      .getBooleanValue();
   FloatValue blockSlot = ValueBuilder.create(this, "方块位置")
      .setDefaultFloatValue(2.0F)
      .setFloatStep(1.0F)
      .setMinFloatValue(1.0F)
      .setMaxFloatValue(9.0F)
      .setVisibility(() -> this.switchBlock.getCurrentValue() && !this.offhandItems.isCurrentMode("方块"))
      .build()
      .getFloatValue();
   FloatValue maxBlockSize = ValueBuilder.create(this, "最大方块数量")
      .setDefaultFloatValue(256.0F)
      .setFloatStep(64.0F)
      .setMinFloatValue(64.0F)
      .setMaxFloatValue(512.0F)
      .setVisibility(() -> this.switchBlock.getCurrentValue())
      .build()
      .getFloatValue();
   BooleanValue switchPickaxe = ValueBuilder.create(this, "切换稿子").setDefaultBooleanValue(true).build().getBooleanValue();
   FloatValue pickaxeSlot = ValueBuilder.create(this, "稿子位置")
      .setDefaultFloatValue(3.0F)
      .setFloatStep(1.0F)
      .setMinFloatValue(1.0F)
      .setMaxFloatValue(9.0F)
      .setVisibility(() -> this.switchPickaxe.getCurrentValue())
      .build()
      .getFloatValue();
   BooleanValue switchAxe = ValueBuilder.create(this, "切换斧子").setDefaultBooleanValue(true).build().getBooleanValue();
   FloatValue axeSlot = ValueBuilder.create(this, "斧子位置")
      .setDefaultFloatValue(4.0F)
      .setFloatStep(1.0F)
      .setMinFloatValue(1.0F)
      .setMaxFloatValue(9.0F)
      .setVisibility(() -> this.switchAxe.getCurrentValue())
      .build()
      .getFloatValue();
   BooleanValue switchBow = ValueBuilder.create(this, "切换弓类").setDefaultBooleanValue(true).build().getBooleanValue();
   FloatValue bowSlot = ValueBuilder.create(this, "弓类位置")
      .setDefaultFloatValue(5.0F)
      .setFloatStep(1.0F)
      .setMinFloatValue(1.0F)
      .setMaxFloatValue(9.0F)
      .setVisibility(() -> this.switchBow.getCurrentValue())
      .build()
      .getFloatValue();
   ModeValue preferBow = ValueBuilder.create(this, "弓类优先权")
      .setModes("弩", "力量弓", "冲击弓")
      .setVisibility(() -> this.switchBow.getCurrentValue())
      .build()
      .getModeValue();
   FloatValue maxArrowSize = ValueBuilder.create(this, "最大装备数量")
      .setDefaultFloatValue(256.0F)
      .setFloatStep(64.0F)
      .setMinFloatValue(64.0F)
      .setMaxFloatValue(512.0F)
      .setVisibility(() -> this.switchBow.getCurrentValue())
      .build()
      .getFloatValue();
   BooleanValue switchWaterBucket = ValueBuilder.create(this, "切换水桶").setDefaultBooleanValue(true).build().getBooleanValue();
   FloatValue waterBucketSlot = ValueBuilder.create(this, "水桶位置")
      .setDefaultFloatValue(6.0F)
      .setFloatStep(1.0F)
      .setMinFloatValue(1.0F)
      .setMaxFloatValue(9.0F)
      .setVisibility(() -> this.switchWaterBucket.getCurrentValue())
      .build()
      .getFloatValue();
   BooleanValue switchEnderPearl = ValueBuilder.create(this, "切换末影珍珠").setDefaultBooleanValue(true).build().getBooleanValue();
   FloatValue enderPearlSlot = ValueBuilder.create(this, "末影珍珠位置")
      .setDefaultFloatValue(7.0F)
      .setFloatStep(1.0F)
      .setMinFloatValue(1.0F)
      .setMaxFloatValue(9.0F)
      .setVisibility(() -> this.switchEnderPearl.getCurrentValue())
      .build()
      .getFloatValue();
   BooleanValue switchFireball = ValueBuilder.create(this, "切换烈焰弹").setDefaultBooleanValue(true).build().getBooleanValue();
   FloatValue fireballSlot = ValueBuilder.create(this, "烈焰弹位置")
      .setDefaultFloatValue(8.0F)
      .setFloatStep(1.0F)
      .setMinFloatValue(1.0F)
      .setMaxFloatValue(9.0F)
      .setVisibility(() -> this.switchFireball.getCurrentValue())
      .build()
      .getFloatValue();
   BooleanValue switchGoldenApple = ValueBuilder.create(this, "切换金苹果")
      .setVisibility(() -> !this.offhandItems.isCurrentMode("金苹果"))
      .setDefaultBooleanValue(true)
      .build()
      .getBooleanValue();
   FloatValue goldenAppleSlot = ValueBuilder.create(this, "金苹果位置")
      .setDefaultFloatValue(9.0F)
      .setFloatStep(1.0F)
      .setMinFloatValue(1.0F)
      .setMaxFloatValue(9.0F)
      .setVisibility(() -> this.switchGoldenApple.getCurrentValue() && !this.offhandItems.isCurrentMode("金苹果"))
      .build()
      .getFloatValue();
   BooleanValue throwItems = ValueBuilder.create(this, "投掷物品").setDefaultBooleanValue(true).build().getBooleanValue();
   FloatValue waterBucketCount = ValueBuilder.create(this, "保持水桶")
      .setDefaultFloatValue(1.0F)
      .setFloatStep(1.0F)
      .setMinFloatValue(0.0F)
      .setMaxFloatValue(5.0F)
      .setVisibility(() -> this.throwItems.getCurrentValue())
      .build()
      .getFloatValue();
   FloatValue lavaBucketCount = ValueBuilder.create(this, "保持岩浆桶")
      .setDefaultFloatValue(1.0F)
      .setFloatStep(1.0F)
      .setMinFloatValue(0.0F)
      .setMaxFloatValue(5.0F)
      .setVisibility(() -> this.throwItems.getCurrentValue())
      .build()
      .getFloatValue();
   BooleanValue keepProjectile = ValueBuilder.create(this, "保持鸡蛋或雪球").setDefaultBooleanValue(true).build().getBooleanValue();
   BooleanValue switchProjectile = ValueBuilder.create(this, "切换投掷物")
      .setDefaultBooleanValue(false)
      .setVisibility(() -> this.keepProjectile.getCurrentValue() && !this.offhandItems.isCurrentMode("投掷物"))
      .build()
      .getBooleanValue();
   FloatValue projectileSlot = ValueBuilder.create(this, "鸡蛋或雪球位置")
      .setDefaultFloatValue(9.0F)
      .setFloatStep(1.0F)
      .setMinFloatValue(1.0F)
      .setMaxFloatValue(9.0F)
      .setVisibility(() -> this.switchProjectile.getCurrentValue() && this.keepProjectile.getCurrentValue() && !this.offhandItems.isCurrentMode("投掷物"))
      .build()
      .getFloatValue();
   FloatValue maxProjectileSize = ValueBuilder.create(this, "最大鸡蛋或雪球数量")
      .setDefaultFloatValue(64.0F)
      .setFloatStep(16.0F)
      .setMinFloatValue(16.0F)
      .setMaxFloatValue(256.0F)
      .setVisibility(() -> this.keepProjectile.getCurrentValue())
      .build()
      .getFloatValue();
   BooleanValue switchRod = ValueBuilder.create(this, "切换鱼竿")
      .setVisibility(() -> !this.offhandItems.isCurrentMode("鱼竿"))
      .setDefaultBooleanValue(false)
      .build()
      .getBooleanValue();
   FloatValue rodSlot = ValueBuilder.create(this, "鱼竿位置")
      .setDefaultFloatValue(9.0F)
      .setFloatStep(1.0F)
      .setMinFloatValue(1.0F)
      .setMaxFloatValue(9.0F)
      .setVisibility(() -> this.switchRod.getCurrentValue() && !this.offhandItems.isCurrentMode("鱼竿"))
      .build()
      .getFloatValue();
   int noMoveTicks = 0;
   private boolean clickOffHand = false;
   private boolean inventoryOpen = false;

   public static int getMaxBlockSize() {
      return (int)((InventoryCleaner)Naven.getInstance().getModuleManager().getModule(InventoryCleaner.class)).maxBlockSize.getCurrentValue();
   }

   public static boolean shouldKeepProjectile() {
      return ((InventoryCleaner)Naven.getInstance().getModuleManager().getModule(InventoryCleaner.class)).keepProjectile.getCurrentValue();
   }

   public static int getMaxProjectileSize() {
      return (int)((InventoryCleaner)Naven.getInstance().getModuleManager().getModule(InventoryCleaner.class)).maxProjectileSize.getCurrentValue();
   }

   public static int getMaxArrowSize() {
      return (int)((InventoryCleaner)Naven.getInstance().getModuleManager().getModule(InventoryCleaner.class)).maxArrowSize.getCurrentValue();
   }

   public static int getWaterBucketCount() {
      return (int)((InventoryCleaner)Naven.getInstance().getModuleManager().getModule(InventoryCleaner.class)).waterBucketCount.getCurrentValue();
   }

   public static int getLavaBucketCount() {
      return (int)((InventoryCleaner)Naven.getInstance().getModuleManager().getModule(InventoryCleaner.class)).lavaBucketCount.getCurrentValue();
   }
   public boolean isItemUseful(ItemStack stack) {
      if (stack.isEmpty()) {
         return false;
      } else if (InventoryUtils.isGodItem(stack)) {
         return true;
      } else if (stack.getDisplayName().getString().contains("点击使用")) {
         return true;
      }
      // 添加排除逻辑：不获取钓鱼竿、指南针、箱子、铁砧、小麦种子、铲子和锄头
      else if (stack.getItem() == Items.FISHING_ROD ||
              stack.getItem() == Items.COMPASS ||
              stack.getItem() == Items.CHEST ||
              stack.getItem() == Items.ANVIL ||
              stack.getItem() == Items.WHEAT_SEEDS) {
         return false;
      } else if (stack.getItem() instanceof ArmorItem) {
         ArmorItem item = (ArmorItem)stack.getItem();
         float protection = InventoryUtils.getProtection(stack);
         if (InventoryUtils.getCurrentArmorScore(item.getEquipmentSlot()) >= protection) {
            return false;
         } else {
            float bestArmor = InventoryUtils.getBestArmorScore(item.getEquipmentSlot());
            return !(protection < bestArmor);
         }
      } else if (stack.getItem() instanceof SwordItem) {
         return InventoryUtils.getBestSword() == stack;
      } else if (stack.getItem() instanceof PickaxeItem) {
         return InventoryUtils.getBestPickaxe() == stack;
      } else if (stack.getItem() instanceof AxeItem && !InventoryUtils.isSharpnessAxe(stack)) {
         return InventoryUtils.getBestAxe() == stack;
      }
      // 修改原有的铲子和锄头判断，添加排除逻辑
      else if (stack.getItem() instanceof ShovelItem || stack.getItem() instanceof HoeItem) {
         return false;
      } else if (stack.getItem() instanceof CrossbowItem) {
         return InventoryUtils.getBestCrossbow() == stack;
      } else if (stack.getItem() instanceof BowItem && InventoryUtils.isPunchBow(stack)) {
         return InventoryUtils.getBestPunchBow() == stack;
      } else if (stack.getItem() instanceof BowItem && InventoryUtils.isPowerBow(stack)) {
         return InventoryUtils.getBestPowerBow() == stack;
      } else if (stack.getItem() instanceof BowItem && InventoryUtils.getItemCount(Items.BOW) > 1) {
         return false;
      } else if (stack.getItem() == Items.WATER_BUCKET && InventoryUtils.getItemCount(Items.WATER_BUCKET) > getWaterBucketCount()) {
         return false;
      } else if (stack.getItem() == Items.LAVA_BUCKET && InventoryUtils.getItemCount(Items.LAVA_BUCKET) > getLavaBucketCount()) {
         return false;
      } else if (stack.getItem() instanceof FishingRodItem && InventoryUtils.getItemCount(Items.FISHING_ROD) > 1) {
         return false;
      } else if ((stack.getItem() == Items.SNOWBALL || stack.getItem() == Items.EGG) && !shouldKeepProjectile()) {
         return false;
      } else {
         return stack.getItem() instanceof ItemNameBlockItem ? false : InventoryUtils.isCommonItemUseful(stack);
      }
   }

   @EventTarget
   public void onPacket(EventPacket e) {
      if (e.getType() == EventType.SEND) {
         if (e.getPacket() instanceof ServerboundContainerClosePacket) {
            this.inventoryOpen = false;
         }

         if (this.inventoryOpen && !this.inventoryOnly.getCurrentValue()) {
            if (e.getPacket() instanceof ServerboundMovePlayerPacket) {
               if (MoveUtils.isMoving()) {
                  mc.getConnection().send(new ServerboundContainerClosePacket(mc.player.inventoryMenu.containerId));
               }
            } else if (e.getPacket() instanceof ServerboundUseItemOnPacket
               || e.getPacket() instanceof ServerboundUseItemPacket
               || e.getPacket() instanceof ServerboundInteractPacket
               || e.getPacket() instanceof ServerboundPlayerActionPacket) {
               mc.getConnection().send(new ServerboundContainerClosePacket(mc.player.inventoryMenu.containerId));
            }
         }
      }
   }

   private boolean checkConfig() {
      List<Pair<BooleanValue, FloatValue>> pairs = new ArrayList<>();
      if (!this.keepProjectile.getCurrentValue()) {
         this.switchProjectile.setCurrentValue(false);
      }

      pairs.add(Pair.of(this.switchSword, this.swordSlot));
      pairs.add(Pair.of(this.switchPickaxe, this.pickaxeSlot));
      pairs.add(Pair.of(this.switchAxe, this.axeSlot));
      pairs.add(Pair.of(this.switchBow, this.bowSlot));
      pairs.add(Pair.of(this.switchWaterBucket, this.waterBucketSlot));
      pairs.add(Pair.of(this.switchEnderPearl, this.enderPearlSlot));
      pairs.add(Pair.of(this.switchFireball, this.fireballSlot));
      if (!this.offhandItems.isCurrentMode("金苹果")) {
         pairs.add(Pair.of(this.switchGoldenApple, this.goldenAppleSlot));
      }

      if (!this.offhandItems.isCurrentMode("投掷物")) {
         pairs.add(Pair.of(this.switchProjectile, this.projectileSlot));
      }

      if (!this.offhandItems.isCurrentMode("鱼竿")) {
         pairs.add(Pair.of(this.switchRod, this.rodSlot));
      }

      if (!this.offhandItems.isCurrentMode("方块")) {
         pairs.add(Pair.of(this.switchBlock, this.blockSlot));
      }

      Set<Integer> usedSlot = new HashSet<>();

      for (Pair<BooleanValue, FloatValue> pair : pairs) {
         if (((BooleanValue)pair.getKey()).getCurrentValue()) {
            int targetSlot = (int)(((FloatValue)pair.getValue()).getCurrentValue() - 1.0F);
            if (usedSlot.contains(targetSlot)) {
               return false;
            }

            usedSlot.add(targetSlot);
         }
      }

      return true;
   }

   @EventTarget
   public void onMotion(EventMotion e) {
      if (e.getType() == EventType.PRE) {
         if (!(mc.screen instanceof ClickGUI) && !this.checkConfig()) {
            Notification notification = new Notification(
               NotificationLevel.ERROR, "背包管理中的位置设置重复了! 请检查你的设置!", 8000L
            );
            Naven.getInstance().getNotificationManager().addNotification(notification);
            this.toggle();
            return;
         }

         if (InventoryUtils.shouldDisableFeatures()) {
            return;
         }

         if (MoveUtils.isMoving()) {
            this.noMoveTicks = 0;
         } else {
            this.noMoveTicks++;
         }

         if (ChestStealer.isWorking()
            || Naven.getInstance().getModuleManager().getModule(Scaffold.class).isEnabled()
            || (this.inventoryOnly.getCurrentValue() ? !(mc.screen instanceof InventoryScreen) : this.noMoveTicks <= 1)) {
            this.clickOffHand = false;
            return;
         }

         if (mc.screen instanceof AbstractContainerScreen container && container.getMenu().containerId != mc.player.inventoryMenu.containerId) {
            return;
         }

         if (this.autoArmor.getCurrentValue()) {
            for (int i = 0; i < mc.player.getInventory().armor.size(); i++) {
               ItemStack stack = (ItemStack)mc.player.getInventory().armor.get(i);
               if (stack.getItem() instanceof ArmorItem) {
                  ArmorItem item = (ArmorItem)stack.getItem();
                  if (!stack.isEmpty()
                     && timer.delay(this.delay.getCurrentValue())
                     && InventoryUtils.getBestArmorScore(item.getEquipmentSlot()) > InventoryUtils.getProtection(stack)) {
                     mc.gameMode.handleInventoryMouseClick(mc.player.inventoryMenu.containerId, 4 + (4 - i), 1, ClickType.THROW, mc.player);
                     this.inventoryOpen = true;
                     timer.reset();
                  }
               }
            }

            for (int ix = 0; ix < mc.player.getInventory().items.size(); ix++) {
               ItemStack stack = (ItemStack)mc.player.getInventory().items.get(ix);
               if (!stack.isEmpty() && stack.getItem() instanceof ArmorItem) {
                  ArmorItem item = (ArmorItem)stack.getItem();
                  float currentItemScore = InventoryUtils.getProtection(stack);
                  boolean isBestItem = InventoryUtils.getBestArmorScore(item.getEquipmentSlot()) == currentItemScore;
                  boolean isBetterItem = InventoryUtils.getCurrentArmorScore(item.getEquipmentSlot()) < currentItemScore;
                  if (isBestItem && isBetterItem && timer.delay(this.delay.getCurrentValue())) {
                     if (ix < 9) {
                        mc.gameMode.handleInventoryMouseClick(mc.player.inventoryMenu.containerId, ix + 36, 0, ClickType.QUICK_MOVE, mc.player);
                     } else {
                        mc.gameMode.handleInventoryMouseClick(mc.player.inventoryMenu.containerId, ix, 0, ClickType.QUICK_MOVE, mc.player);
                     }

                     this.inventoryOpen = true;
                     timer.reset();
                  }
               }
            }
         }

         if (this.clickOffHand && timer.delay(this.delay.getCurrentValue())) {
            mc.gameMode.handleInventoryMouseClick(mc.player.inventoryMenu.containerId, 45, 0, ClickType.PICKUP, mc.player);
            this.inventoryOpen = true;
            this.clickOffHand = false;
            timer.reset();
         }

         if (this.offhandItems.isCurrentMode("金苹果")) {
            ItemStack offHand = (ItemStack)mc.player.getInventory().offhand.get(0);
            int slot = InventoryUtils.getItemSlot(Items.GOLDEN_APPLE);
            if (slot != -1 && timer.delay(this.delay.getCurrentValue())) {
               if (offHand.getItem() == Items.GOLDEN_APPLE) {
                  ItemStack goldenAppleStack = (ItemStack)mc.player.getInventory().items.get(slot);
                  if (offHand.getCount() + goldenAppleStack.getCount() <= 64) {
                     if (slot < 9) {
                        mc.gameMode.handleInventoryMouseClick(mc.player.inventoryMenu.containerId, slot + 36, 0, ClickType.PICKUP, mc.player);
                     } else {
                        mc.gameMode.handleInventoryMouseClick(mc.player.inventoryMenu.containerId, slot, 0, ClickType.PICKUP, mc.player);
                     }

                     this.inventoryOpen = true;
                     this.clickOffHand = true;
                     timer.reset();
                  }
               } else {
                  this.swapOffHand(slot);
               }
            }
         } else if (this.offhandItems.isCurrentMode("投掷物")) {
            ItemStack offHand = (ItemStack)mc.player.getInventory().offhand.get(0);
            ItemStack bestProjectile = InventoryUtils.getBestProjectile();
            if (bestProjectile != null) {
               int slot = InventoryUtils.getItemStackSlot(bestProjectile);
               boolean shouldSwap = false;
               if (offHand.getItem() != Items.EGG && offHand.getItem() != Items.SNOWBALL) {
                  shouldSwap = true;
               } else if (offHand.getCount() < bestProjectile.getCount()) {
                  shouldSwap = true;
               }

               if (shouldSwap && slot != -1 && timer.delay(this.delay.getCurrentValue())) {
                  this.swapOffHand(slot);
               }
            }
         } else if (this.offhandItems.isCurrentMode("鱼竿")) {
            ItemStack offHand = (ItemStack)mc.player.getInventory().offhand.get(0);
            int slotx = InventoryUtils.getItemSlot(Items.FISHING_ROD);
            if (slotx != -1 && timer.delay(this.delay.getCurrentValue()) && offHand.getItem() != Items.FISHING_ROD) {
               this.swapOffHand(slotx);
            }
         } else if (this.offhandItems.isCurrentMode("方块")) {
            ItemStack offHand = (ItemStack)mc.player.getInventory().offhand.get(0);
            ItemStack bestBlock = InventoryUtils.getBestBlock();
            if (bestBlock != null) {
               int slotx = InventoryUtils.getItemStackSlot(bestBlock);
               boolean shouldSwapx = false;
               if (Scaffold.isValidStack(offHand)) {
                  if (offHand.getCount() < bestBlock.getCount()) {
                     shouldSwapx = true;
                  }
               } else {
                  shouldSwapx = true;
               }

               if (shouldSwapx && slotx != -1 && timer.delay(this.delay.getCurrentValue())) {
                  this.swapOffHand(slotx);
               }
            }
         }

         if (this.switchGoldenApple.getCurrentValue() && !this.offhandItems.isCurrentMode("金苹果")) {
            this.swapItem((int)(this.goldenAppleSlot.getCurrentValue() - 1.0F), Items.GOLDEN_APPLE);
         }

         if (this.switchBlock.getCurrentValue()) {
            int blockSlot = (int)(this.blockSlot.getCurrentValue() - 1.0F);
            ItemStack currentBlock = (ItemStack)mc.player.getInventory().items.get(blockSlot);
            ItemStack bestBlock = InventoryUtils.getBestBlock();
            if (bestBlock != null
               && (bestBlock.getCount() > currentBlock.getCount() || !Scaffold.isValidStack(currentBlock))
               && !this.offhandItems.isCurrentMode("方块")) {
               this.swapItem(blockSlot, bestBlock);
            }

            if ((float)InventoryUtils.getBlockCountInInventory() > this.maxBlockSize.getCurrentValue()) {
               ItemStack worstBlock = InventoryUtils.getWorstBlock();
               this.throwItem(worstBlock);
            }
         }

         if (this.switchSword.getCurrentValue()) {
            int slotxx = (int)(this.swordSlot.getCurrentValue() - 1.0F);
            ItemStack currentSword = (ItemStack)mc.player.getInventory().items.get(slotxx);
            ItemStack bestSword = InventoryUtils.getBestSword();
            ItemStack bestShapeAxe = InventoryUtils.getBestShapeAxe();
            if (InventoryUtils.getAxeDamage(bestShapeAxe) > InventoryUtils.getSwordDamage(bestSword)) {
               bestSword = bestShapeAxe;
            }

            if (bestSword != null) {
               float currentDamage = currentSword.getItem() instanceof SwordItem
                  ? InventoryUtils.getSwordDamage(currentSword)
                  : InventoryUtils.getAxeDamage(currentSword);
               float bestWeaponDamage = bestSword.getItem() instanceof SwordItem
                  ? InventoryUtils.getSwordDamage(bestSword)
                  : InventoryUtils.getAxeDamage(bestSword);
               if (bestWeaponDamage > currentDamage) {
                  this.swapItem(slotxx, bestSword);
               }
            }
         }

         if (this.switchPickaxe.getCurrentValue()) {
            int slotxxx = (int)(this.pickaxeSlot.getCurrentValue() - 1.0F);
            ItemStack bestPickaxe = InventoryUtils.getBestPickaxe();
            ItemStack currentPickaxe = (ItemStack)mc.player.getInventory().items.get(slotxxx);
            if (bestPickaxe != null
               && bestPickaxe.getItem() instanceof PickaxeItem
               && (InventoryUtils.getToolScore(bestPickaxe) > InventoryUtils.getToolScore(currentPickaxe) || !(currentPickaxe.getItem() instanceof PickaxeItem))
               )
             {
               this.swapItem(slotxxx, bestPickaxe);
            }
         }

         if (this.switchAxe.getCurrentValue()) {
            int slotxxx = (int)(this.axeSlot.getCurrentValue() - 1.0F);
            ItemStack bestAxe = InventoryUtils.getBestAxe();
            ItemStack currentAxe = (ItemStack)mc.player.getInventory().items.get(slotxxx);
            if (bestAxe != null
               && bestAxe.getItem() instanceof AxeItem
               && (InventoryUtils.getToolScore(bestAxe) > InventoryUtils.getToolScore(currentAxe) || !(currentAxe.getItem() instanceof AxeItem))) {
               this.swapItem(slotxxx, bestAxe);
            }
         }

         if (this.switchRod.getCurrentValue() && !this.offhandItems.isCurrentMode("鱼竿")) {
            int slotxxx = (int)(this.rodSlot.getCurrentValue() - 1.0F);
            ItemStack bestRod = InventoryUtils.getFishingRod();
            ItemStack currentRod = (ItemStack)mc.player.getInventory().items.get(slotxxx);
            if (!(currentRod.getItem() instanceof FishingRodItem)) {
               this.swapItem(slotxxx, bestRod);
            }
         }

         if (this.switchBow.getCurrentValue()) {
            int slotxxx = (int)(this.bowSlot.getCurrentValue() - 1.0F);
            ItemStack currentBow = (ItemStack)mc.player.getInventory().items.get(slotxxx);
            ItemStack bestBow;
            float bestBowScore;
            float currentBowScore;
            if (this.preferBow.isCurrentMode("弩")) {
               bestBow = InventoryUtils.getBestCrossbow();
               bestBowScore = InventoryUtils.getCrossbowScore(bestBow);
               currentBowScore = InventoryUtils.getCrossbowScore(currentBow);
            } else if (this.preferBow.isCurrentMode("力量弓")) {
               bestBow = InventoryUtils.getBestPowerBow();
               bestBowScore = InventoryUtils.getPowerBowScore(bestBow);
               currentBowScore = InventoryUtils.getPowerBowScore(currentBow);
            } else {
               bestBow = InventoryUtils.getBestPunchBow();
               bestBowScore = InventoryUtils.getPunchBowScore(bestBow);
               currentBowScore = InventoryUtils.getPunchBowScore(currentBow);
            }

            if (bestBow == null) {
               bestBow = InventoryUtils.getBestCrossbow();
               bestBowScore = InventoryUtils.getCrossbowScore(bestBow);
               currentBowScore = InventoryUtils.getCrossbowScore(currentBow);
            }

            if (bestBow == null) {
               bestBow = InventoryUtils.getBestPowerBow();
               bestBowScore = InventoryUtils.getPowerBowScore(bestBow);
               currentBowScore = InventoryUtils.getPowerBowScore(currentBow);
            }

            if (bestBow == null) {
               bestBow = InventoryUtils.getBestPunchBow();
               bestBowScore = InventoryUtils.getPunchBowScore(bestBow);
               currentBowScore = InventoryUtils.getPunchBowScore(currentBow);
            }

            if (bestBow != null && bestBowScore > currentBowScore) {
               this.swapItem(slotxxx, bestBow);
            }

            if ((float)InventoryUtils.getItemCount(Items.ARROW) > this.maxArrowSize.getCurrentValue()) {
               ItemStack worstArrow = InventoryUtils.getWorstArrow();
               this.throwItem(worstArrow);
            }
         }

         if (this.switchEnderPearl.getCurrentValue()) {
            this.swapItem((int)(this.enderPearlSlot.getCurrentValue() - 1.0F), Items.ENDER_PEARL);
         }

         if (this.switchWaterBucket.getCurrentValue()) {
            this.swapItem((int)(this.waterBucketSlot.getCurrentValue() - 1.0F), Items.WATER_BUCKET);
         }

         if (this.switchFireball.getCurrentValue()) {
            this.swapItem((int)(this.fireballSlot.getCurrentValue() - 1.0F), Items.FIRE_CHARGE);
         }

         if (this.keepProjectile.getCurrentValue()) {
            if ((float)(InventoryUtils.getItemCount(Items.EGG) + InventoryUtils.getItemCount(Items.SNOWBALL)) > this.maxProjectileSize.getCurrentValue()) {
               ItemStack worstProjectile = InventoryUtils.getWorstProjectile();
               this.throwItem(worstProjectile);
            }

            if (this.switchProjectile.getCurrentValue() && !this.offhandItems.isCurrentMode("投掷物")) {
               int projectileSlot = (int)(this.projectileSlot.getCurrentValue() - 1.0F);
               if (InventoryUtils.getItemCount(Items.EGG) > 0) {
                  this.swapItem(projectileSlot, Items.EGG);
               } else if (InventoryUtils.getItemCount(Items.SNOWBALL) > 0) {
                  this.swapItem(projectileSlot, Items.SNOWBALL);
               }
            }
         }

         if (this.throwItems.getCurrentValue()) {
            List<Integer> slots = IntStream.range(0, mc.player.getInventory().items.size()).boxed().collect(Collectors.toList());
            Collections.shuffle(slots);

            for (Integer slotxxxx : slots) {
               ItemStack stack = (ItemStack)mc.player.getInventory().items.get(slotxxxx);
               if (!stack.isEmpty() && !this.isItemUseful(stack)) {
                  this.throwItem(stack);
               }
            }
         }
      }
   }

   private void swapOffHand(int slot) {
      if (slot < 9) {
         mc.gameMode.handleInventoryMouseClick(mc.player.inventoryMenu.containerId, slot + 36, 40, ClickType.SWAP, mc.player);
      } else {
         mc.gameMode.handleInventoryMouseClick(mc.player.inventoryMenu.containerId, slot, 40, ClickType.SWAP, mc.player);
      }

      this.inventoryOpen = true;
      timer.reset();
   }

   private void throwItem(ItemStack item) {
      if (InventoryUtils.isItemValid(item) && timer.delay(this.delay.getCurrentValue())) {
         int itemSlot = InventoryUtils.getItemStackSlot(item);
         if (itemSlot != -1) {
            if (itemSlot < 9) {
               mc.gameMode.handleInventoryMouseClick(mc.player.inventoryMenu.containerId, itemSlot + 36, 1, ClickType.THROW, mc.player);
            } else {
               mc.gameMode.handleInventoryMouseClick(mc.player.inventoryMenu.containerId, itemSlot, 1, ClickType.THROW, mc.player);
            }

            this.inventoryOpen = true;
            timer.reset();
         }
      }
   }

   private void swapItem(int targetSlot, ItemStack bestItem) {
      ItemStack currentSlot = (ItemStack)mc.player.getInventory().items.get(targetSlot);
      if (InventoryUtils.isItemValid(currentSlot) && bestItem != currentSlot && timer.delay(this.delay.getCurrentValue())) {
         int bestItemSlot = InventoryUtils.getItemStackSlot(bestItem);
         if (bestItemSlot != -1) {
            if (bestItemSlot < 9) {
               mc.gameMode.handleInventoryMouseClick(mc.player.inventoryMenu.containerId, bestItemSlot + 36, targetSlot, ClickType.SWAP, mc.player);
            } else {
               mc.gameMode.handleInventoryMouseClick(mc.player.inventoryMenu.containerId, bestItemSlot, targetSlot, ClickType.SWAP, mc.player);
            }

            this.inventoryOpen = true;
            timer.reset();
         }
      }
   }

   private void swapItem(int targetSlot, Item item) {
      ItemStack currentSlot = (ItemStack)mc.player.getInventory().items.get(targetSlot);
      if (InventoryUtils.isItemValid(currentSlot) && timer.delay(this.delay.getCurrentValue())) {
         int bestItemSlot = InventoryUtils.getItemSlot(item);
         if (bestItemSlot != -1) {
            ItemStack bestItemStack = (ItemStack)mc.player.getInventory().items.get(bestItemSlot);
            if (currentSlot.getItem() != item || currentSlot.getItem() == item && currentSlot.getCount() < bestItemStack.getCount()) {
               if (bestItemSlot < 9) {
                  mc.gameMode.handleInventoryMouseClick(mc.player.inventoryMenu.containerId, bestItemSlot + 36, targetSlot, ClickType.SWAP, mc.player);
               } else {
                  mc.gameMode.handleInventoryMouseClick(mc.player.inventoryMenu.containerId, bestItemSlot, targetSlot, ClickType.SWAP, mc.player);
               }

               this.inventoryOpen = true;
               timer.reset();
            }
         }
      }
   }

   public String getLocalizedName() {
      return LanguageManager.getInstance().getLocalizedString("背包管理", "Inventory Cleaner");
   }

   public String getLocalizedDescription() {
      return LanguageManager.getInstance().getLocalizedString("自动管理你的背包", "Automatically manage your inventory");
   }
}
