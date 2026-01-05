package com.heypixel.heypixelmod.obsoverlay.modules.impl.move;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMotion;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.mojang.blaze3d.platform.InputConstants;
import com.heypixel.heypixelmod.obsoverlay.ui.LanguageManager;

@ModuleInfo(
   name = "安全行走",
   description = "防止你从方块上掉下来",
   category = Category.MOVEMENT
)
public class SafeWalk extends Module {
   public static boolean isOnBlockEdge(float sensitivity) {
      return !mc.level
         .getCollisions(mc.player, mc.player.getBoundingBox().move(0.0, -0.5, 0.0).inflate((double)(-sensitivity), 0.0, (double)(-sensitivity)))
         .iterator()
         .hasNext();
   }

   @EventTarget
   public void onMotion(EventMotion e) {
      if (e.getType() == EventType.PRE) {
         mc.options.keyShift.setDown(mc.player.onGround() && isOnBlockEdge(0.3F));
      }
   }

   @Override
   public void onDisable() {
      boolean isHoldingShift = InputConstants.isKeyDown(mc.getWindow().getWindow(), mc.options.keyShift.getKey().getValue());
      mc.options.keyShift.setDown(isHoldingShift);
   }

   public String getLocalizedName() {
      return LanguageManager.getInstance().getLocalizedString("安全行走", "Safe Walk");
   }

   public String getLocalizedDescription() {
      return LanguageManager.getInstance().getLocalizedString("防止你从方块上掉下来", "Prevent you from falling off blocks");
   }
}
