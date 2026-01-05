package com.heypixel.heypixelmod.obsoverlay.modules.impl.move;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMotion;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.ui.LanguageManager;

@ModuleInfo(
   name = "疾跑",
   description = "自动疾跑",
   category = Category.MOVEMENT
)
public class Sprint extends Module {
   @EventTarget(0)
   public void onMotion(EventMotion e) {
      if (e.getType() == EventType.PRE) {
         mc.options.keySprint.setDown(true);
         mc.options.toggleSprint().set(false);
      }
   }

   public String getLocalizedName() {
      return LanguageManager.getInstance().getLocalizedString("疾跑", "Sprint");
   }

   public String getLocalizedDescription() {
      return LanguageManager.getInstance().getLocalizedString("自动疾跑", "Auto sprint");
   }
   
   @Override
   public void onDisable() {
      mc.options.keySprint.setDown(false);
   }
}
