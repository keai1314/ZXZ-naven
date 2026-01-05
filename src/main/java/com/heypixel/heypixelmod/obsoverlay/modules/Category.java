package com.heypixel.heypixelmod.obsoverlay.modules;

import com.heypixel.heypixelmod.obsoverlay.utils.FontIcons;

public enum Category {
   COMBAT("战斗", FontIcons.SWORD),
   MOVEMENT("移动", FontIcons.RUNNING),
   RENDER("视觉", FontIcons.EYE),
   MISC("设置", FontIcons.OTHER);

   private final String displayName;
   private final String icon;

   private Category(final String displayName, final String icon) {
      this.displayName = displayName;
      this.icon = icon;
   }

   public String getDisplayName() {
      return this.displayName;
   }

   public String getIcon() {
      return this.icon;
   }
}
