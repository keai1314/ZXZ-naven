package com.heypixel.heypixelmod.obsoverlay.modules.impl.render;

import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import com.heypixel.heypixelmod.obsoverlay.ui.LanguageManager;

@ModuleInfo(
   name = "夜视",
   description = "让你的世界更明亮.",
   category = Category.RENDER
)
public class FullBright extends Module {
   public FloatValue brightness = ValueBuilder.create(this, "亮度")
      .setDefaultFloatValue(1.0F)
      .setFloatStep(0.1F)
      .setMinFloatValue(0.0F)
      .setMaxFloatValue(1.0F)
      .build()
      .getFloatValue();
      
   public String getLocalizedName() {
      return LanguageManager.getInstance().getLocalizedString("夜视", "Full Bright");
   }

   public String getLocalizedDescription() {
      return LanguageManager.getInstance().getLocalizedString("让你的世界更明亮.", "Make your world brighter.");
   }
}