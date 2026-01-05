package com.heypixel.heypixelmod.obsoverlay.modules.impl.render;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRenderTabOverlay;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.StringUtils;
import com.heypixel.heypixelmod.obsoverlay.ui.LanguageManager;

@ModuleInfo(
   name = "名字保护",
   description = "保护你的名字",
   category = Category.RENDER
)
public class NameProtect extends Module {
   public static NameProtect instance;

   public NameProtect() {
      instance = this;
   }

   public static String getName(String string) {
      if (!instance.isEnabled() || mc.player == null) {
         return string;
      } else {
         return string.contains(mc.player.getName().getString()) ? StringUtils.replace(string, mc.player.getName().getString(), "§d王洪文§7") : string;
      }
   }

   @EventTarget
   public void onRenderTab(EventRenderTabOverlay e) {
      e.setComponent(Component.literal(getName(e.getComponent().getString())));
   }
   
   public String getLocalizedName() {
      return LanguageManager.getInstance().getLocalizedString("名字保护", "Name Protect");
   }

   public String getLocalizedDescription() {
      return LanguageManager.getInstance().getLocalizedString("保护你的名字", "Protect your name");
   }
}