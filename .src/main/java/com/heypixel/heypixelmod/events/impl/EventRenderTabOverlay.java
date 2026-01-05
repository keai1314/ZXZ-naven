package com.heypixel.heypixelmod.events.impl;

import com.heypixel.heypixelmod.events.api.events.Event;
import com.heypixel.heypixelmod.events.api.types.EventType;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;

public class EventRenderTabOverlay implements Event {
    private EventType type;
    private Component component;
    private PlayerInfo playerInfo;

    public void setType(EventType type) {
        this.type = type;
    }

    public void setComponent(Component component) {
        this.component = component;
    }

    public EventType getType() {
        return this.type;
    }

    public Component getComponent() {
        return this.component;
    }

    public PlayerInfo getPlayerInfo() {
        return playerInfo;
    }

    public EventRenderTabOverlay(EventType type, Component component) {
        this(type, component, null);
    }

    public EventRenderTabOverlay(EventType type, Component component, PlayerInfo playerInfo) {
        this.type = type;
        this.component = component;
        this.playerInfo = playerInfo;
    }
}
