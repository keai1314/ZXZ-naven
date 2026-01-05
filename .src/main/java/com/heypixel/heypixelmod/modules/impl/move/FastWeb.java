package com.heypixel.heypixelmod.modules.impl.move;

import com.heypixel.heypixelmod.events.api.EventTarget;
import com.heypixel.heypixelmod.events.api.types.EventType;
import com.heypixel.heypixelmod.events.impl.EventMotion;
import com.heypixel.heypixelmod.events.impl.EventMoveInput;
import com.heypixel.heypixelmod.events.impl.EventStuckInBlock;
import com.heypixel.heypixelmod.modules.Category;
import com.heypixel.heypixelmod.modules.Module;
import com.heypixel.heypixelmod.modules.ModuleInfo;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

@ModuleInfo(
   name = "FastWeb",
   category = Category.MOVEMENT,
   description = "Allows you to walk faster on cobwebs"
)
public class FastWeb extends Module {
   private int playerInWebTick = 0;
   private int ticksInWeb = 0;

   @EventTarget
   public void onMotion(EventMotion e) {
      if (e.getType() == EventType.POST && this.playerInWebTick < mc.player.tickCount) {
         this.ticksInWeb = 0;
      }
   }

   @EventTarget
   public void onJump(EventMoveInput e) {
      if (this.ticksInWeb > 1) {
         e.setJump(false);
      }
   }

   @EventTarget
   public void onStuck(EventStuckInBlock e) {
      if (e.getState().getBlock() == Blocks.COBWEB) {
         this.playerInWebTick = mc.player.tickCount;
         this.ticksInWeb++;
         if (this.ticksInWeb > 5) {
            Vec3 newSpeed = new Vec3(0.88, 1.88, 0.88);
            e.setStuckSpeedMultiplier(newSpeed);
         }
      }
   }
}
