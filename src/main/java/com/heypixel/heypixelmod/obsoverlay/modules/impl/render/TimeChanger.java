package com.heypixel.heypixelmod.obsoverlay.modules.impl.render;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMotion;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventPacket;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import com.heypixel.heypixelmod.obsoverlay.ui.LanguageManager;

@ModuleInfo(
   name = "时间改变",
   description = "改变世界时间",
   category = Category.RENDER
)
public class TimeChanger extends Module {
   FloatValue time = ValueBuilder.create(this, "世界时间")
      .setDefaultFloatValue(8000.0F)
      .setFloatStep(1.0F)
      .setMinFloatValue(0.0F)
      .setMaxFloatValue(24000.0F)
      .build()
      .getFloatValue();

   @EventTarget
   public void onMotion(EventMotion e) {
      if (e.getType() == EventType.PRE) {
         mc.level.setDayTime((long)this.time.getCurrentValue());
      }
   }

   @EventTarget
   public void onPacket(EventPacket event) {
      if (event.getPacket() instanceof ClientboundSetTimePacket) {
         event.setCancelled(true);
      }
   }
   
   public String getLocalizedName() {
      return LanguageManager.getInstance().getLocalizedString("时间改变", "Time Changer");
   }

   public String getLocalizedDescription() {
      return LanguageManager.getInstance().getLocalizedString("改变世界时间", "Change world time");
   }
}