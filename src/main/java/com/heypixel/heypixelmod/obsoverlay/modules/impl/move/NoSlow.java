package com.heypixel.heypixelmod.obsoverlay.modules.impl.move;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventPacket;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRunTicks;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventSlowdown;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.combat.FakeLag;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.item.BowItem;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.StreamSupport;


@ModuleInfo(
        name = "NoSlow",
        description = "Using Item No slowdown",
        category = Category.MOVEMENT
)
public class NoSlow extends Module {
    private int blinkSlowdownCounter = 0;
    private boolean blinkActive = false;
    private boolean usingActive = false;
    private final LinkedBlockingQueue<Packet<?>> movementQueue = new LinkedBlockingQueue<>();
   public FloatValue slowtick = ValueBuilder.create(this, "Slowdown Ticks")
           .setDefaultFloatValue(12.0F)
           .setMinFloatValue(0.0F)
           .setMaxFloatValue(20.0F)
           .setFloatStep(1.0F)
           .build()
           .getFloatValue();

//   public BooleanValue postBlinkDelay = ValueBuilder.create(this, "Post Blink Delay")
//           .setDefaultBooleanValue(false)
//           .build()
//           .getBooleanValue();

    private int postDelayTicks = 0;

    private int maxUseDuration = 0;
    private int useTimer = 0;

    @EventTarget
    public void onSlow(EventSlowdown eventSlowdown) {
        boolean usingNow = mc.player != null && mc.player.getUseItemRemainingTicks() > 0;
        if (usingNow && mc.player.getUseItem() != null && mc.player.getUseItem().getItem() instanceof BowItem) {
            return;
        }
        if (usingNow && !usingActive) {
            usingActive = true;
            blinkSlowdownCounter = (int) slowtick.getCurrentValue();
            blinkActive = false;
            postDelayTicks = 0;
            movementQueue.clear();

            useTimer = 0;
            if (mc.player.getUseItem() != null) {
                maxUseDuration = mc.player.getUseItem().getUseDuration();
            } else {
                maxUseDuration = 32;
            }
        }
        if (usingNow) {
            if (blinkSlowdownCounter > 0) {
                blinkSlowdownCounter--;
                return;
            } else {
                blinkActive = true;
                if (mc.options.keyUse.isDown()) {
                    mc.options.keyUse.setDown(false);
                }
                return;
            }
        } else {
                /*
                if (blinkActive) {
                    blinkActive = false;
                    releasePackets();
                }
                blinkActive = false;
                usingActive = false;
                wasSlowdownActive = false;
                pausePhase = false;
                pauseTimer = 0;
                */
            return;
        }
    }

    @EventTarget
    public void onRunTicks(EventRunTicks event) {
        if (!isEnabled() || event.getType() != EventType.PRE)
            return;

        if (usingActive) {
            useTimer++;
        }

        if (blinkActive && useTimer >= maxUseDuration) {
            if (/*postBlinkDelay.getCurrentValue() && */postDelayTicks < 1) {
                postDelayTicks++;
                return;
            }
            blinkActive = false;
            releasePackets();
            usingActive = false;
            return;
        }

        if (blinkActive) {
            while (StreamSupport.stream(mc.level.entitiesForRendering().spliterator(), false).anyMatch(entity -> entity instanceof PrimedTnt && mc.player.distanceTo(entity) < 10) && !movementQueue.isEmpty()) {
                release1Tick();
            }

            while (!movementQueue.isEmpty()) {
                long movementCount = movementQueue.stream().filter(p -> p instanceof ServerboundMovePlayerPacket).count();
                if (movementCount < 100) {
                    break;
                }
                release1Tick();
            }
        }
    }

    @EventTarget
    public void onPacket(EventPacket event) {
        if (!isEnabled()) return;

        if (event.getType() == EventType.RECEIVE) {
            if (event.getPacket() instanceof ClientboundPlayerPositionPacket) {
                releasePackets();
            }
            return;
        }

        if (event.getType() != EventType.SEND) return;
        if (!blinkActive) return;
        Packet<?> packet = event.getPacket();

        if (packet instanceof ServerboundUseItemOnPacket || packet instanceof ServerboundUseItemPacket) {
            return;
        }

        if (packet instanceof ServerboundChatPacket) {
            return;
        }

        event.setCancelled(true);
        movementQueue.offer(packet);
    }

    private void releasePackets() {
        if (mc.getConnection() == null) {
            movementQueue.clear();
            return;
        }
        try {
            while (!movementQueue.isEmpty()) {
                Packet<?> packet = movementQueue.poll();
                if (packet != null) {
                    // 检查FakeLag是否启用，如果启用则交给FakeLag处理
                    FakeLag fakeLag = (FakeLag) Naven.getInstance().getModuleManager().getModule(FakeLag.class);
                    if (fakeLag.isEnabled()) {
                        fakeLag.handlePacketFromNoSlow(packet);
                    } else {
                        mc.getConnection().getConnection().send(packet);
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void release1Tick() {
        if (mc.getConnection() == null) {
            movementQueue.clear();
            return;
        }
        try {
            while (!movementQueue.isEmpty()) {
                Packet<?> packet = movementQueue.poll();
                if (packet != null) {
                    // 检查FakeLag是否启用，如果启用则交给FakeLag处理
                    FakeLag fakeLag = (FakeLag) Naven.getInstance().getModuleManager().getModule(FakeLag.class);
                    if (fakeLag.isEnabled()) {
                        fakeLag.handlePacketFromNoSlow(packet);
                    } else {
                        mc.getConnection().getConnection().send(packet);
                    }
                }
                if (packet instanceof ServerboundMovePlayerPacket) {
                    break;
                }
            }
        } catch (Exception ignored) {
        }
    }
}
