package com.heypixel.heypixelmod.obsoverlay.modules.impl.render;

import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import com.heypixel.heypixelmod.obsoverlay.ui.LanguageManager;

@ModuleInfo(
   name = "记分板",
   description = "修改记分牌",
   category = Category.RENDER
)
public class Scoreboard extends Module {
   public BooleanValue hideScore = ValueBuilder.create(this, "隐藏红色分数").setDefaultBooleanValue(true).build().getBooleanValue();
   public FloatValue down = ValueBuilder.create(this, "向下")
      .setDefaultFloatValue(120.0F)
      .setFloatStep(1.0F)
      .setMinFloatValue(0.0F)
      .setMaxFloatValue(300.0F)
      .build()
      .getFloatValue();
      
   public String getLocalizedName() {
      return LanguageManager.getInstance().getLocalizedString("记分板", "Scoreboard");
   }

   public String getLocalizedDescription() {
      return LanguageManager.getInstance().getLocalizedString("修改记分牌", "Modify scoreboard");
   }
}