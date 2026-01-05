package com.heypixel.heypixelmod.obsoverlay.modules.impl.misc;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMotion;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.move.LongJump;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Fireball;
import com.heypixel.heypixelmod.obsoverlay.ui.LanguageManager;

@ModuleInfo(
        name = "防火焰弹",
        description = "放置火焰弹伤害你",
        category = Category.MISC
)
public class AntiFireball extends Module {
   // 保护自己：当自己刚发射火焰弹的几秒内不拦截，避免打到自己发出的火焰弹
   public BooleanValue protectSelf = ValueBuilder.create(this, "自用检测")
           .setDefaultBooleanValue(true)
           .build()
           .getBooleanValue();

   public FloatValue protectDelaySeconds = ValueBuilder.create(this, "保护延迟(s)")
           .setDefaultFloatValue(3.0F)
           .setFloatStep(0.5F)
           .setMinFloatValue(0.5F)
           .setMaxFloatValue(10.0F)
           .build()
           .getFloatValue();

   private long selfProtectUntilMs = 0L;

   public String getLocalizedName() {
      return LanguageManager.getInstance().getLocalizedString("防火焰弹", "Anti Fireball");
   }

   public String getLocalizedDescription() {
      return LanguageManager.getInstance().getLocalizedString("放置火焰弹伤害你", "Prevent fireballs from damaging you");
   }

   @EventTarget
   public void onMotion(EventMotion e) {
      if (!Naven.getInstance().getModuleManager().getModule(LongJump.class).isEnabled()) {
         if (e.getType() == EventType.PRE) {
            if (mc == null || mc.level == null || mc.player == null || mc.gameMode == null) {
               return;
            }

            long now = System.currentTimeMillis();

            // 若开启保护自己逻辑：
            if (protectSelf.getCurrentValue()) {
               // 一旦附近检测到归属为自己的火焰弹，启动保护计时
               boolean hasOwnFireballNearby = StreamSupport
                       .stream(mc.level.entitiesForRendering().spliterator(), true)
                       .anyMatch(entityx -> entityx instanceof Fireball
                               && ((Fireball) entityx).getOwner() == mc.player
                               && mc.player.distanceTo(entityx) < 10.0F);

               if (hasOwnFireballNearby) {
                  selfProtectUntilMs = now + (long)(protectDelaySeconds.getCurrentValue() * 1000L);
               }

               // 在保护时间内直接跳过拦截
               if (now < selfProtectUntilMs) {
                  return;
               }
            }

            // 常规拦截最近的火焰弹（不考虑来源）
            Stream<Entity> stream = StreamSupport.stream(mc.level.entitiesForRendering().spliterator(), true);
            Optional<Fireball> fireball = stream
                    .filter(entityx -> entityx instanceof Fireball && mc.player.distanceTo(entityx) < 6.0F)
                    .map(entityx -> (Fireball) entityx)
                    .findFirst();
            if (!fireball.isPresent()) {
               return;
            }

            Fireball entity = fireball.get();
            mc.gameMode.attack(mc.player, entity);
            mc.player.swing(InteractionHand.MAIN_HAND);
         }
      }
   }
}
