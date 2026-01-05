package com.heypixel.heypixelmod.modules.impl.misc;

import com.heypixel.heypixelmod.modules.Category;
import com.heypixel.heypixelmod.modules.Module;
import com.heypixel.heypixelmod.modules.ModuleInfo;
import com.mojang.blaze3d.systems.RenderSystem;

@ModuleInfo(
        name = "ResetOpenGL",
        category = Category.MISC,
        description = "Resets OpenGL state when enabled"
)
public class ResetOpenGL extends Module {
    @Override
    public void onEnable() {
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        this.toggle();
    }
}