package com.heypixel.heypixelmod.modules.impl.move;

import com.heypixel.heypixelmod.mixin.accessors.LivingEntityAccessor;
import com.heypixel.heypixelmod.events.api.EventTarget;
import com.heypixel.heypixelmod.events.api.types.EventType;
import com.heypixel.heypixelmod.events.impl.EventMotion;
import com.heypixel.heypixelmod.modules.Category;
import com.heypixel.heypixelmod.modules.Module;
import com.heypixel.heypixelmod.modules.ModuleInfo;

@ModuleInfo(
   name = "NoJumpDelay",
   description = "Removes the delay when jumping",
   category = Category.MOVEMENT
)
public class NoJumpDelay extends Module {
   @EventTarget
   public void onMotion(EventMotion e) {
      if (e.getType() == EventType.PRE) {
         ((LivingEntityAccessor)mc.player).setNoJumpDelay(0);
      }
   }
}
