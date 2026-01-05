package com.heypixel.heypixelmod.obsoverlay.modules.impl.combat;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRunTicks;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import com.heypixel.heypixelmod.obsoverlay.ui.LanguageManager;

import java.util.List;

@ModuleInfo(
        name = "SnowballAura",
        description = "Auto trrows snowball",
        category = Category.COMBAT
)
public class SnowballAura extends Module {
    private final Minecraft mc = Minecraft.getInstance();
    private double detectionRange = 8.0;
    private double throwRange = 6.0;
    private int delay = 0;
    private boolean ignoreTeammates = true;
    private boolean targetPlayers = true;
    private boolean targetMonsters = true;
    private boolean autoSwitch = true;
    private boolean predictiveAiming = true;
    private boolean useOffhand = true;

    private int tickCounter = 0;
    private int lastSlot = -1;

    @Override
    public void onEnable() {
        tickCounter = 0;
        lastSlot = -1;
    }

    @Override
    public void onDisable() {
        if (autoSwitch && lastSlot != -1 && mc.player != null) {
            mc.player.getInventory().selected = lastSlot;
        }
    }

    @EventTarget
    public void onTick(EventRunTicks event) {
        if (event.getType() != EventType.POST || mc.player == null || mc.level == null) return;

        LocalPlayer player = mc.player;
        if (tickCounter < delay) {
            tickCounter++;
            return;
        }
        tickCounter = 0;
        Entity target = findTarget(player);
        if (target == null) return;
        if (player.distanceTo(target) > throwRange) return;
        InteractionHand hand = findSnowballHand();
        if (hand == null) return;
        if (hand == InteractionHand.MAIN_HAND && autoSwitch) {
            int snowballSlot = findSnowballSlot();
            if (snowballSlot == -1) return;

            if (lastSlot == -1) {
                lastSlot = player.getInventory().selected;
            }
            player.getInventory().selected = snowballSlot;
        }
        throwSnowballWithAnimation(player, target, hand);
    }

    private Entity findTarget(LocalPlayer player) {
        AABB detectionBox = new AABB(
                player.getX() - detectionRange,
                player.getY() - detectionRange,
                player.getZ() - detectionRange,
                player.getX() + detectionRange,
                player.getY() + detectionRange,
                player.getZ() + detectionRange
        );

        List<Entity> allEntities = mc.level.getEntities(null, detectionBox);

        return allEntities.stream()
                .filter(entity -> isValidTarget(player, entity))
                .min((a, b) -> Double.compare(player.distanceToSqr(a), player.distanceToSqr(b)))
                .orElse(null);
    }

    private boolean isValidTarget(LocalPlayer player, Entity target) {
        if (target == player || !target.isAlive()) return false;
        if (player.distanceTo(target) > detectionRange) return false;

        if (target instanceof Player) {
            Player playerTarget = (Player) target;
            return targetPlayers && (!ignoreTeammates || !isTeammate(player, playerTarget));
        } else if (target instanceof Monster) {
            return targetMonsters;
        }
        return false;
    }

    private boolean isTeammate(LocalPlayer player, Player target) {
        return player.getTeam() != null &&
                target.getTeam() != null &&
                player.getTeam().getName().equals(target.getTeam().getName());
    }

    private InteractionHand findSnowballHand() {
        if (mc.player == null) return null;

        if (useOffhand && mc.player.getOffhandItem().getItem() == Items.SNOWBALL) {
            return InteractionHand.OFF_HAND;
        }

        if (mc.player.getMainHandItem().getItem() == Items.SNOWBALL) {
            return InteractionHand.MAIN_HAND;
        }

        if (autoSwitch && findSnowballSlot() != -1) {
            return InteractionHand.MAIN_HAND;
        }

        return null;
    }

    private int findSnowballSlot() {
        LocalPlayer player = mc.player;
        if (player == null) return -1;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() == Items.SNOWBALL) {
                return i;
            }
        }
        return -1;
    }

    private void throwSnowballWithAnimation(LocalPlayer player, Entity target, InteractionHand hand) {
        if (mc.gameMode == null || player == null) return;
        float originalYaw = player.getYRot();
        float originalPitch = player.getXRot();
        aimAtTarget(player, target);
        player.swing(hand);
        mc.gameMode.useItem(player, hand);
        player.setYRot(originalYaw);
        player.setXRot(originalPitch);
    }

    private void aimAtTarget(LocalPlayer player, Entity target) {
        if (player == null || target == null) return;

        Vec3 targetPos = predictiveAiming ? calculatePredictedPosition(target) : target.getEyePosition(1.0F);
        Vec3 playerPos = player.getEyePosition(1.0F);

        double dx = targetPos.x - playerPos.x;
        double dy = targetPos.y - playerPos.y;
        double dz = targetPos.z - playerPos.z;

        double yaw = Math.atan2(dz, dx) * 180.0 / Math.PI - 90.0;
        double distance = Math.sqrt(dx * dx + dz * dz);
        double pitch = Math.atan2(dy, distance) * 180.0 / Math.PI;

        player.setYRot((float) yaw);
        player.setXRot((float) -pitch);
    }

    private Vec3 calculatePredictedPosition(Entity target) {
        Vec3 currentPos = target.getEyePosition(1.0F);
        Vec3 velocity = target.getDeltaMovement();
        double distance = mc.player.distanceTo(target);
        double timeToTarget = distance / 1.5;

        return new Vec3(
                currentPos.x + velocity.x * timeToTarget,
                currentPos.y + velocity.y * timeToTarget,
                currentPos.z + velocity.z * timeToTarget
        );
    }

    public String getLocalizedName() {
      return LanguageManager.getInstance().getLocalizedString("SnowballAura", "SnowballAura");
   }

   public String getLocalizedDescription() {
      return LanguageManager.getInstance().getLocalizedString("Auto trrows snowball", "Auto throws snowball");
   }

    public double getDetectionRange() { return detectionRange; }
    public void setDetectionRange(double detectionRange) { this.detectionRange = detectionRange; }
    public double getThrowRange() { return throwRange; }
    public void setThrowRange(double throwRange) { this.throwRange = throwRange; }
    public int getDelay() { return delay; }
    public void setDelay(int delay) { this.delay = delay; }
    public boolean isIgnoreTeammates() { return ignoreTeammates; }
    public void setIgnoreTeammates(boolean ignoreTeammates) { this.ignoreTeammates = ignoreTeammates; }
    public boolean isTargetPlayers() { return targetPlayers; }
    public void setTargetPlayers(boolean targetPlayers) { this.targetPlayers = targetPlayers; }
    public boolean isTargetMonsters() { return targetMonsters; }
    public void setTargetMonsters(boolean targetMonsters) { this.targetMonsters = targetMonsters; }
    public boolean isAutoSwitch() { return autoSwitch; }
    public void setAutoSwitch(boolean autoSwitch) { this.autoSwitch = autoSwitch; }
    public boolean isPredictiveAiming() { return predictiveAiming; }
    public void setPredictiveAiming(boolean predictiveAiming) { this.predictiveAiming = predictiveAiming; }
    public boolean isUseOffhand() { return useOffhand; }
    public void setUseOffhand(boolean useOffhand) { this.useOffhand = useOffhand; }
}
