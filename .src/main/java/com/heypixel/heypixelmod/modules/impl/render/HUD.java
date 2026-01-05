package com.heypixel.heypixelmod.modules.impl.render;

import com.heypixel.heypixelmod.Naven;
import com.heypixel.heypixelmod.events.api.EventTarget;
import com.heypixel.heypixelmod.events.api.types.EventType;
import com.heypixel.heypixelmod.events.impl.EventRender2D;
import com.heypixel.heypixelmod.events.impl.EventShader;
import com.heypixel.heypixelmod.modules.Category;
import com.heypixel.heypixelmod.modules.Module;
import com.heypixel.heypixelmod.modules.ModuleInfo;
import com.heypixel.heypixelmod.ui.BetterHotBar;
import com.heypixel.heypixelmod.ui.Watermark.Watermark;
import com.heypixel.heypixelmod.ui.ArrayList.ArrayList;
import com.heypixel.heypixelmod.ui.notification.NotificationMode;
import com.heypixel.heypixelmod.values.ValueBuilder;
import com.heypixel.heypixelmod.values.Value;
import com.heypixel.heypixelmod.values.impl.BooleanValue;
import com.heypixel.heypixelmod.values.impl.FloatValue;
import com.heypixel.heypixelmod.values.impl.ModeValue;

import java.awt.Color;

@ModuleInfo(
        name = "HUD",
        description = "Displays information on your screen",
        category = Category.RENDER
)
public class HUD extends Module {
    public static final int headerColor = new Color(150, 45, 45, 255).getRGB();
    public static final int bodyColor = new Color(0, 0, 0, 120).getRGB();
    public static final int backgroundColor = new Color(0, 0, 0, 40).getRGB();
    public BooleanValue waterMark = ValueBuilder.create(this, "Water Mark").setDefaultBooleanValue(true).build().getBooleanValue();

    public ModeValue watermarkStyle = ValueBuilder.create(this, "Watermark Style")
            .setVisibility(this.waterMark::getCurrentValue)
            .setDefaultModeIndex(0)
            .setModes("Rainbow", "Classic", "Capsule", "exhibition", "skeet", "Alpha")
            .build()
            .getModeValue();

    public FloatValue watermarkSize = ValueBuilder.create(this, "Watermark Size")
            .setVisibility(this.waterMark::getCurrentValue)
            .setDefaultFloatValue(0.4F)
            .setFloatStep(0.01F)
            .setMinFloatValue(0.1F)
            .setMaxFloatValue(1.0F)
            .build()
            .getFloatValue();
    public FloatValue watermarkCornerRadius = ValueBuilder.create(this, "Watermark Corner Radius")
            .setVisibility(this.waterMark::getCurrentValue)
            .setDefaultFloatValue(5.0F)
            .setFloatStep(0.5F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(20.0F)
            .build()
            .getFloatValue();
    public FloatValue watermarkVPadding = ValueBuilder.create(this, "Watermark V-Padding")
            .setVisibility(this.waterMark::getCurrentValue)
            .setDefaultFloatValue(4.0F)
            .setFloatStep(0.5F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(10.0F)
            .build()
            .getFloatValue();
    public BooleanValue renderBlackBackground = ValueBuilder.create(this, "RenderBlackBackGround")
            .setVisibility(() -> this.waterMark.getCurrentValue() && this.watermarkStyle.isCurrentMode("Capsule"))
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();
    public BooleanValue blackFont = ValueBuilder.create(this, "BlackFont")
            .setVisibility(() -> this.waterMark.getCurrentValue() && this.watermarkStyle.isCurrentMode("Capsule"))
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();
    public BooleanValue moduleToggleSound = ValueBuilder.create(this, "Module Toggle Sound").setDefaultBooleanValue(true).build().getBooleanValue();
    public BooleanValue notification = ValueBuilder.create(this, "Notification").setDefaultBooleanValue(true).build().getBooleanValue();
    public ModeValue notificationStyle = ValueBuilder.create(this, "Notification Style")
            .setVisibility(this.notification::getCurrentValue)
            .setModes("SouthSide", "Naven", "Capsule", "isLand")
            .setDefaultModeIndex(0)
            .setOnUpdate(this::onNotificationStyleChange)
            .build()
            .getModeValue();
    public BooleanValue arrayList = ValueBuilder.create(this, "Array List").setDefaultBooleanValue(true).build().getBooleanValue();
    public ModeValue arrayListMode = ValueBuilder.create(this, "ArrayList Mode")
            .setVisibility(this.arrayList::getCurrentValue)
            .setDefaultModeIndex(0)
            .setModes("Normal", "Exhibition", "Capsule")
            .build()
            .getModeValue();

    public BooleanValue arrayListCapsule = ValueBuilder.create(this, "ArrayList Capsule")
            .setVisibility(this.arrayList::getCurrentValue)
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();
    public BooleanValue prettyModuleName = ValueBuilder.create(this, "Pretty Module Name")
            .setOnUpdate(value -> Module.update = true)
            .setVisibility(this.arrayList::getCurrentValue)
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();
    public BooleanValue hideRenderModules = ValueBuilder.create(this, "Hide Render Modules")
            .setOnUpdate(value -> Module.update = true)
            .setVisibility(this.arrayList::getCurrentValue)
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();
    public BooleanValue rainbow = ValueBuilder.create(this, "Rainbow")
            .setDefaultBooleanValue(true)
            .setVisibility(this.arrayList::getCurrentValue)
            .build()
            .getBooleanValue();
    public FloatValue rainbowSpeed = ValueBuilder.create(this, "Rainbow Speed")
            .setVisibility(() -> this.arrayList.getCurrentValue() && this.rainbow.getCurrentValue())
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(20.0F)
            .setDefaultFloatValue(10.0F)
            .setFloatStep(0.1F)
            .build()
            .getFloatValue();
    public FloatValue rainbowOffset = ValueBuilder.create(this, "Rainbow Offset")
            .setVisibility(() -> this.arrayList.getCurrentValue() && this.rainbow.getCurrentValue())
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(20.0F)
            .setDefaultFloatValue(10.0F)
            .setFloatStep(0.1F)
            .build()
            .getFloatValue();
    public FloatValue colorR = ValueBuilder.create(this, "R-Color")
            .setVisibility(() -> this.arrayList.getCurrentValue() && !this.rainbow.getCurrentValue())
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(255.0F)
            .setDefaultFloatValue(255.0F)
            .setFloatStep(1.0F)
            .build()
            .getFloatValue();
    public FloatValue colorG = ValueBuilder.create(this, "G-Color")
            .setVisibility(() -> this.arrayList.getCurrentValue() && !this.rainbow.getCurrentValue())
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(255.0F)
            .setDefaultFloatValue(255.0F)
            .setFloatStep(1.0F)
            .build()
            .getFloatValue();
    public FloatValue colorB = ValueBuilder.create(this, "B-Color")
            .setVisibility(() -> this.arrayList.getCurrentValue() && !this.rainbow.getCurrentValue())
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(255.0F)
            .setDefaultFloatValue(255.0F)
            .setFloatStep(1.0F)
            .build()
            .getFloatValue();
    public ModeValue arrayListDirection = ValueBuilder.create(this, "ArrayList Direction")
            .setVisibility(this.arrayList::getCurrentValue)
            .setDefaultModeIndex(0)
            .setModes("Right", "Left")
            .build()
            .getModeValue();
    public FloatValue arrayListSize = ValueBuilder.create(this, "ArrayList Size")
            .setVisibility(this.arrayList::getCurrentValue)
            .setDefaultFloatValue(0.4F)
            .setFloatStep(0.01F)
            .setMinFloatValue(0.1F)
            .setMaxFloatValue(1.0F)
            .build()
            .getFloatValue();
    public FloatValue arrayListSpacing = ValueBuilder.create(this, "ArrayList Spacing")
            .setVisibility(this.arrayList::getCurrentValue)
            .setDefaultFloatValue(2.0F)
            .setFloatStep(0.5F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(10.0F)
            .build()
            .getFloatValue();

    public BooleanValue hotbarRenderBackground = ValueBuilder.create(this, "Hotbar-RenderBackGround")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();

    public FloatValue hotbarAnimationSpeed = ValueBuilder.create(this, "Hotbar-AnimationsSpeed")
            .setDefaultFloatValue(20.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(40.0F)
            .build()
            .getFloatValue();

    @EventTarget
    public void notification(EventRender2D e) {
        if (this.notification.getCurrentValue()) {
            Naven.getInstance().getNotificationManager().onRender(e);
        }
    }

    private void onNotificationStyleChange(Value value) {
        updateNotificationMode();
    }

    private void updateNotificationMode() {
        String currentMode = notificationStyle.getCurrentMode();
        switch (currentMode) {
            case "Naven":
                NotificationMode.setCurrentMode(
                        NotificationMode.NAVEN);
                break;
            case "Capsule":
                NotificationMode.setCurrentMode(
                        NotificationMode.CAPSULE);
                break;
            case "isLand":
                NotificationMode.setCurrentMode(
                        NotificationMode.ISLAND);
                break;
            case "SouthSide":
            default:
                NotificationMode.setCurrentMode(
                        NotificationMode.SOUTHSIDE);
                break;
        }
    }

    @EventTarget
    public void onShader(EventShader e) {

        if (this.notification.getCurrentValue()) {
            if (e.getType() == EventType.SHADOW) {
                Naven.getInstance().getNotificationManager().onRenderShadow(e);
            } else if (e.getType() == EventType.BLUR) {
                Naven.getInstance().getNotificationManager().onRenderBlur(e);
            }
        }

        if (this.waterMark.getCurrentValue()) {
            Watermark.onShader(e, this.watermarkStyle.getCurrentMode(), this.watermarkCornerRadius.getCurrentValue(), this.watermarkSize.getCurrentValue(), this.watermarkVPadding.getCurrentValue(), this.renderBlackBackground.getCurrentValue(), this.blackFont.getCurrentValue());
        }

        if (this.arrayList.getCurrentValue() && e.getType() == EventType.BLUR) {
            ArrayList.onShader(e);
        }

        BetterHotBar.onShader(e, this.hotbarRenderBackground.getCurrentValue());
    }

    @EventTarget
    public void onRender(EventRender2D e) {
        if (this.waterMark.getCurrentValue()) {
            Watermark.onRender(e, this.watermarkSize.getCurrentValue(), this.watermarkStyle.getCurrentMode(), this.rainbow.getCurrentValue(), this.rainbowSpeed.getCurrentValue(), this.rainbowOffset.getCurrentValue(), this.watermarkCornerRadius.getCurrentValue(), this.watermarkVPadding.getCurrentValue(), this.renderBlackBackground.getCurrentValue(), this.blackFont.getCurrentValue());
        }

        if (this.arrayList.getCurrentValue()) {
            ArrayList.Mode mode = ArrayList.Mode.Normal;
            if (this.arrayListMode.isCurrentMode("Exhibition")) mode = ArrayList.Mode.Exhibition;
            else if (this.arrayListMode.isCurrentMode("Capsule")) mode = ArrayList.Mode.Capsule;
            ArrayList.onRender(e, mode, this.arrayListCapsule.getCurrentValue(), this.prettyModuleName.getCurrentValue(), this.hideRenderModules.getCurrentValue(), this.rainbow.getCurrentValue(), this.rainbowSpeed.getCurrentValue(), this.rainbowOffset.getCurrentValue(), this.arrayListDirection.getCurrentMode(), this.arrayListSize.getCurrentValue(), this.arrayListSpacing.getCurrentValue(), this.colorR.getCurrentValue(), this.colorG.getCurrentValue(), this.colorB.getCurrentValue());
        }

        if (BetterHotBar.shouldRenderCustomHotbar()) {
            BetterHotBar.renderCustomHotbar(e.getGuiGraphics(), 0.0F, this.hotbarAnimationSpeed.getCurrentValue(), this.hotbarRenderBackground.getCurrentValue());
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
        updateNotificationMode();
    }
}