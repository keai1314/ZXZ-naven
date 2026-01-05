package com.heypixel.heypixelmod.obsoverlay.modules.impl.render;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRenderScoreboard;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRenderTabOverlay;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import com.heypixel.heypixelmod.obsoverlay.ui.LanguageManager;

@ModuleInfo(
   name = "服务器名称欺骗",
   description = "欺骗服务器的名称",
   category = Category.RENDER
)
public class ScoreboardSpoof extends Module {
   @EventTarget
   public void onRenderScoreboard(EventRenderScoreboard e) {
      String string = e.getComponent().getString();
      if (string.contains("布吉岛")) {
         MutableComponent textComponent = Component.literal("§d§l不准岛");
         textComponent.setStyle(e.getComponent().getStyle());
         e.setComponent(textComponent);
      }
   }

   @EventTarget
   public void onRenderTab(EventRenderTabOverlay e) {
      String string = e.getComponent().getString();
      if (string.contains("布吉岛")) {
         if (e.getType() == EventType.HEADER) {
            e.setComponent(Component.literal("§d§l不准岛"));
         } else if (e.getType() == EventType.FOOTER) {
            e.setComponent(Component.literal(""));
         }
      }
   }
   
   public String getLocalizedName() {
      return LanguageManager.getInstance().getLocalizedString("服务器名称欺骗", "Scoreboard Spoof");
   }

   public String getLocalizedDescription() {
      return LanguageManager.getInstance().getLocalizedString("欺骗服务器的名称", "Spoof server name");
   }
}