package com.heypixel.heypixelmod.obsoverlay.modules.impl.render;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import com.heypixel.heypixelmod.obsoverlay.ui.LanguageManager;

@ModuleInfo(
   name = "发光",
   description = "让实体发光",
   category = Category.RENDER
)
public class Glow extends Module {
   BooleanValue players = ValueBuilder.create(this, "玩家").setDefaultBooleanValue(true).build().getBooleanValue();
   BooleanValue items = ValueBuilder.create(this, "项目").setDefaultBooleanValue(false).build().getBooleanValue();
   BooleanValue mobs = ValueBuilder.create(this, "生物").setDefaultBooleanValue(false).build().getBooleanValue();
   BooleanValue animals = ValueBuilder.create(this, "动物").setDefaultBooleanValue(false).build().getBooleanValue();
   BooleanValue arrows = ValueBuilder.create(this, "箭头").setDefaultBooleanValue(false).build().getBooleanValue();

   public static boolean shouldGlow(Entity entity) {
      Glow module = (Glow)Naven.getInstance().getModuleManager().getModule(Glow.class);
      if (!module.isEnabled()) {
         return false;
      } else if (entity instanceof Player && module.players.getCurrentValue()) {
         return true;
      } else if (entity instanceof ItemEntity && module.items.getCurrentValue()) {
         return true;
      } else if (entity instanceof Mob && module.mobs.getCurrentValue()) {
         return true;
      } else {
         return entity instanceof Animal && module.animals.getCurrentValue() ? true : entity instanceof Arrow && module.arrows.getCurrentValue();
      }
   }
   
   public String getLocalizedName() {
      return LanguageManager.getInstance().getLocalizedString("发光", "Glow");
   }

   public String getLocalizedDescription() {
      return LanguageManager.getInstance().getLocalizedString("让实体发光", "Make entities glow");
   }
}