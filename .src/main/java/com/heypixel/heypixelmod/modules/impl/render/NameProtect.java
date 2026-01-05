package com.heypixel.heypixelmod.modules.impl.render;

import com.heypixel.heypixelmod.events.api.EventTarget;
import com.heypixel.heypixelmod.events.impl.EventRenderTabOverlay;
import com.heypixel.heypixelmod.modules.Category;
import com.heypixel.heypixelmod.modules.Module;
import com.heypixel.heypixelmod.modules.ModuleInfo;
import com.heypixel.heypixelmod.values.ValueBuilder;
import com.heypixel.heypixelmod.values.impl.StringValue;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.StringUtils;

@ModuleInfo(
   name = "NameProtect",
   description = "Protect your name",
   category = Category.RENDER
)
public class NameProtect extends Module {
   public static NameProtect instance;
   private static String customHiddenName = "§dHidden§7";

   public StringValue hiddenNameValue = ValueBuilder.create(this, "Hidden Name")
      .setDefaultStringValue("Hidden")
      .setOnUpdate((value) -> {
         customHiddenName = "§d" + value.getStringValue().getCurrentValue() + "§7";
      })
      .build()
      .getStringValue();

   public NameProtect() {
      instance = this;
      customHiddenName = "§d" + hiddenNameValue.getCurrentValue() + "§7";
   }

   public static void setCustomHiddenName(String name) {
      customHiddenName = "§d" + name + "§7";
      if (instance != null && instance.hiddenNameValue != null) {
         instance.hiddenNameValue.setCurrentValue(name);
      }
   }

   public static String getHiddenName() {
      return customHiddenName;
   }

   public static String getName(String string) {
      if (!instance.isEnabled() || mc.player == null) {
         return string;
      } else {
         return string.contains(mc.player.getName().getString()) ? StringUtils.replace(string, mc.player.getName().getString(), customHiddenName) : string;
      }
   }

   @EventTarget
   public void onRenderTab(EventRenderTabOverlay e) {
      e.setComponent(Component.literal(getName(e.getComponent().getString())));
   }
}