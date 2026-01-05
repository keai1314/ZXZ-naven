package com.heypixel.heypixelmod.obsoverlay.modules.impl.render;

import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.ui.ClickGUI;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue; // 添加FloatValue导入
import com.heypixel.heypixelmod.obsoverlay.ui.LanguageManager;
import com.heypixel.heypixelmod.obsoverlay.Naven;

@ModuleInfo(
        name = "界面",
        category = Category.RENDER,
        description = "界面"
)
public class ClickGUIModule extends Module {
   ClickGUI clickGUI = null;
   public BooleanValue chinese = ValueBuilder.create(this, "中文").setDefaultBooleanValue(true).build().getBooleanValue();
   // 添加背景默认开启设置
   public BooleanValue background = ValueBuilder.create(this, "背景").setDefaultBooleanValue(true).build().getBooleanValue();
   // 添加边框圆滑度设置
   public FloatValue cornerRadius = ValueBuilder.create(this, "边框圆滑度").setDefaultFloatValue(3.0f).setMinFloatValue(0.0f).setMaxFloatValue(10.0f).setFloatStep(0.5f).build().getFloatValue();
   private boolean lastChineseValue = true;

   @Override
   protected void initModule() {
      super.initModule();
      this.setKey(344);
   }

   @Override
   public void onEnable() {
      // 确保背景模块启用
      if (this.background.getCurrentValue()) {
         Module backgroundModule = Naven.getInstance().getModuleManager().getModule(PostProcess.class);
         if (backgroundModule != null && !backgroundModule.isEnabled()) {
            backgroundModule.toggle();
         }
      }
      
      if (this.clickGUI == null) {
         this.clickGUI = new ClickGUI();
      }

      mc.setScreen(this.clickGUI);
      this.toggle();
   }

   public void onUpdate() {
      // 每次都更新全局语言设置，确保与UI设置同步
      boolean currentChineseValue = this.chinese.getCurrentValue();
      LanguageManager.getInstance().setLanguage(currentChineseValue);
      
      // 检查语言设置是否发生变化
      if (currentChineseValue != this.lastChineseValue) {
         // 强制刷新模块显示
         Module.update = true;
         // 更新上次记录的值
         this.lastChineseValue = currentChineseValue;
      }
      
      // 确保背景模块状态与设置同步
      if (this.background.getCurrentValue()) {
         Module backgroundModule = Naven.getInstance().getModuleManager().getModule(PostProcess.class);
         if (backgroundModule != null && !backgroundModule.isEnabled()) {
            backgroundModule.toggle();
         }
      }
   }
}