package com.heypixel.heypixelmod.modules.impl.render;

import com.heypixel.heypixelmod.events.api.EventTarget;
import com.heypixel.heypixelmod.events.api.types.EventType;
import com.heypixel.heypixelmod.events.impl.EventMotion;
import com.heypixel.heypixelmod.events.impl.EventPacket;
import com.heypixel.heypixelmod.modules.Category;
import com.heypixel.heypixelmod.modules.Module;
import com.heypixel.heypixelmod.modules.ModuleInfo;
import com.heypixel.heypixelmod.values.ValueBuilder;
import com.heypixel.heypixelmod.values.impl.FloatValue;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;

@ModuleInfo(
   name = "TimeChanger",
   description = "Change the time of the world",
   category = Category.RENDER
)
public class TimeChanger extends Module {
   FloatValue time = ValueBuilder.create(this, "World Time")
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
}
