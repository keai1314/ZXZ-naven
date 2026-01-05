package com.heypixel.heypixelmod.modules.impl.render;

import com.heypixel.heypixelmod.events.api.EventTarget;
import com.heypixel.heypixelmod.events.api.types.EventType;
import com.heypixel.heypixelmod.events.impl.EventRenderScoreboard;
import com.heypixel.heypixelmod.events.impl.EventRenderTabOverlay;
import com.heypixel.heypixelmod.modules.Category;
import com.heypixel.heypixelmod.modules.Module;
import com.heypixel.heypixelmod.modules.ModuleInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

@ModuleInfo(
   name = "ServerNameSpoof",
   description = "Spoof the server name",
   category = Category.RENDER
)
public class ScoreboardSpoof extends Module {
   @EventTarget
   public void onRenderScoreboard(EventRenderScoreboard e) {
      String string = e.getComponent().getString();
      if (string.contains("布吉岛")) {
         MutableComponent textComponent = Component.literal("§d§l岛罐岛");
         textComponent.setStyle(e.getComponent().getStyle());
         e.setComponent(textComponent);
      }
   }

   @EventTarget
   public void onRenderTab(EventRenderTabOverlay e) {
      String string = e.getComponent().getString();
      if (string.contains("布吉岛")) {
         if (e.getType() == EventType.HEADER) {
            e.setComponent(Component.literal("§d§l岛罐岛"));
         } else if (e.getType() == EventType.FOOTER) {
            e.setComponent(Component.literal(""));
         }
      }
   }
}
