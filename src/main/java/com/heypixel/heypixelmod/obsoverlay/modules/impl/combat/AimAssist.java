package com.heypixel.heypixelmod.obsoverlay.modules.impl.combat;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRunTicks;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.misc.Teams;
import com.heypixel.heypixelmod.obsoverlay.utils.BlinkingPlayer;
import com.heypixel.heypixelmod.obsoverlay.utils.FriendManager;
import com.heypixel.heypixelmod.obsoverlay.ui.LanguageManager;
import com.heypixel.heypixelmod.obsoverlay.utils.Vector2f;
import com.heypixel.heypixelmod.obsoverlay.utils.rotation.RotationManager;
import com.heypixel.heypixelmod.obsoverlay.utils.rotation.RotationUtils;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.ModeValue;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Squid;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

@ModuleInfo(
   name = "自动瞄准",
   description = "自动瞄准最近的实体",
   category = Category.COMBAT
)
public class AimAssist extends Module {
   BooleanValue attackPlayer = ValueBuilder.create(this, "攻击玩家").setDefaultBooleanValue(true).build().getBooleanValue();
   BooleanValue attackInvisible = ValueBuilder.create(this, "攻击隐形").setDefaultBooleanValue(false).build().getBooleanValue();
   BooleanValue attackAnimals = ValueBuilder.create(this, "攻击动物").setDefaultBooleanValue(false).build().getBooleanValue();
   BooleanValue attackMobs = ValueBuilder.create(this, "攻击生物").setDefaultBooleanValue(false).build().getBooleanValue();
   BooleanValue clickonly = ValueBuilder.create(this, "仅点击").setDefaultBooleanValue(true).build().getBooleanValue();
   BooleanValue slient = ValueBuilder.create(this, "静默瞄准").setDefaultBooleanValue(true).build().getBooleanValue();
   FloatValue rotateSpeed = ValueBuilder.create(this, "转头速度")
      .setDefaultFloatValue(10.0F)
      .setFloatStep(1.0F)
      .setMinFloatValue(1.0F)
      .setMaxFloatValue(90.0F)
      .build()
      .getFloatValue();
   FloatValue aimRange = ValueBuilder.create(this, "瞄准范围")
      .setDefaultFloatValue(5.0F)
      .setFloatStep(0.1F)
      .setMinFloatValue(1.0F)
      .setMaxFloatValue(6.0F)
      .build()
      .getFloatValue();
   FloatValue fov = ValueBuilder.create(this, "角度")
      .setDefaultFloatValue(360.0F)
      .setFloatStep(1.0F)
      .setMinFloatValue(10.0F)
      .setMaxFloatValue(360.0F)
      .build()
      .getFloatValue();
   ModeValue priority = ValueBuilder.create(this, "优先权").setModes("生命值", "角度", "范围", "无").build().getModeValue();
   public Vector2f targetRotation = new Vector2f();
   public boolean working = false;
   public boolean slientaim = this.slient.currentValue;
   
   public String getLocalizedName() {
      return LanguageManager.getInstance().getLocalizedString("自动瞄准", "Aim Assist");
   }

   public String getLocalizedDescription() {
      return LanguageManager.getInstance().getLocalizedString("自动瞄准最近的实体", "Automatically aim at the nearest entity");
   }

   @EventTarget
   public void onMotion(EventRunTicks e) {
      if (e.getType() == EventType.PRE && mc.player != null) {
         if (this.clickonly.currentValue && !mc.options.keyAttack.isDown()) {
            this.working = false;
            return;
         }

         Entity target = this.getTarget();
         float targetYaw;
         float targetPitch;
         if (target != null) {
            Vector2f rotations = RotationUtils.getRotations(target);
            targetYaw = mc.player.getYRot() + RotationUtils.getAngleDifference(rotations.getX(), mc.player.getYRot());
            targetPitch = rotations.getY();
            this.working = true;
         } else {
            targetYaw = mc.player.getYRot();
            targetPitch = mc.player.getXRot();
            if (this.targetRotation.getX() % 360.0F == targetYaw % 360.0F) {
               this.working = false;
            }
         }

         if (this.working) {
            this.targetRotation.setX(RotationUtils.rotateToYaw(this.rotateSpeed.getCurrentValue(), this.targetRotation.getX(), targetYaw));
            this.targetRotation.setY(targetPitch);
         } else {
            this.targetRotation.setX(targetYaw);
            this.targetRotation.setY(targetPitch);
         }
      }
   }

   public boolean isValidTarget(Entity entity) {
      if (entity == mc.player) {
         return false;
      } else if (entity instanceof LivingEntity living) {
         if (living instanceof BlinkingPlayer) {
            return false;
         } else {
            AntiBots module = (AntiBots)Naven.getInstance().getModuleManager().getModule(AntiBots.class);
            if (module == null || !module.isEnabled() || !AntiBots.isBot(entity) && !AntiBots.isBedWarsBot(entity)) {
               if (Teams.isSameTeam(living)) {
                  return false;
               } else if (FriendManager.isFriend(living)) {
                  return false;
               } else if (living.isDeadOrDying() || living.getHealth() <= 0.0F) {
                  return false;
               } else if (entity instanceof ArmorStand) {
                  return false;
               } else if (entity.isInvisible() && !this.attackInvisible.getCurrentValue()) {
                  return false;
               } else if (entity instanceof Player && !this.attackPlayer.getCurrentValue()) {
                  return false;
               } else if (!(entity instanceof Player) || !((double)entity.getBbWidth() < 0.5) && !living.isSleeping()) {
                  if ((entity instanceof Mob || entity instanceof Slime || entity instanceof Bat || entity instanceof AbstractGolem)
                     && !this.attackMobs.getCurrentValue()) {
                     return false;
                  } else if ((entity instanceof Animal || entity instanceof Squid) && !this.attackAnimals.getCurrentValue()) {
                     return false;
                  } else {
                     return entity instanceof Villager && !this.attackAnimals.getCurrentValue() ? false : !(entity instanceof Player) || !entity.isSpectator();
                  }
               } else {
                  return false;
               }
            } else {
               return false;
            }
         }
      } else {
         return false;
      }
   }

   public boolean isValidAttack(Entity entity) {
      if (!this.isValidTarget(entity)) {
         return false;
      } else {
         Vec3 closestPoint = RotationUtils.getClosestPoint(mc.player.getEyePosition(), entity.getBoundingBox());
         if (closestPoint.distanceTo(mc.player.getEyePosition()) > (double)this.aimRange.getCurrentValue()) {
            return false;
         } else {
            boolean b = RotationUtils.inFoV(entity, this.fov.getCurrentValue() / 2.0F);
            if (entity.getName().getString().equals("Standing")) {
               System.out.println(b);
            }

            return b;
         }
      }
   }

   private Entity getTarget() {
      Stream<Entity> stream = StreamSupport.<Entity>stream(mc.level.entitiesForRendering().spliterator(), true)
         .filter(entity -> entity instanceof Entity)
         .filter(this::isValidAttack);
      List<Entity> possibleTargets = stream.collect(Collectors.toList());
      if (this.priority.isCurrentMode("范围")) {
         possibleTargets.sort(Comparator.comparingDouble(o -> (double)o.distanceTo(mc.player)));
      } else if (this.priority.isCurrentMode("角度")) {
         possibleTargets.sort(
            Comparator.comparingDouble(o -> (double)RotationUtils.getDistanceBetweenAngles(RotationManager.rotations.x, RotationUtils.getRotations(o).x))
         );
      } else if (this.priority.isCurrentMode("生命值")) {
         possibleTargets.sort(Comparator.comparingDouble(o -> o instanceof LivingEntity living ? (double)living.getHealth() : 0.0));
      }

      return possibleTargets.isEmpty() ? null : possibleTargets.get(0);
   }
}