package com.heypixel.heypixelmod.utils.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.nio.ByteBuffer;

public class BlurInit2 {
    private static int blurTexture = -1;
    private static int lastWidth = -1;
    private static int lastHeight = -1;

    public static void init() {

    }

    public static void renderBlur(GuiGraphics guiGraphics, int blurStrength) {
        if (blurStrength <= 0) return;

        Minecraft mc = Minecraft.getInstance();
        int windowWidth = mc.getWindow().getWidth();
        int windowHeight = mc.getWindow().getHeight();

        if (lastWidth != windowWidth || lastHeight != windowHeight) {
            if (blurTexture != -1) {
                GL11.glDeleteTextures(blurTexture);
            }
            lastWidth = windowWidth;
            lastHeight = windowHeight;
            blurTexture = -1;
        }

        if (blurTexture == -1) {
            blurTexture = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, blurTexture);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, windowWidth, windowHeight, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);
        }

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, blurTexture);
        GL11.glCopyTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, 0, 0, windowWidth, windowHeight, 0);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, blurTexture);

        Matrix4f matrix = guiGraphics.pose().last().pose();
        BufferBuilder builder = Tesselator.getInstance().getBuilder();

        int scaledWidth = mc.getWindow().getGuiScaledWidth();
        int scaledHeight = mc.getWindow().getGuiScaledHeight();

        float offset = blurStrength * 0.002F;
        float alpha = 0.15F;

        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        for (int pass = 0; pass < 3; pass++) {
            float currentOffset = offset * (pass + 1);

            float[][] offsets = {
                {0, 0},
                {-currentOffset, -currentOffset},
                {0, -currentOffset},
                {currentOffset, -currentOffset},
                {-currentOffset, 0},
                {currentOffset, 0},
                {-currentOffset, currentOffset},
                {0, currentOffset},
                {currentOffset, currentOffset}
            };

            for (float[] off : offsets) {
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);

                builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

                float u1 = off[0];
                float v1 = off[1];
                float u2 = 1.0F + off[0];
                float v2 = 1.0F + off[1];

                builder.vertex(matrix, 0, scaledHeight, 0).uv(u1, v2).endVertex();
                builder.vertex(matrix, scaledWidth, scaledHeight, 0).uv(u2, v2).endVertex();
                builder.vertex(matrix, scaledWidth, 0, 0).uv(u2, v1).endVertex();
                builder.vertex(matrix, 0, 0, 0).uv(u1, v1).endVertex();

                Tesselator.getInstance().end();
            }

            alpha *= 0.7F;
        }

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    public static void cleanup() {
        if (blurTexture != -1) {
            GL11.glDeleteTextures(blurTexture);
            blurTexture = -1;
        }
        lastWidth = -1;
        lastHeight = -1;
    }
}