package com.heypixel.heypixelmod.obsoverlay.managers;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMotion;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventPacket;
import com.heypixel.heypixelmod.obsoverlay.utils.NetworkUtils;
import java.util.Deque;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedDeque;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.network.protocol.game.ServerboundKeepAlivePacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import net.minecraft.network.protocol.login.ServerboundHelloPacket;
import net.minecraft.network.protocol.login.ServerboundKeyPacket;
import net.minecraft.network.protocol.status.ServerboundPingRequestPacket;
import net.minecraft.network.protocol.status.ServerboundStatusRequestPacket;

public class LagManager {
    private static final Minecraft mc = Minecraft.getInstance();
    public final Deque<LagPacket> packetQueue = new ConcurrentLinkedDeque<LagPacket>();
    private int tickDelay = 0;
    private boolean flushing = false;
    private Vec3 lastPosition = new Vec3(0.0, 0.0, 0.0);

    public boolean handlePacket(Packet<?> packet) {
        this.flushQueue();
        if (packet instanceof ServerboundKeepAlivePacket || packet instanceof ServerboundChatPacket) {
            return false;
        }
        if (this.tickDelay > 0) {
            this.packetQueue.offer(new LagPacket(packet));
            return true;
        }
        if (packet instanceof ServerboundMovePlayerPacket) {
            ServerboundMovePlayerPacket serverboundMovePlayerPacket = (ServerboundMovePlayerPacket)packet;
            this.lastPosition = new Vec3(serverboundMovePlayerPacket.getX(0.0), serverboundMovePlayerPacket.getY(0.0), serverboundMovePlayerPacket.getZ(0.0));
        }
        return false;
    }

    public void setDelay(int n) {
        this.tickDelay = n;
    }

    public Vec3 getLastPosition() {
        return this.lastPosition;
    }

    public boolean isFlushing() {
        return this.flushing;
    }

    public int getQueueSize() {
        return this.packetQueue.size();
    }

    public boolean isQueueEmpty() {
        return this.packetQueue.isEmpty();
    }

    public void reset() {
        this.packetQueue.clear();
        this.tickDelay = 0;
        this.flushing = false;
    }

    public void releaseAll() {
        while (!this.packetQueue.isEmpty()) {
            LagPacket lagPacket = this.packetQueue.poll();
            if (lagPacket == null) continue;
            if (lagPacket.packet instanceof ServerboundMovePlayerPacket) {
                ServerboundMovePlayerPacket serverboundMovePlayerPacket = (ServerboundMovePlayerPacket)lagPacket.packet;
                this.lastPosition = new Vec3(serverboundMovePlayerPacket.getX(0.0), serverboundMovePlayerPacket.getY(0.0), serverboundMovePlayerPacket.getZ(0.0));
            }
            NetworkUtils.sendPacketNoEvent(lagPacket.packet);
        }
    }

    private void flushQueue() {
        if (mc.getConnection() == null) {
            this.packetQueue.clear();
            return;
        }
        
        this.flushing = true;
        
        // 遍历所有数据包，释放延迟超过tickDelay的数据包
        Iterator<LagPacket> iterator = this.packetQueue.iterator();
        while (iterator.hasNext()) {
            LagPacket lagPacket = iterator.next();
            if (this.tickDelay <= 0 || lagPacket.delay > this.tickDelay) {
                // 释放数据包
                NetworkUtils.sendPacketNoEvent(lagPacket.packet);
                if (lagPacket.packet instanceof ServerboundMovePlayerPacket) {
                    ServerboundMovePlayerPacket serverboundMovePlayerPacket = (ServerboundMovePlayerPacket)lagPacket.packet;
                    this.lastPosition = new Vec3(serverboundMovePlayerPacket.getX(0.0), serverboundMovePlayerPacket.getY(0.0), serverboundMovePlayerPacket.getZ(0.0));
                }
                // 从队列中移除
                iterator.remove();
            } else {
                // 数据包延迟还没到，继续保留
                break;
            }
        }
        
        this.flushing = false;
    }

    private void incrementDelays() {
        this.packetQueue.forEach(lagPacket -> ++lagPacket.delay);
    }

    @EventTarget
    public void onMotion(EventMotion eventMotion) {
        if (eventMotion.getType() == EventType.POST && LagManager.mc.player != null) {
            if (LagManager.mc.player.isDeadOrDying()) {
                this.setDelay(0);
            }
            this.incrementDelays();
            this.flushQueue();
        }
    }

    @EventTarget
    public void onPacket(EventPacket eventPacket) {
        Packet<?> packet = eventPacket.getPacket();
        if (eventPacket.getType() == EventType.SEND) {
            if (packet instanceof ClientIntentionPacket || packet instanceof ServerboundHelloPacket || packet instanceof ServerboundKeyPacket || packet instanceof ServerboundStatusRequestPacket || packet instanceof ServerboundPingRequestPacket) {
                this.setDelay(0);
            } else if (this.handlePacket(packet)) {
                eventPacket.setCancelled(true);
            }
        }
    }

    public static class Vec3 {
        public final double x;
        public final double y;
        public final double z;

        public Vec3(double d, double d2, double d3) {
            this.x = d;
            this.y = d2;
            this.z = d3;
        }

        public Vec3 addVector(double d, double d2, double d3) {
            return new Vec3(this.x + d, this.y + d2, this.z + d3);
        }
    }

    public static class LagPacket {
        public final Packet<?> packet;
        public int delay;

        public LagPacket(Packet<?> packet) {
            this.packet = packet;
            this.delay = 0;
        }
    }
}