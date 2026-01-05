package com.heypixel.heypixelmod.events.api.events;

import com.heypixel.heypixelmod.Naven;
import com.heypixel.heypixelmod.events.api.EventTarget;
import com.heypixel.heypixelmod.events.api.types.EventType;
import com.heypixel.heypixelmod.events.impl.EventPacket;
import com.heypixel.heypixelmod.events.impl.EventRunTicks;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundDisconnectPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.sounds.SoundEvents;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class BackTrackPacketEventManager {

    private static final BackTrackPacketEventManager INSTANCE = new BackTrackPacketEventManager();

    private final Queue<Packet<?>> delayedPacketQueue = new ConcurrentLinkedQueue<>();
    private final Queue<Packet<?>> packetProcessQueue = new ConcurrentLinkedQueue<>();

    private boolean shouldCancelPackets = false;
    private int currentDelay = 100;
    private long lastProcessTime = 0;

    private final Minecraft mc = Minecraft.getInstance();

    private BackTrackPacketEventManager() {

    }

    public void register() {
        Naven.getInstance().getEventManager().register(this);
    }

    public void unregister() {
        Naven.getInstance().getEventManager().unregister(this);
    }

    public static BackTrackPacketEventManager getInstance() {
        return INSTANCE;
    }

    @EventTarget
    public void onPacket(EventPacket event) {
        if (event.getType() != EventType.RECEIVE || event.isCancelled()) {
            return;
        }

        if (!shouldCancelPackets && delayedPacketQueue.isEmpty()) {
            return;
        }

        Packet<?> packet = event.getPacket();

        if (packet instanceof ClientboundPlayerPositionPacket ||
            packet instanceof ClientboundDisconnectPacket) {
            clear(true);
            return;
        }

        if (packet instanceof ClientboundSoundPacket soundPacket) {
            if (soundPacket.getSound().value() == SoundEvents.PLAYER_HURT) {
                return;
            }
        }

        event.setCancelled(true);
        delayedPacketQueue.add(packet);
    }

    @EventTarget
    public void onTick(EventRunTicks event) {
        if (mc.player == null || mc.level == null) {
            clear(true);
            return;
        }

        if (shouldCancelPackets) {
            processPackets();
        } else {
            clear();
        }

        processPacketQueue();

        lastProcessTime = System.currentTimeMillis();
    }

    private void processPackets() {
        delayedPacketQueue.removeIf(packet -> {
            if (System.currentTimeMillis() - lastProcessTime >= currentDelay) {
                packetProcessQueue.add(packet);
                return true;
            }
            return false;
        });
    }

    private void processPacketQueue() {
        packetProcessQueue.removeIf(packet -> {
            if (mc.getConnection() != null) {
                try {

                    ((Packet) packet).handle(mc.getConnection());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return true;
        });
    }

    public void clear(boolean handlePackets) {
        if (handlePackets) {
            delayedPacketQueue.forEach(packet -> {
                if (mc.getConnection() != null) {
                    try {
                        ((Packet) packet).handle(mc.getConnection());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        delayedPacketQueue.clear();
        packetProcessQueue.clear();
        shouldCancelPackets = false;
        lastProcessTime = System.currentTimeMillis();
    }

    public void clear() {
        clear(true);
    }

    public void setShouldCancelPackets(boolean shouldCancelPackets) {
        this.shouldCancelPackets = shouldCancelPackets;
        if (!shouldCancelPackets) {
            clear();
        }
    }

    public boolean isShouldCancelPackets() {
        return shouldCancelPackets;
    }

    public void setCurrentDelay(int currentDelay) {
        this.currentDelay = currentDelay;
    }

    public int getCurrentDelay() {
        return currentDelay;
    }

    public boolean isProcessingPackets() {
        return !delayedPacketQueue.isEmpty() || !packetProcessQueue.isEmpty();
    }

    public int getQueuedPacketCount() {
        return delayedPacketQueue.size() + packetProcessQueue.size();
    }
}