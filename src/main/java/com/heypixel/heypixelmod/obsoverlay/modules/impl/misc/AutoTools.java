package com.heypixel.heypixelmod.obsoverlay.modules.impl.misc;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMotion;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventUpdateHeldItem;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.utils.InventoryUtils;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.RedStoneOreBlock;
import net.minecraft.world.level.block.WebBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;
import com.heypixel.heypixelmod.obsoverlay.ui.LanguageManager;

@ModuleInfo(
   name = "自动选择工具",
   description = "自动为工作选择最好的工具",
   category = Category.MISC
)
public class AutoTools extends Module {
   private final BooleanValue checkSword = ValueBuilder.create(this, "检查剑").setDefaultBooleanValue(true).build().getBooleanValue();
   private final BooleanValue switchBack = ValueBuilder.create(this, "切换回去").setDefaultBooleanValue(true).build().getBooleanValue();
   private final BooleanValue silent = ValueBuilder.create(this, "静默")
      .setVisibility(this.switchBack::getCurrentValue)
      .setDefaultBooleanValue(true)
      .build()
      .getBooleanValue();
   private int originSlot = -1;

   public String getLocalizedName() {
      return LanguageManager.getInstance().getLocalizedString("自动选择工具", "Auto Tools");
   }

   public String getLocalizedDescription() {
      return LanguageManager.getInstance().getLocalizedString("自动为工作选择最好的工具", "Automatically select the best tool for the job");
   }
   
   @EventTarget
   public void onUpdateHeldItem(EventUpdateHeldItem e) {
      if (this.switchBack.getCurrentValue() && this.silent.getCurrentValue() && e.getHand() == InteractionHand.MAIN_HAND && this.originSlot != -1) {
         e.setItem(mc.player.getInventory().getItem(this.originSlot));
      }
   }

   @EventTarget
   public void onMotion(EventMotion e) {
      if (e.getType() == EventType.PRE) {
         if (mc.gameMode.isDestroying()) {
            if (this.checkSword.getCurrentValue()) {
               ItemStack itemStack = mc.player.getMainHandItem();
               if (itemStack.getItem() instanceof SwordItem) {
                  return;
               }
            }

            if (mc.hitResult.getType() == Type.BLOCK) {
               BlockHitResult hitResult = (BlockHitResult)mc.hitResult;
               int bestTool = this.getBestTool(hitResult.getBlockPos());
               if (bestTool != -1 && bestTool != mc.player.getInventory().selected) {
                  this.originSlot = mc.player.getInventory().selected;
                  mc.player.getInventory().selected = bestTool;
               }
            }
         }
      } else if (!mc.gameMode.isDestroying() && this.switchBack.getCurrentValue() && this.originSlot != -1) {
         mc.player.getInventory().selected = this.originSlot;
         this.originSlot = -1;
      }
   }

   private int getBestTool(BlockPos pos) {
      BlockState blockState = mc.level.getBlockState(pos);
      Block block = blockState.getBlock();
      int slot = 0;
      float dmg = 1.0F;

      for (int index = 0; index < 9; index++) {
         ItemStack itemStack = mc.player.getInventory().getItem(index);
         if (!InventoryUtils.isGodItem(itemStack)
            && !itemStack.isEmpty()
            && !blockState.isAir()
            && (!(itemStack.getItem() instanceof SwordItem) || block instanceof WebBlock)) {
            float strVsBlock = itemStack.getItem().getDestroySpeed(itemStack, blockState);
            if (strVsBlock > 1.0F && !(block instanceof DropExperienceBlock) && !(block instanceof RedStoneOreBlock)) {
               int i = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.BLOCK_EFFICIENCY, itemStack);
               if (i > 0) {
                  strVsBlock += (float)(i * i + 1);
               }
            }

            if (strVsBlock > dmg) {
               slot = index;
               dmg = strVsBlock;
            }
         }
      }

      return dmg > 1.0F ? slot : -1;
   }
}
