package com.heypixel.heypixelmod.modules.impl.combat;

import com.heypixel.heypixelmod.events.api.EventTarget;
import com.heypixel.heypixelmod.events.api.types.EventType;
import com.heypixel.heypixelmod.events.impl.EventClick;
import com.heypixel.heypixelmod.events.impl.EventPacket;
import com.heypixel.heypixelmod.events.impl.EventRunTicks;
import com.heypixel.heypixelmod.modules.Category;
import com.heypixel.heypixelmod.modules.Module;
import com.heypixel.heypixelmod.modules.ModuleInfo;
import com.heypixel.heypixelmod.utils.BlockUtils;
import com.heypixel.heypixelmod.utils.DamageUtils;
import com.heypixel.heypixelmod.utils.PacketUtils;
import com.heypixel.heypixelmod.utils.Vector2f;
import com.heypixel.heypixelmod.utils.rotation.RotationManager;
import com.heypixel.heypixelmod.utils.rotation.RotationUtils;
import com.heypixel.heypixelmod.values.ValueBuilder;
import com.heypixel.heypixelmod.values.impl.BooleanValue;
import com.heypixel.heypixelmod.values.impl.FloatValue;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.PosRot;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.item.EndCrystalItem;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

@ModuleInfo(
        name = "CrystalAura",
        category = Category.COMBAT,
        description = "Automatically place and break end crystals for skywars"
)
public class CrystalAura extends Module {
   public static Vector2f rotations;
   private Entity entity;
   private Entity placeTarget;
   private BlockPos placePos;
   private boolean shouldAttackAfterPlace = false;

   BooleanValue breakOption = ValueBuilder.create(this, "Break").setDefaultBooleanValue(true).build().getBooleanValue();
   BooleanValue place = ValueBuilder.create(this, "Place").setDefaultBooleanValue(false).build().getBooleanValue();
   BooleanValue breakOnPacket = ValueBuilder.create(this, "Break on Packet (Danger)").setDefaultBooleanValue(false).build().getBooleanValue();
   FloatValue fov = ValueBuilder.create(this, "Fov")
           .setDefaultFloatValue(360.0F)
           .setFloatStep(1.0F)
           .setMinFloatValue(10.0F)
           .setMaxFloatValue(360.0F)
           .build()
           .getFloatValue();

   @EventTarget
   public void onPacket(EventPacket e) {
      if (e.getType() == EventType.RECEIVE && e.getPacket() instanceof ClientboundAddEntityPacket && this.breakOnPacket.getCurrentValue()) {
         ClientboundAddEntityPacket packet = (ClientboundAddEntityPacket) e.getPacket();
         if (packet.getType() == EntityType.END_CRYSTAL) {
            EndCrystal pTarget = new EndCrystal(mc.level, packet.getX(), packet.getY(), packet.getZ());
            pTarget.setId(packet.getId());
            if (mc.player.distanceTo(pTarget) <= 4.0F) {
               Vector2f rotations = RotationUtils.getRotations(pTarget);
               mc.getConnection()
                       .send(new PosRot(mc.player.getX(), mc.player.getY(), mc.player.getZ(), rotations.getX(), rotations.getY(), mc.player.onGround()));

               PacketUtils.sendSequencedPacket(id -> new ServerboundUseItemOnPacket(InteractionHand.MAIN_HAND, new BlockHitResult(new Vec3(pTarget.getX(), pTarget.getY(), pTarget.getZ()), Direction.DOWN, pTarget.blockPosition(), false), id));

               float currentYaw = mc.player.getYRot();
               float currentPitch = mc.player.getXRot();
               mc.player.setYRot(RotationManager.rotations.x);
               mc.player.setXRot(RotationManager.rotations.y);
               mc.getConnection().send(ServerboundInteractPacket.createAttackPacket(pTarget, false));
               mc.player.swing(InteractionHand.MAIN_HAND);
               mc.player.setYRot(currentYaw);
               mc.player.setXRot(currentPitch);
            }
         }
      }
   }

   @EventTarget
   public void onEarlyTick(EventRunTicks e) {
      if (e.getType() == EventType.PRE && mc.player != null && mc.level != null) {
         this.entity = null;
         this.placeTarget = null;
         this.placePos = null;
         rotations = null;

         if (this.breakOption.getCurrentValue()) {
            Optional<Entity> anyCrystal = StreamSupport.<Entity>stream(mc.level.entitiesForRendering().spliterator(), true)
                    .filter(entityx -> entityx instanceof EndCrystal)
                    .findAny();
            if (anyCrystal.isPresent()) {
               Entity entity = anyCrystal.get();
               Vector2f rots = RotationUtils.getRotations(entity);
               double minDistance = RotationUtils.getMinDistance(entity, rots);

               boolean hasLineOfSight = mc.level.clip(new ClipContext(mc.player.getEyePosition(), entity.position(), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player)).getType() == net.minecraft.world.phys.HitResult.Type.MISS;

               if (minDistance <= 3.0 && RotationUtils.inFov(entity, this.fov.getCurrentValue() / 2.0F) && hasLineOfSight) {
                  rotations = rots;
                  this.entity = entity;
               }
            }
         }

         if (this.shouldAttackAfterPlace) {
            Optional<Entity> justPlacedCrystal = StreamSupport.<Entity>stream(mc.level.entitiesForRendering().spliterator(), true)
                    .filter(entityx -> entityx instanceof EndCrystal && entityx.blockPosition().equals(this.placePos))
                    .findFirst();
            if (justPlacedCrystal.isPresent()) {
               EndCrystal crystal = (EndCrystal) justPlacedCrystal.get();

               Vector2f rots = RotationUtils.getRotations(crystal);
               rotations = rots;

               mc.getConnection().send(ServerboundInteractPacket.createAttackPacket(crystal, false));
               mc.player.swing(InteractionHand.MAIN_HAND);

               this.shouldAttackAfterPlace = false;
            }
         }

         if (this.place.getCurrentValue()) {
            if (mc.player.getMainHandItem().getItem() instanceof EndCrystalItem) {
               Optional<AbstractClientPlayer> anyPlayer = mc.level.players().stream()
                       .filter(player -> player != mc.player && player.isAlive() && !player.isDeadOrDying())
                       .min((a, b) -> Double.compare(mc.player.distanceToSqr(a), mc.player.distanceToSqr(b)));
               if (anyPlayer.isPresent()) {
                  this.placeTarget = anyPlayer.get();
                  AABB targetBoundingBox = this.placeTarget.getBoundingBox();

                  List<BlockPos> potentialPositions = new ArrayList<>();
                  BlockPos targetBlock = this.placeTarget.blockPosition();

                  potentialPositions.add(targetBlock.north().below());
                  potentialPositions.add(targetBlock.south().below());
                  potentialPositions.add(targetBlock.east().below());
                  potentialPositions.add(targetBlock.west().below());

                  BlockPos bestPlacePos = null;
                  double maxDamage = -1.0;

                  for (BlockPos pos : potentialPositions) {
                     AABB crystalBox = new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1.0, pos.getY() + 2.0, pos.getZ() + 1.0);

                     boolean hasLineOfSightToPlace = mc.level.clip(new ClipContext(mc.player.getEyePosition(), new Vec3(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player)).getType() == net.minecraft.world.phys.HitResult.Type.MISS;

                     if (!targetBoundingBox.intersects(crystalBox) && hasLineOfSightToPlace) {
                        double damage = DamageUtils.getExplosionDamage((LivingEntity) this.placeTarget, pos, DamageUtils.ExplosionType.CRYSTAL, null);
                        if (damage > maxDamage) {
                           maxDamage = damage;
                           bestPlacePos = pos;
                        }
                     }
                  }

                  if (bestPlacePos != null) {
                     this.placePos = bestPlacePos;

                     if (mc.player.distanceToSqr(this.placePos.getX() + 0.5, this.placePos.getY() + 1.0, this.placePos.getZ() + 0.5) <= 20.25) {
                        Vec3 hitVec = new Vec3(this.placePos.getX() + 0.5, this.placePos.getY() + 1.0, this.placePos.getZ() + 0.5);
                        rotations = RotationUtils.getRotationsVector(hitVec);
                     }
                  }
               }
            }
         }
      }
   }

   @EventTarget
   public void onLateTick(EventRunTicks e) {
      if (e.getType() == EventType.POST && this.place.getCurrentValue() && rotations != null && this.placePos != null) {
         if (mc.player.getMainHandItem().getItem() instanceof EndCrystalItem) {
            Vec3 hitVec = new Vec3(this.placePos.getX() + 0.5, this.placePos.getY() + 1.0, this.placePos.getZ() + 0.5);

            BlockUtils.interactWithBlock(
                    new BlockHitResult(
                            hitVec,
                            Direction.UP,
                            this.placePos,
                            false
                    ),
                    true
            );

            this.shouldAttackAfterPlace = true;
         }
      }
   }

   @EventTarget
   public void onClick(EventClick e) {
      if (this.breakOption.getCurrentValue() && this.entity != null) {
         float currentYaw = mc.player.getYRot();
         float currentPitch = mc.player.getXRot();
         mc.player.setYRot(rotations.x);
         mc.player.setXRot(rotations.y);
         mc.getConnection().send(ServerboundInteractPacket.createAttackPacket(this.entity, false));
         mc.player.swing(InteractionHand.MAIN_HAND);
         mc.player.setYRot(currentYaw);
         mc.player.setXRot(currentPitch);
         this.entity = null;
      }
   }
}
