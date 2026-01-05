package com.heypixel.heypixelmod.obsoverlay.modules.impl.render;

import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.ui.LanguageManager;

@ModuleInfo(
   name = "无受伤动画",
   description = "禁用受伤抖动动画",
   category = Category.RENDER
)
public class NoHurtCam extends Module {
   public String getLocalizedName() {
      return LanguageManager.getInstance().getLocalizedString("无受伤动画", "No Hurt Cam");
   }

   public String getLocalizedDescription() {
      return LanguageManager.getInstance().getLocalizedString("禁用受伤抖动动画", "Disable hurt shake animation");
   }
}