package com.heypixel.heypixelmod.modules.impl.move;

import com.heypixel.heypixelmod.events.api.EventTarget;
import com.heypixel.heypixelmod.events.api.types.EventType;
import com.heypixel.heypixelmod.events.impl.EventMoveInput;
import com.heypixel.heypixelmod.events.impl.EventPacket;
import com.heypixel.heypixelmod.events.impl.EventRunTicks;
import com.heypixel.heypixelmod.modules.Category;
import com.heypixel.heypixelmod.modules.Module;
import com.heypixel.heypixelmod.modules.ModuleInfo;
import com.heypixel.heypixelmod.ui.ClickGUI;
import com.heypixel.heypixelmod.values.ValueBuilder;
import com.heypixel.heypixelmod.values.impl.BooleanValue;
import com.heypixel.heypixelmod.values.impl.ModeValue;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;

import java.util.ArrayList;
import java.util.List;

@ModuleInfo(
        name = "InventoryMove",
        description = "Inventory move but with aca maybe banned",
        category = Category.MOVEMENT
)
public class InventoryMove extends Module {
    private final Minecraft minecraft = Minecraft.getInstance();

    public ModeValue mode = ValueBuilder.create(this, "Mode")
            .setModes("Normal", "Hypixel")
            .build()
            .getModeValue();

    public BooleanValue sneak = ValueBuilder.create(this, "Sneak")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();

    public BooleanValue sprint = ValueBuilder.create(this, "Sprint")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();
    private int tick = 0;
    private double dist = 0;
    private boolean c16 = false;
    private boolean c0d = false;
    private boolean OpenInventory = false;
    private ServerboundContainerClosePacket pc = null;
    private ServerboundPlayerCommandPacket c16C = null;
    public static List<Packet<?>> InvPacketList = new ArrayList<>();

    private boolean wasSprintingBeforeGui = false;
    private boolean isInGui = false;
    @EventTarget
    public void onPacket(EventPacket event) {
        if (event.getType() == EventType.PRE && mode.isCurrentMode("Hypixel")) {
            Packet<?> packet = event.getPacket();

            if (packet instanceof ServerboundMovePlayerPacket) {
                if ((minecraft.screen instanceof ContainerScreen) && tick > 0) {
                    InvPacketList.add(packet);
                    event.setCancelled(true);
                }
            }

            if (packet instanceof ServerboundPlayerCommandPacket p) {
                if (c16 && p.getAction() == ServerboundPlayerCommandPacket.Action.OPEN_INVENTORY) {
                    event.setCancelled(true);
                }
                c16 = true;
            }

            if (packet instanceof ServerboundContainerClickPacket && (tick > 0 || OpenInventory)) {
                InvPacketList.add(packet);
                event.setCancelled(true);
            }

            if (packet instanceof ServerboundContainerClosePacket) {
                if (c0d && !(tick > 1) && OpenInventory) {
                    event.setCancelled(true);
                } else {
                    if (!InvPacketList.isEmpty()) {
                        event.setCancelled(true);
                        for (Packet<?> p : InvPacketList) {
                            minecraft.getConnection().send(p);
                        }
                        InvPacketList.clear();
                        minecraft.getConnection().send(packet);
                    }
                }
                c0d = true;
            }
        }
    }

    @EventTarget(1)
    public void handleMoveInput(EventMoveInput event) {
        if (!this.isMovementAllowed()) {
            if (this.isInGui) {
                this.wasSprintingBeforeGui = false;
                this.isInGui = false;
            }

            if (mode.isCurrentMode("Hypixel")) {
                if (minecraft.screen instanceof ContainerScreen) {
                    if (dist / tick > 0.05) {
                        if (!InvPacketList.isEmpty()) {
                            for (Packet<?> p : InvPacketList) {
                                minecraft.getConnection().send(p);
                            }
                            InvPacketList.clear();
                        }
                        tick = 0;
                        dist = 0;
                    } else if (tick > 0) {
                        if (!InvPacketList.isEmpty()) {
                            for (Packet<?> p : InvPacketList) {
                                minecraft.getConnection().send(p);
                            }
                            InvPacketList.clear();
                        }
                        tick = 1;
                        dist = 0;
                    }
                }
            }
        } else {
            if (!this.isInGui) {
                this.isInGui = true;
                if (this.minecraft.player != null) {
                    this.wasSprintingBeforeGui = this.minecraft.player.isSprinting();
                    if (!this.sprint.getCurrentValue() && this.minecraft.player.isSprinting()) {
                        this.minecraft.player.setSprinting(false);
                    }
                }
            }

            event.setForward(this.calculateForwardMovement());
            event.setStrafe(this.calculateStrafeMovement());
            event.setJump(this.isKeyActive(this.minecraft.options.keyJump));

            event.setSneak(this.sneak.getCurrentValue() && this.isKeyActive(this.minecraft.options.keyShift));
        }
    }

    @EventTarget
    public void processTick(EventRunTicks event) {
        if (this.isValidTickEvent(event) && this.minecraft.player != null) {
            LocalPlayer player = this.minecraft.player;

            if (mode.isCurrentMode("Hypixel")) {
                c16 = false;
                c0d = false;
                OpenInventory = false;

                if (minecraft.screen instanceof InventoryScreen) {
                    double xDist = player.getX() - player.xOld;
                    double zDist = player.getZ() - player.zOld;
                    double lastDist = Math.sqrt(xDist * xDist + zDist * zDist);
                    OpenInventory = true;

                    if (tick == 1) {
                        minecraft.getConnection().send(new ServerboundPlayerCommandPacket(
                                player, ServerboundPlayerCommandPacket.Action.OPEN_INVENTORY));
                    }

                    if (dist / tick > 0.00) {
                        if (tick == ((dist / tick > 0.45) ? 2 : 3)) {
                            if (!InvPacketList.isEmpty()) {
                                for (Packet<?> p : InvPacketList) {
                                    minecraft.getConnection().send(p);
                                }
                                InvPacketList.clear();
                            }
                        }
                        if (tick > ((dist / tick > 0.45) ? 2 : 3)) {
                            minecraft.getConnection().send(new ServerboundContainerClosePacket(0));
                            tick = 0;
                            dist = 0;
                        }
                    } else if (tick > 0) {
                        if (!InvPacketList.isEmpty()) {
                            for (Packet<?> p : InvPacketList) {
                                minecraft.getConnection().send(p);
                            }
                            InvPacketList.clear();
                        }
                        tick = 1;
                        dist = 0;
                    }

                    tick++;
                    dist += lastDist;
                } else if (minecraft.screen instanceof ContainerScreen) {
                    double xDist = player.getX() - player.xOld;
                    double zDist = player.getZ() - player.zOld;
                    double lastDist = Math.sqrt(xDist * xDist + zDist * zDist);
                    tick++;
                    dist += lastDist;
                } else {
                    tick = 0;
                    dist = 0;
                }
            }
            if (!this.sprint.getCurrentValue() && player.isSprinting()) {
                player.setSprinting(false);
            }

            if (this.sprint.getCurrentValue() && this.wasSprintingBeforeGui && this.canContinueSprinting(player)) {
                player.setSprinting(true);
            }

            this.adjustPlayerRotation();
        }
    }

    private boolean isKeyActive(KeyMapping keyMapping) {
        return InputConstants.isKeyDown(
                minecraft.getWindow().getWindow(),
                keyMapping.getKey().getValue()
        );
    }

    private boolean isKeyActive(int keyCode) {
        return InputConstants.isKeyDown(
                minecraft.getWindow().getWindow(),
                keyCode
        );
    }

    private boolean canContinueSprinting(LocalPlayer player) {
        boolean isMovingForward = player.input.forwardImpulse > 0.0F;
        boolean isInValidState = player.getHealth() > 0.0F && !player.isInWater() && !player.isInLava() && !player.isShiftKeyDown() && !player.isPassenger() && !player.input.jumping;
        return isMovingForward && isInValidState;
    }

    private boolean isMovementAllowed() {
        Screen currentScreen = this.minecraft.screen;
        return this.minecraft.player != null && currentScreen != null && (this.isContainerScreen(currentScreen) || this.isClickGuiScreen(currentScreen));
    }

    private boolean isContainerScreen(Screen screen) {
        return screen instanceof AbstractContainerScreen;
    }

    private boolean isClickGuiScreen(Screen screen) {
        String className = screen.getClass().getSimpleName();
        return screen instanceof ClickGUI || className.contains("ClickGUI") || className.contains("ClickGui");
    }

    private float calculateForwardMovement() {
        if (this.isKeyActive(this.minecraft.options.keyUp)) {
            return 1.0F;
        } else {
            return this.isKeyActive(this.minecraft.options.keyDown) ? -1.0F : 0.0F;
        }
    }

    private float calculateStrafeMovement() {
        if (this.isKeyActive(this.minecraft.options.keyLeft)) {
            return 1.0F;
        } else {
            return this.isKeyActive(this.minecraft.options.keyRight) ? -1.0F : 0.0F;
        }
    }

    private boolean isValidTickEvent(EventRunTicks event) {
        return event.getType() == EventType.PRE && this.isMovementAllowed();
    }

    private void adjustPlayerRotation() {
        LocalPlayer player = this.minecraft.player;
        float currentPitch = player.getXRot();
        float currentYaw = player.getYRot();
        if (this.isKeyActive(265)) {
            player.setXRot(Math.max(currentPitch - 5.0F, -90.0F));
        }

        if (this.isKeyActive(264)) {
            player.setXRot(Math.min(currentPitch + 5.0F, 90.0F));
        }

        if (this.isKeyActive(263)) {
            player.setYRot(currentYaw - 5.0F);
        }

        if (this.isKeyActive(262)) {
            player.setYRot(currentYaw + 5.0F);
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        InvPacketList.clear();
        pc = null;
        c16C = null;
    }

}