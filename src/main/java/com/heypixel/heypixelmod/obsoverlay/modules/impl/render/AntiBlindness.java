package com.heypixel.heypixelmod.obsoverlay.modules.impl.render;

import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.ui.LanguageManager;

@ModuleInfo(
   name = "防失明",
   description = "防止失明",
   category = Category.RENDER
)
public class AntiBlindness extends Module {

   public String getLocalizedName() {
      return LanguageManager.getInstance().getLocalizedString("防失明", "Anti Blindness");
   }

   public String getLocalizedDescription() {
      return LanguageManager.getInstance().getLocalizedString("防止失明", "Prevent blindness");
   }
}
