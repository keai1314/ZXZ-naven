package com.heypixel.heypixelmod.obsoverlay.modules.impl.render;

import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.ui.LanguageManager;

@ModuleInfo(
   name = "禁用视觉",
   description = "禁用视觉",
   category = Category.RENDER
)
public class NoRender extends Module {
   public BooleanValue disableEffects = ValueBuilder.create(this, "禁用药水效果").setDefaultBooleanValue(true).build().getBooleanValue();
   
   public String getLocalizedName() {
      return LanguageManager.getInstance().getLocalizedString("无视觉", "No Render");
   }

   public String getLocalizedDescription() {
      return LanguageManager.getInstance().getLocalizedString("禁用视觉", "Disable visuals");
   }
}