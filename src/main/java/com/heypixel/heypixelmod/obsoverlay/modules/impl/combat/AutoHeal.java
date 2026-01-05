//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.heypixel.heypixelmod.obsoverlay.modules.impl.combat;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMotion;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.utils.InventoryUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.PacketUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.TimeHelper;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.ModeValue;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import com.heypixel.heypixelmod.obsoverlay.ui.LanguageManager;

@ModuleInfo(
        name = "自动恢复",
        description = "当你生命值低时自动恢复生命值.",
        category = Category.COMBAT
)
public class AutoHeal extends Module {
   private final TimeHelper timer = new TimeHelper();
   private final BooleanValue speedCheck = ValueBuilder.create(this, "速度检查").setDefaultBooleanValue(true).build().getBooleanValue();
   private final BooleanValue regenCheck = ValueBuilder.create(this, "恢复检查").setDefaultBooleanValue(true).build().getBooleanValue();
   private final FloatValue delay = ValueBuilder.create(this, "延迟").setDefaultFloatValue(500.0F).setFloatStep(1.0F).setMinFloatValue(100.0F).setMaxFloatValue(1000.0F).build().getFloatValue();
   private final FloatValue health = ValueBuilder.create(this, "生命值").setDefaultFloatValue(0.5F).setFloatStep(0.05F).setMinFloatValue(0.0F).setMaxFloatValue(1.0F).build().getFloatValue();
   private final ModeValue mode = ValueBuilder.create(this, "模式").setModes(new String[]{"汤", "头"}).setDefaultModeIndex(0).build().getModeValue();
   private boolean switchBack = false;
   private boolean useItem = false;
   private boolean throwItem = false;

   public AutoHeal() {
   }

   public String getLocalizedName() {
      return LanguageManager.getInstance().getLocalizedString("自动恢复", "Auto Heal");
   }

   public String getLocalizedDescription() {
      return LanguageManager.getInstance().getLocalizedString("当你生命值低时自动恢复生命值.", "Automatically heal when your health is low.");
   }

   @EventTarget
   public void onMotion(EventMotion e) {
      if (e.getType() == EventType.PRE) {
         if (this.useItem) {
            PacketUtils.sendSequencedPacket((id) -> {
               return new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, id);
            });
            this.useItem = false;
            return;
         }

         if (this.throwItem) {
            mc.getConnection().send(new ServerboundPlayerActionPacket(Action.DROP_ITEM, BlockPos.ZERO, Direction.DOWN));
            this.throwItem = false;
            return;
         }

         if (this.switchBack) {
            mc.getConnection().send(new ServerboundSetCarriedItemPacket(mc.player.getInventory().selected));
            this.switchBack = false;
            return;
         }

         if (!this.timer.delay((double)this.delay.getCurrentValue())) {
            return;
         }

         if (mc.player.hasEffect(MobEffects.MOVEMENT_SPEED) && this.speedCheck.getCurrentValue()) {
            return;
         }

         if (mc.player.hasEffect(MobEffects.REGENERATION) && this.regenCheck.getCurrentValue()) {
            return;
         }

         if (mc.player.getHealth() / mc.player.getMaxHealth() < this.health.getCurrentValue()) {
            int ix;
            ItemStack stack;
            if (this.mode.isCurrentMode("汤")) {
               for(ix = 0; ix < 9; ++ix) {
                  stack = (ItemStack)mc.player.getInventory().items.get(ix);
                  if (stack.getItem() == Items.MUSHROOM_STEW) {
                     this.switchUseItem(ix, true);
                     this.switchBack = true;
                     break;
                  }
               }
            } else if (this.mode.isCurrentMode("头")) {
               for(ix = 0; ix < 9; ++ix) {
                  stack = (ItemStack)mc.player.getInventory().items.get(ix);
                  if (InventoryUtils.isGoldenHead(stack)) {
                     this.switchUseItem(ix, false);
                     this.switchBack = true;
                     break;
                  }
               }
            }
         }
      }

   }

   private void switchUseItem(int slot, boolean throwItem) {
      mc.getConnection().send(new ServerboundSetCarriedItemPacket(slot));
      this.throwItem = throwItem;
      this.useItem = true;
   }
}
