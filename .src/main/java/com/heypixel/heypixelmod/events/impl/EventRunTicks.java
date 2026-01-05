package com.heypixel.heypixelmod.events.impl;

import com.heypixel.heypixelmod.events.api.events.Event;
import com.heypixel.heypixelmod.events.api.types.EventType;

public class EventRunTicks implements Event {
   private final EventType type;

   public EventType getType() {
      return this.type;
   }

   public EventRunTicks(EventType type) {
      this.type = type;
   }
}
