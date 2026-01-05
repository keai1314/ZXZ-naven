package com.heypixel.heypixelmod.modules.impl.misc;

import com.heypixel.heypixelmod.events.api.EventTarget;
import com.heypixel.heypixelmod.events.impl.EventUpdate;
import com.heypixel.heypixelmod.modules.Category;
import com.heypixel.heypixelmod.modules.Module;
import com.heypixel.heypixelmod.modules.ModuleInfo;

@ModuleInfo(
        name = "AutoBan",
        description = "ban me :D",
        category = Category.MISC
)
public class AutoBan extends Module {
    private final float simpleSpeed = 10.0F;

    @EventTarget
    public void onUpdate(EventUpdate event) {
        double yaw = Math.toRadians(mc.player.getYRot());
        mc.player.setDeltaMovement(
                    -Math.sin(yaw) * this.simpleSpeed,
                    mc.player.getDeltaMovement().y,
                    Math.cos(yaw) * this.simpleSpeed
        );
    }
}