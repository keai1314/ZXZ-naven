package com.heypixel.heypixelmod.obsoverlay.modules.impl.render;

import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.ui.LanguageManager;

@ModuleInfo(
   name = "防呕吐",
   description = "防呕吐",
   category = Category.RENDER
)
public class AntiNausea extends Module {

   public String getLocalizedName() {
      return LanguageManager.getInstance().getLocalizedString("防呕吐", "Anti Nausea");
   }

   public String getLocalizedDescription() {
      return LanguageManager.getInstance().getLocalizedString("防呕吐", "Prevent nausea");
   }
}
