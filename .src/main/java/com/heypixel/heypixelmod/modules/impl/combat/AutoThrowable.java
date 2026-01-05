package com.heypixel.heypixelmod.modules.impl.combat;

import com.heypixel.heypixelmod.Naven;
import com.heypixel.heypixelmod.events.api.EventTarget;
import com.heypixel.heypixelmod.events.api.types.EventType;
import com.heypixel.heypixelmod.events.impl.EventMotion;
import com.heypixel.heypixelmod.modules.Category;
import com.heypixel.heypixelmod.modules.Module;
import com.heypixel.heypixelmod.modules.ModuleInfo;
import com.heypixel.heypixelmod.utils.PacketUtils;
import com.heypixel.heypixelmod.utils.TimeHelper;
import com.heypixel.heypixelmod.utils.rotation.RotationUtils;
import com.heypixel.heypixelmod.values.ValueBuilder;
import com.heypixel.heypixelmod.values.impl.BooleanValue;
import com.heypixel.heypixelmod.values.impl.FloatValue;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

import java.util.Random;
import java.util.stream.StreamSupport;

@ModuleInfo(
        name = "AutoThrowable",
        description = "Automatically throws throwable items like snowballs or eggs at targets.",
        category = Category.COMBAT
)
public class AutoThrowable extends Module {
    private static final Random random = new Random();
    private final Minecraft mc = Minecraft.getInstance();

    private final BooleanValue onlyWhenCombat = ValueBuilder.create(this, "Only Combat")
            .setDefaultBooleanValue(true)
            .build().getBooleanValue();

    private final BooleanValue legitMode = ValueBuilder.create(this, "Legit")
            .setDefaultBooleanValue(false)
            .build().getBooleanValue();

    private final FloatValue prepareTimeMin = ValueBuilder.create(this, "Ready Time Min(ms)")
            .setDefaultFloatValue(100.0F)
            .setFloatStep(50.0F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(1000.0F)
            .build().getFloatValue();

    private final FloatValue prepareTimeMax = ValueBuilder.create(this, "Ready TimeMax(ms)")
            .setDefaultFloatValue(300.0F)
            .setFloatStep(50.0F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(1000.0F)
            .build().getFloatValue();

    private final FloatValue throwWaitTimeMin = ValueBuilder.create(this, "Throw Delay Min(ms)")
            .setDefaultFloatValue(50.0F)
            .setFloatStep(20.0F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(500.0F)
            .build().getFloatValue();

    private final FloatValue throwWaitTimeMax = ValueBuilder.create(this, "Throw Delay Max(ms)")
            .setDefaultFloatValue(200.0F)
            .setFloatStep(20.0F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(500.0F)
            .build().getFloatValue();

    private final FloatValue switchBackTimeMin = ValueBuilder.create(this, "Switch Back TimeMin(ms)")
            .setDefaultFloatValue(300.0F)
            .setFloatStep(50.0F)
            .setMinFloatValue(100.0F)
            .setMaxFloatValue(2000.0F)
            .build().getFloatValue();

    private final FloatValue switchBackTimeMax = ValueBuilder.create(this, "Switch Backt Time Max(ms)")
            .setDefaultFloatValue(700.0F)
            .setFloatStep(50.0F)
            .setMinFloatValue(100.0F)
            .setMaxFloatValue(2000.0F)
            .build().getFloatValue();

    private final FloatValue cooldownTimeMin = ValueBuilder.create(this, "Delay Min(ms)")
            .setDefaultFloatValue(800.0F)
            .setFloatStep(100.0F)
            .setMinFloatValue(500.0F)
            .setMaxFloatValue(5000.0F)
            .build().getFloatValue();

    private final FloatValue cooldownTimeMax = ValueBuilder.create(this, "Delay Max(ms)")
            .setDefaultFloatValue(1500.0F)
            .setFloatStep(100.0F)
            .setMinFloatValue(500.0F)
            .setMaxFloatValue(5000.0F)
            .build().getFloatValue();

    private final TimeHelper timer = new TimeHelper();
    private final TimeHelper cooldownTimer = new TimeHelper();
    private int originalSlot = -1;
    private int throwableSlot = -1;
    private State state = State.IDLE;
    private Entity target = null;
    private long currentStageTime;

    private enum State {
        IDLE,
        PREPARING,
        SWITCHING_TO,
        CHECKING_THROWABLE,
        SWITCHED,
        THROWING,
        SWITCHING_BACK,
        CHECKING_BACK,
        COOLDOWN
    }

    @Override
    public void onEnable() {
        state = State.IDLE;
        originalSlot = -1;
        throwableSlot = -1;
        target = null;
        timer.reset();
        cooldownTimer.reset();
        currentStageTime = 0;
        super.onEnable();
    }

    @EventTarget
    public void onMotion(EventMotion event) {
        LocalPlayer player = mc.player;

        if (event.getType() != EventType.PRE || player == null || mc.level == null || mc.screen != null) {
            return;
        }

        if (!cooldownTimer.delay(currentStageTime) && state == State.COOLDOWN) {
            return;
        }

        if (onlyWhenCombat.getCurrentValue() && !isCombatModuleActive()) {
            resetState();
            return;
        }

        target = getValidTarget();
        if (target == null) {
            resetState();
            return;
        }

        handleThrowing();
    }

    private void handleThrowing() {
        LocalPlayer player = mc.player;
        if (player == null) return;

        switch (state) {
            case IDLE:

                throwableSlot = findThrowableSlot();
                if (throwableSlot == -1 && !isThrowable(player.getOffhandItem())) {
                    resetState();
                    break;
                }

                originalSlot = player.getInventory().selected;

                currentStageTime = getRandomTime(prepareTimeMin.getCurrentValue(), prepareTimeMax.getCurrentValue());
                timer.reset();
                state = State.PREPARING;
                break;

            case PREPARING:
                if (timer.delay(currentStageTime)) {

                    if (isThrowable(player.getOffhandItem())) {
                        currentStageTime = getRandomTime(throwWaitTimeMin.getCurrentValue(), throwWaitTimeMax.getCurrentValue());
                        timer.reset();
                        state = State.SWITCHED;
                    } else if (throwableSlot != -1 && throwableSlot != originalSlot) {

                        switchToThrowable(player);
                        state = State.SWITCHING_TO;
                        timer.reset();
                        currentStageTime = 50;
                    } else {
                        resetState();
                    }
                }
                break;

            case SWITCHING_TO:
                if (timer.delay(currentStageTime)) {
                    state = State.CHECKING_THROWABLE;
                }
                break;

            case CHECKING_THROWABLE:

                if (isHoldingThrowable(player)) {
                    currentStageTime = getRandomTime(throwWaitTimeMin.getCurrentValue(), throwWaitTimeMax.getCurrentValue());
                    timer.reset();
                    state = State.SWITCHED;
                } else {

                    switchToThrowable(player);
                    timer.reset();
                    currentStageTime = 50;
                    state = State.SWITCHING_TO;
                }
                break;

            case SWITCHED:
                if (timer.delay(currentStageTime)) {

                    InteractionHand hand = isThrowable(player.getOffhandItem()) ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
                    if (legitMode.getCurrentValue()) {
                        simulateRightClick();
                    } else {
                        PacketUtils.sendSequencedPacket(id -> new ServerboundUseItemPacket(hand, id));
                        player.swing(hand);
                    }
                    state = State.THROWING;
                    timer.reset();
                    currentStageTime = 50;
                }
                break;

            case THROWING:
                if (timer.delay(currentStageTime)) {
                    if (originalSlot != -1 && originalSlot != player.getInventory().selected && !isThrowable(player.getOffhandItem())) {
                        currentStageTime = getRandomTime(switchBackTimeMin.getCurrentValue(), switchBackTimeMax.getCurrentValue());
                        timer.reset();
                        state = State.SWITCHING_BACK;
                    } else {
                        finishThrow();
                    }
                }
                break;

            case SWITCHING_BACK:
                if (timer.delay(currentStageTime) && originalSlot != -1) {
                    switchToOriginal(player);
                    timer.reset();
                    currentStageTime = 50;
                    state = State.CHECKING_BACK;
                }
                break;

            case CHECKING_BACK:
                if (player.getInventory().selected == originalSlot) {
                    finishThrow();
                } else {

                    switchToOriginal(player);
                    timer.reset();
                    currentStageTime = 50;
                    state = State.SWITCHING_BACK;
                }
                break;

            case COOLDOWN:
                state = State.IDLE;
                break;
        }
    }

    private void switchToThrowable(LocalPlayer player) {
        if (legitMode.getCurrentValue()) {
            simulateKeyPress(getSlotKey(throwableSlot));
        } else {
            player.getInventory().selected = throwableSlot;
        }
    }

    private void switchToOriginal(LocalPlayer player) {
        if (legitMode.getCurrentValue()) {
            simulateKeyPress(getSlotKey(originalSlot));
        } else {
            player.getInventory().selected = originalSlot;
        }
    }

    private void finishThrow() {
        currentStageTime = getRandomTime(cooldownTimeMin.getCurrentValue(), cooldownTimeMax.getCurrentValue());
        cooldownTimer.reset();
        state = State.COOLDOWN;
    }

    private void resetState() {
        state = State.IDLE;
        originalSlot = -1;
        throwableSlot = -1;
        target = null;
        timer.reset();
    }

    private long getRandomTime(float min, float max) {
        if (min >= max) return (long) min;
        return (long) (min + random.nextFloat() * (max - min));
    }

    private int getSlotKey(int slot) {
        return GLFW.GLFW_KEY_1 + slot;
    }

    private void simulateKeyPress(int keyCode) {

        KeyMapping key = mc.options.keyHotbarSlots[keyCode - GLFW.GLFW_KEY_1];
        if (key == null) return;

        InputConstants.Key inputKey = key.getKey();

        int pressDelay = random.nextInt(20) + 10;

        mc.tell(() -> {
            try {

                KeyMapping.set(inputKey, true);
                KeyMapping.click(inputKey);

                long start = System.currentTimeMillis();
                while (System.currentTimeMillis() - start < pressDelay) {
                    Thread.yield();
                }

                KeyMapping.set(inputKey, false);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void simulateRightClick() {
        KeyMapping useKey = mc.options.keyUse;
        InputConstants.Key inputKey = useKey.getKey();

        int clickDelay = random.nextInt(30) + 20;

        mc.tell(() -> {
            try {

                KeyMapping.set(inputKey, true);
                KeyMapping.click(inputKey);

                long start = System.currentTimeMillis();
                while (System.currentTimeMillis() - start < clickDelay) {
                    Thread.yield();
                }

                KeyMapping.set(inputKey, false);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private boolean isCombatModuleActive() {
        Module killaura = Naven.getInstance().getModuleManager().getModule(Aura.class);
        Module aimAssist = Naven.getInstance().getModuleManager().getModule(AimAssist.class);
        return (killaura != null && killaura.isEnabled()) || (aimAssist != null && aimAssist.isEnabled());
    }

    private Entity getValidTarget() {

        Module aimAssistModule = Naven.getInstance().getModuleManager().getModule(AimAssist.class);
        if (aimAssistModule instanceof AimAssist aimAssist && aimAssist.isEnabled()) {

            Entity aimTarget = getAimAssistTarget(aimAssist);
            if (aimTarget != null && isValidTarget(aimTarget)) {
                return aimTarget;
            }
        }

        Module killaura = Naven.getInstance().getModuleManager().getModule(Aura.class);
        if (killaura != null && killaura.isEnabled() && Aura.targets != null && !Aura.targets.isEmpty()) {
            for (Entity entity : Aura.targets) {
                if (isValidTarget(entity)) {
                    return entity;
                }
            }
        }

        return null;
    }

    private Entity getAimAssistTarget(AimAssist aimAssist) {
        if (mc.level == null || mc.player == null) return null;

        return StreamSupport.stream(mc.level.entitiesForRendering().spliterator(), false)
                .filter(entity -> entity instanceof Entity)
                .filter(aimAssist::isValidAttack)
                .findFirst()
                .orElse(null);
    }

    private boolean isValidTarget(Entity entity) {
        if (entity == null || entity == mc.player || !entity.isAlive()) {
            return false;
        }
        double distance = entity.distanceTo(mc.player);

        return distance <= 10.0 && RotationUtils.inFov(entity, 90.0F);
    }

    private int findThrowableSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (isThrowable(stack)) {
                return i;
            }
        }
        return -1;
    }

    private boolean isThrowable(ItemStack stack) {
        if (stack.isEmpty()) return false;
        Item item = stack.getItem();
        return item == Items.SNOWBALL || item == Items.EGG;
    }

    private boolean isHoldingThrowable(LocalPlayer player) {
        return isThrowable(player.getMainHandItem()) || isThrowable(player.getOffhandItem());
    }
}