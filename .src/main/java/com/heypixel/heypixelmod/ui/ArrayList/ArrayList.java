package com.heypixel.heypixelmod.ui.ArrayList;

import com.heypixel.heypixelmod.Naven;
import com.heypixel.heypixelmod.events.impl.EventRender2D;
import com.heypixel.heypixelmod.events.impl.EventShader;
import com.heypixel.heypixelmod.modules.Category;
import com.heypixel.heypixelmod.modules.Module;
import com.heypixel.heypixelmod.modules.ModuleManager;
import com.heypixel.heypixelmod.ui.HUDEditor;
import com.heypixel.heypixelmod.utils.RenderUtils;
import com.heypixel.heypixelmod.utils.SmoothAnimationTimer;
import com.heypixel.heypixelmod.utils.renderer.Fonts;
import com.heypixel.heypixelmod.utils.renderer.text.CustomTextRenderer;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import org.joml.Vector4f;

import java.awt.Color;
import java.util.List;

public class ArrayList {

    public enum Mode {
        Normal,
        Exhibition,
        Capsule
    }

    private static List<Module> renderModules;
    private static final java.util.ArrayList<Vector4f> blurMatrices = new java.util.ArrayList<>();
    public static final int backgroundColor = new Color(0, 0, 0, 40).getRGB();

    private static final java.util.Map<Module, Float> widthCache = new java.util.HashMap<>();
    private static final java.util.Map<Module, String> nameCache = new java.util.HashMap<>();
    private static float cachedArrayListSize = -1.0F;
    private static boolean cachedPrettyName = false;
    private static boolean cachedHideRender = false;
    private static float cachedMaxWidth = 0.0F;

    private static String getModuleDisplayName(Module module, boolean pretty) {
        String name = pretty ? module.getPrettyName() : module.getName();
        return name + (module.getSuffix() == null ? "" : " §7" + module.getSuffix());
    }

    public static void onShader(EventShader e) {

        for (Vector4f blurMatrix : blurMatrices) {
            RenderUtils.drawRoundedRect(e.getStack(), blurMatrix.x(), blurMatrix.y(), blurMatrix.z(), blurMatrix.w(), 3.0F, Integer.MIN_VALUE);
        }
    }

    private static void drawVerticalAnimatedRainbowBar(com.mojang.blaze3d.vertex.PoseStack stack, float x, float y, float width, float height, float rainbowSpeed, float rainbowOffset) {

        int segments = Math.max(4, Math.min(12, (int)(height / 2.0F)));
        float segmentHeight = height / (float)segments;
        for (int s = 0; s < segments; s++) {
            float segY0 = y + s * segmentHeight;
            float segY1 = (s == segments - 1) ? (y + height) : (segY0 + segmentHeight);
            float sampleY = (segY0 + segY1) * 0.5F;
            int color = RenderUtils.getRainbowOpaque(
                    (int)(-sampleY * rainbowOffset),
                    1.0F, 1.0F, (21.0F - rainbowSpeed) * 1000.0F
            );
            RenderUtils.fill(stack, x, segY0, x + width, segY1, color);
        }
    }

    public static void onRender(EventRender2D e, Mode mode, boolean capsule, boolean prettyModuleName, boolean hideRenderModules, boolean rainbow, float rainbowSpeed, float rainbowOffset, String arrayListDirection, float arrayListSize, float arrayListSpacing, float colorR, float colorG, float colorB) {
        blurMatrices.clear();
        CustomTextRenderer font = Fonts.opensans;
        e.getStack().pushPose();
        Minecraft mc = Minecraft.getInstance();
        ModuleManager moduleManager = Naven.getInstance().getModuleManager();

        boolean needRebuild = Module.update
                || renderModules == null
                || cachedArrayListSize != arrayListSize
                || cachedPrettyName != prettyModuleName
                || cachedHideRender != hideRenderModules;

        if (needRebuild) {
            renderModules = new java.util.ArrayList<>(moduleManager.getModules());
            if (hideRenderModules) {
                renderModules.removeIf(modulex -> modulex.getCategory() == Category.RENDER);
            }

            widthCache.clear();
            nameCache.clear();
            cachedMaxWidth = 0.0F;
            for (Module m : renderModules) {
                String display = getModuleDisplayName(m, prettyModuleName);
                nameCache.put(m, display);
                float w = font.getWidth(display, (double)arrayListSize);
                widthCache.put(m, w);
                if (w > cachedMaxWidth) cachedMaxWidth = w;
            }

            renderModules.sort((o1, o2) -> Float.compare(widthCache.getOrDefault(o2, 0.0F), widthCache.getOrDefault(o1, 0.0F)));

            cachedArrayListSize = arrayListSize;
            cachedPrettyName = prettyModuleName;
            cachedHideRender = hideRenderModules;
            Module.update = false;
        }

        float maxWidth = cachedMaxWidth;
        if (maxWidth < 50.0F) maxWidth = 100.0F;

        HUDEditor.HUDElement arrayListElement =
                HUDEditor.getInstance().getHUDElement("arraylist");

        float arrayListX, arrayListY;
        if (arrayListElement != null) {
            if ("Right".equals(arrayListDirection)) {
                arrayListX = (float)arrayListElement.x;
            } else {
                arrayListX = (float)arrayListElement.x;
            }
            arrayListY = (float)arrayListElement.y;
        } else {
            arrayListX = "Right".equals(arrayListDirection)
                    ? (float)mc.getWindow().getGuiScaledWidth() - maxWidth - 6.0F
                    : 3.0F;
            arrayListY = 3.0F;
        }
        float height = 0.0F;
        double fontHeight = font.getHeight(true, (double)arrayListSize);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();

        final int rainbowPeriodMs = (int)((21.0F - rainbowSpeed) * 1000.0F);
        final float rainbowOffsetMul = rainbowOffset;

        for (Module module : renderModules) {
            SmoothAnimationTimer animation = module.getAnimation();
            if (module.isEnabled()) {
                animation.target = 100.0F;
            } else {
                animation.target = 0.0F;
            }

            animation.update(true);
            if (animation.value > 0.0F) {

                String displayName = nameCache.get(module);
                if (displayName == null) {
                    displayName = getModuleDisplayName(module, prettyModuleName);
                    nameCache.put(module, displayName);
                }
                Float cachedWidth = widthCache.get(module);
                float stringWidth = cachedWidth != null ? cachedWidth : font.getWidth(displayName, (double)arrayListSize);
                if (cachedWidth == null) widthCache.put(module, stringWidth);
                float left = -stringWidth * (1.0F - animation.value / 100.0F);
                float right = maxWidth - stringWidth * (animation.value / 100.0F);
                float innerX = "Left".equals(arrayListDirection) ? left : right;
                float moduleHeight = (float)((double)(animation.value / 100.0F) * fontHeight);
                float moduleX = arrayListX + innerX;
                float moduleY = arrayListY + height + 2.0F;
                float moduleWidth = stringWidth + 3.0F;

                if (mode == Mode.Normal) {

                    RenderUtils.drawRoundedRect(
                            e.getStack(),
                            moduleX,
                            moduleY,
                            moduleWidth,
                            moduleHeight,
                            3.0F,
                            backgroundColor
                    );
                    blurMatrices.add(new Vector4f(moduleX, moduleY, moduleWidth, moduleHeight));
                } else if (mode == Mode.Capsule) {
                    float hPadding = 4.0F;
                    float capsuleX = moduleX + 1.5F - hPadding;
                    float capsuleW = stringWidth + hPadding * 2.0F;
                    float radius = 5.0F;
                    RenderUtils.drawRoundedRect(
                            e.getStack(),
                            capsuleX,
                            moduleY,
                            capsuleW,
                            moduleHeight,
                            radius,
                            backgroundColor
                    );
                    blurMatrices.add(new Vector4f(capsuleX, moduleY, capsuleW, moduleHeight));
                }

                int color = -1;
                int rainbowColor;
                if (rainbow) {
                    float moduleYBase = arrayListY + height + 1.0F;
                    rainbowColor = RenderUtils.getRainbowOpaque((int)(-moduleYBase * rainbowOffsetMul), 1.0F, 1.0F, rainbowPeriodMs);
                    color = rainbowColor;
                } else {
                    int r = (int) colorR;
                    int g = (int) colorG;
                    int b = (int) colorB;
                    color = (255 << 24) | (r << 16) | (g << 8) | b;
                    rainbowColor = color;
                }

                float alpha = animation.value / 100.0F;
                font.setAlpha(alpha);
                font.drawString(
                        e.getStack(),
                        displayName,
                        (double)(moduleX + 1.5F),
                        (double)(arrayListY + height + 1.0F),
                        new Color(color),
                        true,
                        (double)arrayListSize
                );

                if (rainbow && capsule && mode == Mode.Normal) {
                    float capsuleWidth = 2.0f;
                    float capsulePadding = 1.5f;
                    float capsuleX = "Left".equals(arrayListDirection)
                            ? (moduleX - capsuleWidth - capsulePadding)
                            : (moduleX + moduleWidth + capsulePadding);

                    int barColor = rainbowColor;
                    RenderUtils.fill(e.getStack(), capsuleX, moduleY, capsuleX + capsuleWidth, moduleY + moduleHeight, barColor);
                } else if (rainbow && mode == Mode.Capsule) {
                    float barWidth = 2.0f;
                    float hPadding = 4.0f;
                    float capsuleX = moduleX + 1.5F - hPadding;
                    int barColor = rainbowColor;
                    RenderUtils.fill(e.getStack(), capsuleX, moduleY, capsuleX + barWidth, moduleY + moduleHeight, barColor);
                }

                height += (float)((double)(animation.value / 100.0F) * (fontHeight + arrayListSpacing));
            }
        }

        if (arrayListElement != null) {
            arrayListElement.width = Math.max(maxWidth, 100.0F);
            arrayListElement.height = Math.max(height, 50.0F);
        }

        font.setAlpha(1.0F);
        e.getStack().popPose();
    }
}