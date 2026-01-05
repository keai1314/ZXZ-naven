package com.heypixel.heypixelmod.ui.notification;

import com.heypixel.heypixelmod.utils.renderer.Fonts;
import com.mojang.blaze3d.vertex.PoseStack;

public class IsLandNotification extends Notification {

    private final boolean enabled;
    private final String moduleName;

    public IsLandNotification(String message, boolean enabled) {
        super(message, enabled);
        this.enabled = enabled;
        this.moduleName = message;
    }

    public IsLandNotification(NotificationLevel level, String message, long age) {
        super(level, message, age);
        this.enabled = level == NotificationLevel.SUCCESS;
        this.moduleName = message;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getModuleName() {
        return moduleName;
    }

    @Override
    public void renderShader(PoseStack stack, float x, float y) {
    }

    @Override
    public void render(PoseStack stack, float x, float y) {
    }

    @Override
    public float getWidth() {
        float textScale = 0.3F;
        float statusText = (float)Fonts.opensans.getWidth(enabled ? "Module Enable" : "Module Disable", textScale);
        float moduleText = (float)Fonts.opensans.getWidth(moduleName, textScale);
        float maxTextWidth = Math.max(statusText, moduleText);
        return 16.0F + 8.0F + maxTextWidth + 16.0F;
    }

    @Override
    public float getHeight() {
        return 36.0F;
    }
}
