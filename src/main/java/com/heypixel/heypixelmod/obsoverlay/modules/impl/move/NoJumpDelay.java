package com.heypixel.heypixelmod.obsoverlay.modules.impl.move;

import com.heypixel.heypixelmod.mixin.O.accessors.LivingEntityAccessor;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMotion;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.ui.LanguageManager;

@ModuleInfo(
   name = "无跳跃延迟",
   description = "移除跳跃时的延迟",
   category = Category.MOVEMENT
)
public class NoJumpDelay extends Module {
   @EventTarget
   public void onMotion(EventMotion e) {
      if (e.getType() == EventType.PRE) {
         ((LivingEntityAccessor)mc.player).setNoJumpDelay(0);
      }
   }
   
   public String getLocalizedName() {
      return LanguageManager.getInstance().getLocalizedString("无跳跃延迟", "No Jump Delay");
   }

   public String getLocalizedDescription() {
      return LanguageManager.getInstance().getLocalizedString("移除跳跃时的延迟", "Remove delay when jumping");
   }
}
