package com.heypixel.heypixelmod.events.impl;

import com.heypixel.heypixelmod.events.api.events.Event;

public class EventJump implements Event {
   private float yaw;

   public void setYaw(float yaw) {
      this.yaw = yaw;
   }

   public float getYaw() {
      return this.yaw;
   }

   public EventJump(float yaw) {
      this.yaw = yaw;
   }
}
