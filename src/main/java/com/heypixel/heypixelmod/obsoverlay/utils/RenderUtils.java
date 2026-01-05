package com.heypixel.heypixelmod.obsoverlay.utils;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.BufferBuilder.RenderedBuffer;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import java.awt.Color;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.FastColor.ARGB32;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import com.mojang.blaze3d.vertex.VertexFormat;

public class RenderUtils {
   private static final Minecraft mc = Minecraft.getInstance();
   private static final AABB DEFAULT_BOX = new AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);

   public static void drawTracer(PoseStack poseStack, float x, float y, float size, float widthDiv, float heightDiv, int color) {
      GL11.glEnable(GL11.GL_BLEND);
      GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
      GL11.glDisable(GL11.GL_DEPTH_TEST);
      GL11.glDepthMask(false);
      GL11.glEnable(GL11.GL_LINE_SMOOTH);
      RenderSystem.setShader(GameRenderer::getPositionColorShader);
      Matrix4f matrix = poseStack.last().pose();
      float a = (float)(color >> 24 & 0xFF) / 255.0F;
      float r = (float)(color >> 16 & 0xFF) / 255.0F;
      float g = (float)(color >> 8 & 0xFF) / 255.0F;
      float b = (float)(color & 0xFF) / 255.0F;
      BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
      bufferBuilder.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
      bufferBuilder.vertex(matrix, x, y, 0.0F).color(r, g, b, a).endVertex();
      bufferBuilder.vertex(matrix, x - size / widthDiv, y + size, 0.0F).color(r, g, b, a).endVertex();
      bufferBuilder.vertex(matrix, x, y + size / heightDiv, 0.0F).color(r, g, b, a).endVertex();
      bufferBuilder.vertex(matrix, x + size / widthDiv, y + size, 0.0F).color(r, g, b, a).endVertex();
      bufferBuilder.vertex(matrix, x, y, 0.0F).color(r, g, b, a).endVertex();
      BufferUploader.drawWithShader(bufferBuilder.end());
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      GL11.glDisable(GL11.GL_BLEND);
      GL11.glEnable(GL11.GL_DEPTH_TEST);
      GL11.glDepthMask(true);
      GL11.glDisable(GL11.GL_LINE_SMOOTH);
   }

   public static int reAlpha(int color, float alpha) {
      int col = MathUtils.clamp((int)(alpha * 255.0F), 0, 255) << 24;
      col |= MathUtils.clamp(color >> 16 & 0xFF, 0, 255) << 16;
      col |= MathUtils.clamp(color >> 8 & 0xFF, 0, 255) << 8;
      return col | MathUtils.clamp(color & 0xFF, 0, 255);
   }

   public static int getRainbowOpaque(int index, float saturation, float brightness, float speed) {
      float hue = (float)((System.currentTimeMillis() + (long)index) % (long)((int)speed)) / speed;
      return Color.HSBtoRGB(hue, saturation, brightness);
   }

   public static BlockPos getCameraBlockPos() {
      Camera camera = mc.getBlockEntityRenderDispatcher().camera;
      return camera.getBlockPosition();
   }

   public static Vec3 getCameraPos() {
      Camera camera = mc.getBlockEntityRenderDispatcher().camera;
      return camera.getPosition();
   }

   public static void drawCircle(PoseStack stack, Vec3 center, float radius, Color color) {
      drawCircle(stack, center, radius, color, 3.0F);
   }
   
   public static void drawCircle(PoseStack stack, Vec3 center, float radius, Color color, float thickness) {
      BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
      RenderSystem.setShader(GameRenderer::getPositionColorShader);
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableCull();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

      stack.pushPose();
      // 先注释掉区域渲染偏移
      // applyRegionalRenderOffset(stack);
      
      // 设置线条粗细
      GL11.glEnable(GL11.GL_LINE_SMOOTH);
      GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
      RenderSystem.lineWidth(thickness);

      bufferBuilder.begin(VertexFormat.Mode.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);

      // 使用世界坐标直接绘制
      Vec3 cameraPos = getCameraPos();
      for (int i = 0; i <= 360; i++) {
         double radians = Math.toRadians(i);
         float x = (float) (center.x + Math.cos(radians) * radius - cameraPos.x);
         float y = (float) (center.y - cameraPos.y);
         float z = (float) (center.z + Math.sin(radians) * radius - cameraPos.z);
         bufferBuilder.vertex(stack.last().pose(), x, y, z)
                 .color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha())
                 .endVertex();
      }

      BufferUploader.drawWithShader(bufferBuilder.end());
      stack.popPose();

      // 恢复默认线条粗细
      GL11.glDisable(GL11.GL_LINE_SMOOTH);
      RenderSystem.lineWidth(1.0F);
      RenderSystem.enableCull();
      RenderSystem.disableBlend();
   }
   
   // 添加绘制两点之间直线的方法
   public static void drawLine(PoseStack stack, Vec3 start, Vec3 end, Color color, float thickness) {
      BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
      RenderSystem.setShader(GameRenderer::getPositionColorShader);
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableCull();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

      stack.pushPose();
      
      // 设置线条粗细
      GL11.glEnable(GL11.GL_LINE_SMOOTH);
      GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
      RenderSystem.lineWidth(thickness);

      bufferBuilder.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);

      // 使用世界坐标直接绘制
      Vec3 cameraPos = getCameraPos();
      float startX = (float) (start.x - cameraPos.x);
      float startY = (float) (start.y - cameraPos.y);
      float startZ = (float) (start.z - cameraPos.z);
      
      float endX = (float) (end.x - cameraPos.x);
      float endY = (float) (end.y - cameraPos.y);
      float endZ = (float) (end.z - cameraPos.z);
      
      bufferBuilder.vertex(stack.last().pose(), startX, startY, startZ)
              .color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha())
              .endVertex();
      bufferBuilder.vertex(stack.last().pose(), endX, endY, endZ)
              .color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha())
              .endVertex();

      BufferUploader.drawWithShader(bufferBuilder.end());
      stack.popPose();

      // 恢复默认线条粗细
      GL11.glDisable(GL11.GL_LINE_SMOOTH);
      RenderSystem.lineWidth(1.0F);
      RenderSystem.enableCull();
      RenderSystem.disableBlend();
   }

   // 添加一个新的2D圆圈绘制方法
   public static void drawCircle2D(PoseStack stack, float x, float y, float radius, Color color) {
      BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
      RenderSystem.setShader(GameRenderer::getPositionColorShader);
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableCull();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

      bufferBuilder.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);

      // 绘制中心点
      bufferBuilder.vertex(stack.last().pose(), x, y, 0)
              .color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha())
              .endVertex();

      // 绘制圆形
      for (int i = 0; i <= 360; i++) {
         double radians = Math.toRadians(i);
         float px = (float) (x + Math.cos(radians) * radius);
         float py = (float) (y + Math.sin(radians) * radius);
         bufferBuilder.vertex(stack.last().pose(), px, py, 0)
                 .color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha())
                 .endVertex();
      }

      BufferUploader.drawWithShader(bufferBuilder.end());

      RenderSystem.enableCull();
      RenderSystem.disableBlend();
   }

   public static RegionPos getCameraRegion() {
      return RegionPos.of(getCameraBlockPos());
   }

   public static void applyRegionalRenderOffset(PoseStack matrixStack) {
      applyRegionalRenderOffset(matrixStack, getCameraRegion());
   }

   public static void applyRegionalRenderOffset(PoseStack matrixStack, RegionPos region) {
      Vec3 offset = region.toVec3().subtract(getCameraPos());
      matrixStack.translate(offset.x, offset.y, offset.z);
   }

   public static void fill(PoseStack pPoseStack, float pMinX, float pMinY, float pMaxX, float pMaxY, int pColor) {
      innerFill(pPoseStack.last().pose(), pMinX, pMinY, pMaxX, pMaxY, pColor);
   }

   private static void innerFill(Matrix4f pMatrix, float pMinX, float pMinY, float pMaxX, float pMaxY, int pColor) {
      if (pMinX < pMaxX) {
         float i = pMinX;
         pMinX = pMaxX;
         pMaxX = i;
      }

      if (pMinY < pMaxY) {
         float j = pMinY;
         pMinY = pMaxY;
         pMaxY = j;
      }

      float f3 = (float)(pColor >> 24 & 0xFF) / 255.0F;
      float f = (float)(pColor >> 16 & 0xFF) / 255.0F;
      float f1 = (float)(pColor >> 8 & 0xFF) / 255.0F;
      float f2 = (float)(pColor & 0xFF) / 255.0F;
      BufferBuilder bufferbuilder = Tesselator.getInstance().getBuilder();
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.setShader(GameRenderer::getPositionColorShader);
      bufferbuilder.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
      bufferbuilder.vertex(pMatrix, pMinX, pMaxY, 0.0F).color(f, f1, f2, f3).endVertex();
      bufferbuilder.vertex(pMatrix, pMaxX, pMaxY, 0.0F).color(f, f1, f2, f3).endVertex();
      bufferbuilder.vertex(pMatrix, pMaxX, pMinY, 0.0F).color(f, f1, f2, f3).endVertex();
      bufferbuilder.vertex(pMatrix, pMinX, pMinY, 0.0F).color(f, f1, f2, f3).endVertex();
      Tesselator.getInstance().end();
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableBlend();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
   }

   public static void drawRectBound(PoseStack poseStack, float x, float y, float width, float height, int color) {
      Tesselator tesselator = Tesselator.getInstance();
      BufferBuilder buffer = tesselator.getBuilder();
      Matrix4f matrix = poseStack.last().pose();
      float alpha = (float)(color >> 24 & 0xFF) / 255.0F;
      float red = (float)(color >> 16 & 0xFF) / 255.0F;
      float green = (float)(color >> 8 & 0xFF) / 255.0F;
      float blue = (float)(color & 0xFF) / 255.0F;
      buffer.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
      buffer.vertex(matrix, x, y + height, 0.0F).color(red, green, blue, alpha).endVertex();
      buffer.vertex(matrix, x + width, y + height, 0.0F).color(red, green, blue, alpha).endVertex();
      buffer.vertex(matrix, x + width, y, 0.0F).color(red, green, blue, alpha).endVertex();
      buffer.vertex(matrix, x, y, 0.0F).color(red, green, blue, alpha).endVertex();
      tesselator.end();
   }

   private static void color(BufferBuilder buffer, Matrix4f matrix, float x, float y, int color) {
      float alpha = (float)(color >> 24 & 0xFF) / 255.0F;
      float red = (float)(color >> 16 & 0xFF) / 255.0F;
      float green = (float)(color >> 8 & 0xFF) / 255.0F;
      float blue = (float)(color & 0xFF) / 255.0F;
      buffer.vertex(matrix, x, y, 0.0F).color(red, green, blue, alpha).endVertex();
   }

   public static void drawRoundedRect(PoseStack poseStack, float x, float y, float width, float height, float edgeRadius, int color) {
      if (color == 16777215) {
         color = ARGB32.color(255, 255, 255, 255);
      }

      if (edgeRadius < 0.0F) {
         edgeRadius = 0.0F;
      }

      if (edgeRadius > width / 2.0F) {
         edgeRadius = width / 2.0F;
      }

      if (edgeRadius > height / 2.0F) {
         edgeRadius = height / 2.0F;
      }

      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.lineWidth(1.0F);
      
      // 修复圆角矩形绘制，确保四个角正确绘制
      // 中心矩形
      drawRectBound(poseStack, x + edgeRadius, y + edgeRadius, width - edgeRadius * 2.0F, height - edgeRadius * 2.0F, color);
      
      // 上下边矩形
      drawRectBound(poseStack, x + edgeRadius, y, width - edgeRadius * 2.0F, edgeRadius, color);
      drawRectBound(poseStack, x + edgeRadius, y + height - edgeRadius, width - edgeRadius * 2.0F, edgeRadius, color);
      
      // 左右边矩形
      drawRectBound(poseStack, x, y + edgeRadius, edgeRadius, height - edgeRadius * 2.0F, color);
      drawRectBound(poseStack, x + width - edgeRadius, y + edgeRadius, edgeRadius, height - edgeRadius * 2.0F, color);
      
      Tesselator tesselator = Tesselator.getInstance();
      BufferBuilder buffer = tesselator.getBuilder();
      Matrix4f matrix = poseStack.last().pose();
      
      // 左上角扇形
      buffer.begin(Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
      float centerX = x + edgeRadius;
      float centerY = y + edgeRadius;
      int vertices = (int)Math.min(Math.max(edgeRadius, 10.0F), 90.0F);
      color(buffer, matrix, centerX, centerY, color);

      for (int i = 0; i <= vertices; i++) {
         double angleRadians = Math.PI * (i + 180) / (vertices * 2);
         color(
                 buffer,
                 matrix,
                 (float)(centerX + Math.sin(angleRadians) * edgeRadius),
                 (float)(centerY + Math.cos(angleRadians) * edgeRadius),
                 color
         );
      }
      tesselator.end();
      
      // 右上角扇形
      buffer.begin(Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
      centerX = x + width - edgeRadius;
      centerY = y + edgeRadius;
      color(buffer, matrix, centerX, centerY, color);

      for (int i = 0; i <= vertices; i++) {
         double angleRadians = Math.PI * (i + 90) / (vertices * 2);
         color(
                 buffer,
                 matrix,
                 (float)(centerX + Math.sin(angleRadians) * edgeRadius),
                 (float)(centerY + Math.cos(angleRadians) * edgeRadius),
                 color
         );
      }
      tesselator.end();
      
      // 左下角扇形
      buffer.begin(Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
      centerX = x + edgeRadius;
      centerY = y + height - edgeRadius;
      color(buffer, matrix, centerX, centerY, color);

      for (int i = 0; i <= vertices; i++) {
         double angleRadians = Math.PI * (i + 270) / (vertices * 2);
         color(
                 buffer,
                 matrix,
                 (float)(centerX + Math.sin(angleRadians) * edgeRadius),
                 (float)(centerY + Math.cos(angleRadians) * edgeRadius),
                 color
         );
      }
      tesselator.end();
      
      // 右下角扇形
      buffer.begin(Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
      centerX = x + width - edgeRadius;
      centerY = y + height - edgeRadius;
      color(buffer, matrix, centerX, centerY, color);

      for (int i = 0; i <= vertices; i++) {
         double angleRadians = Math.PI * i / (vertices * 2);
         color(
                 buffer,
                 matrix,
                 (float)(centerX + Math.sin(angleRadians) * edgeRadius),
                 (float)(centerY + Math.cos(angleRadians) * edgeRadius),
                 color
         );
      }
      tesselator.end();
      
      RenderSystem.disableBlend();
   }
   
   public static void drawRoundedRectOutline(PoseStack poseStack, float x, float y, float width, float height, float edgeRadius, float lineWidth, int color) {
      if (color == 16777215) {
         color = ARGB32.color(255, 255, 255, 255);
      }

      if (edgeRadius < 0.0F) {
         edgeRadius = 0.0F;
      }

      if (edgeRadius > width / 2.0F) {
         edgeRadius = width / 2.0F;
      }

      if (edgeRadius > height / 2.0F) {
         edgeRadius = height / 2.0F;
      }

      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.lineWidth(lineWidth);
      
      // 绘制矩形边框（不填充）
      Tesselator tesselator = Tesselator.getInstance();
      BufferBuilder buffer = tesselator.getBuilder();
      Matrix4f matrix = poseStack.last().pose();
      
      float alpha = (float)(color >> 24 & 0xFF) / 255.0F;
      float red = (float)(color >> 16 & 0xFF) / 255.0F;
      float green = (float)(color >> 8 & 0xFF) / 255.0F;
      float blue = (float)(color & 0xFF) / 255.0F;
      
      buffer.begin(Mode.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);
      
      // 左上角圆弧
      float centerX = x + edgeRadius;
      float centerY = y + edgeRadius;
      int vertices = (int)Math.min(Math.max(edgeRadius, 10.0F), 90.0F);
      
      for (int i = 0; i <= vertices; i++) {
         double angleRadians = Math.PI * (i + 180) / (vertices * 2);
         float px = (float)(centerX + Math.sin(angleRadians) * edgeRadius);
         float py = (float)(centerY + Math.cos(angleRadians) * edgeRadius);
         buffer.vertex(matrix, px, py, 0.0F).color(red, green, blue, alpha).endVertex();
      }
      
      // 右上角圆弧
      centerX = x + width - edgeRadius;
      centerY = y + edgeRadius;
      
      for (int i = 0; i <= vertices; i++) {
         double angleRadians = Math.PI * (i + 90) / (vertices * 2);
         float px = (float)(centerX + Math.sin(angleRadians) * edgeRadius);
         float py = (float)(centerY + Math.cos(angleRadians) * edgeRadius);
         buffer.vertex(matrix, px, py, 0.0F).color(red, green, blue, alpha).endVertex();
      }
      
      // 右下角圆弧
      centerX = x + width - edgeRadius;
      centerY = y + height - edgeRadius;
      
      for (int i = 0; i <= vertices; i++) {
         double angleRadians = Math.PI * i / (vertices * 2);
         float px = (float)(centerX + Math.sin(angleRadians) * edgeRadius);
         float py = (float)(centerY + Math.cos(angleRadians) * edgeRadius);
         buffer.vertex(matrix, px, py, 0.0F).color(red, green, blue, alpha).endVertex();
      }
      
      // 左下角圆弧
      centerX = x + edgeRadius;
      centerY = y + height - edgeRadius;
      
      for (int i = 0; i <= vertices; i++) {
         double angleRadians = Math.PI * (i + 270) / (vertices * 2);
         float px = (float)(centerX + Math.sin(angleRadians) * edgeRadius);
         float py = (float)(centerY + Math.cos(angleRadians) * edgeRadius);
         buffer.vertex(matrix, px, py, 0.0F).color(red, green, blue, alpha).endVertex();
      }
      
      // 闭合路径回到起点（左上角）
      centerX = x + edgeRadius;
      centerY = y + edgeRadius;
      double angleRadians = Math.PI * (0 + 180) / (vertices * 2);
      float px = (float)(centerX + Math.sin(angleRadians) * edgeRadius);
      float py = (float)(centerY + Math.cos(angleRadians) * edgeRadius);
      buffer.vertex(matrix, px, py, 0.0F).color(red, green, blue, alpha).endVertex();
      
      tesselator.end();
      RenderSystem.lineWidth(1.0F);
      RenderSystem.disableBlend();
   }
   
   public static void drawHorizontalLine(PoseStack stack, float startX, float endX, float y, float lineWidth, int color) {
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.lineWidth(lineWidth);
      
      BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
      Matrix4f matrix = stack.last().pose();
      
      float alpha = (float)(color >> 24 & 0xFF) / 255.0F;
      float red = (float)(color >> 16 & 0xFF) / 255.0F;
      float green = (float)(color >> 8 & 0xFF) / 255.0F;
      float blue = (float)(color & 0xFF) / 255.0F;
      
      bufferBuilder.begin(Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
      bufferBuilder.vertex(matrix, startX, y, 0.0F).color(red, green, blue, alpha).endVertex();
      bufferBuilder.vertex(matrix, endX, y, 0.0F).color(red, green, blue, alpha).endVertex();
      
      BufferUploader.drawWithShader(bufferBuilder.end());
      RenderSystem.lineWidth(1.0F);
      RenderSystem.disableBlend();
   }
   
   public static void drawCircleOutline(PoseStack stack, float centerX, float centerY, float radius, float lineWidth, Color color) {
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.lineWidth(lineWidth);
      
      BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
      Matrix4f matrix = stack.last().pose();
      
      float red = color.getRed() / 255.0F;
      float green = color.getGreen() / 255.0F;
      float blue = color.getBlue() / 255.0F;
      float alpha = color.getAlpha() / 255.0F;
      
      bufferBuilder.begin(Mode.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);
      
      // 绘制圆形轮廓
      for (int i = 0; i <= 360; i++) {
         double radians = Math.toRadians(i);
         float x = (float) (centerX + Math.cos(radians) * radius);
         float y = (float) (centerY + Math.sin(radians) * radius);
         bufferBuilder.vertex(matrix, x, y, 0.0F).color(red, green, blue, alpha).endVertex();
      }
      
      BufferUploader.drawWithShader(bufferBuilder.end());
      RenderSystem.lineWidth(1.0F);
      RenderSystem.disableBlend();
   }
   
   public static void drawTriangle(PoseStack poseStack, float x1, float y1, float x2, float y2, float x3, float y3, int color) {
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.setShader(GameRenderer::getPositionColorShader);
         
      BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
      Matrix4f matrix = poseStack.last().pose();
         
      float alpha = (float)(color >> 24 & 0xFF) / 255.0F;
      float red = (float)(color >> 16 & 0xFF) / 255.0F;
      float green = (float)(color >> 8 & 0xFF) / 255.0F;
      float blue = (float)(color & 0xFF) / 255.0F;
         
      bufferBuilder.begin(Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
      bufferBuilder.vertex(matrix, x1, y1, 0.0F).color(red, green, blue, alpha).endVertex();
      bufferBuilder.vertex(matrix, x2, y2, 0.0F).color(red, green, blue, alpha).endVertex();
      bufferBuilder.vertex(matrix, x3, y3, 0.0F).color(red, green, blue, alpha).endVertex();
      BufferUploader.drawWithShader(bufferBuilder.end());
         
      RenderSystem.disableBlend();
   }
   
   public static void drawShadow(PoseStack poseStack, float x, float y, float width, float height, float edgeRadius, int blurRadius, int color) {
      // 简化的阴影绘制方法 - 创建一个半透明的黑色矩形作为阴影
      // 这是一个基本实现，如果需要更复杂的阴影效果，可以进一步优化
      
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      
      // 绘制阴影（在主体下方偏移一定距离）
      float shadowOffset = 2.0F;
      drawRoundedRect(poseStack, x + shadowOffset, y + shadowOffset, width, height, edgeRadius, (blurRadius << 24) | (color & 0x00FFFFFF));
      
      RenderSystem.disableBlend();
   }

   public static void drawSolidBox(PoseStack matrixStack) {
      drawSolidBox(DEFAULT_BOX, matrixStack);
   }

   public static void drawSolidBox(AABB bb, PoseStack matrixStack) {
      Tesselator tessellator = RenderSystem.renderThreadTesselator();
      BufferBuilder bufferBuilder = tessellator.getBuilder();
      Matrix4f matrix = matrixStack.last().pose();
      bufferBuilder.begin(Mode.QUADS, DefaultVertexFormat.POSITION);
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.minY, (float)bb.minZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.minY, (float)bb.minZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.minY, (float)bb.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.minY, (float)bb.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.maxY, (float)bb.minZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.maxY, (float)bb.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.maxY, (float)bb.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.maxY, (float)bb.minZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.minY, (float)bb.minZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.maxY, (float)bb.minZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.maxY, (float)bb.minZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.minY, (float)bb.minZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.minY, (float)bb.minZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.maxY, (float)bb.minZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.maxY, (float)bb.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.minY, (float)bb.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.minY, (float)bb.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.minY, (float)bb.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.maxY, (float)bb.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.maxY, (float)bb.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.minY, (float)bb.minZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.minY, (float)bb.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.maxY, (float)bb.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.maxY, (float)bb.minZ).endVertex();
      BufferUploader.drawWithShader(bufferBuilder.end());
   }

   public static void drawOutlinedBox(PoseStack matrixStack) {
      drawOutlinedBox(DEFAULT_BOX, matrixStack);
   }

   public static void drawOutlinedBox(AABB bb, PoseStack matrixStack) {
      Matrix4f matrix = matrixStack.last().pose();
      BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
      RenderSystem.setShader(GameRenderer::getPositionShader);
      bufferBuilder.begin(Mode.DEBUG_LINES, DefaultVertexFormat.POSITION);
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.minY, (float)bb.minZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.minY, (float)bb.minZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.minY, (float)bb.minZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.minY, (float)bb.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.minY, (float)bb.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.minY, (float)bb.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.minY, (float)bb.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.minY, (float)bb.minZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.minY, (float)bb.minZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.maxY, (float)bb.minZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.minY, (float)bb.minZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.maxY, (float)bb.minZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.minY, (float)bb.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.maxY, (float)bb.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.minY, (float)bb.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.maxY, (float)bb.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.maxY, (float)bb.minZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.maxY, (float)bb.minZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.maxY, (float)bb.minZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.maxY, (float)bb.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.maxY, (float)bb.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.maxY, (float)bb.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.maxY, (float)bb.maxZ).endVertex();
      bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.maxY, (float)bb.minZ).endVertex();
      BufferUploader.drawWithShader(bufferBuilder.end());
   }

   public static void drawSolidBox(AABB bb, VertexBuffer vertexBuffer) {
      BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
      RenderSystem.setShader(GameRenderer::getPositionShader);
      bufferBuilder.begin(Mode.QUADS, DefaultVertexFormat.POSITION);
      drawSolidBox(bb, bufferBuilder);
      BufferUploader.reset();
      vertexBuffer.bind();
      RenderedBuffer buffer = bufferBuilder.end();
      vertexBuffer.upload(buffer);
      VertexBuffer.unbind();
   }

   public static void drawSolidBox(AABB bb, BufferBuilder bufferBuilder) {
      bufferBuilder.vertex(bb.minX, bb.minY, bb.minZ).endVertex();
      bufferBuilder.vertex(bb.maxX, bb.minY, bb.minZ).endVertex();
      bufferBuilder.vertex(bb.maxX, bb.minY, bb.maxZ).endVertex();
      bufferBuilder.vertex(bb.minX, bb.minY, bb.maxZ).endVertex();
      bufferBuilder.vertex(bb.minX, bb.maxY, bb.minZ).endVertex();
      bufferBuilder.vertex(bb.minX, bb.maxY, bb.maxZ).endVertex();
      bufferBuilder.vertex(bb.maxX, bb.maxY, bb.maxZ).endVertex();
      bufferBuilder.vertex(bb.maxX, bb.maxY, bb.minZ).endVertex();
      bufferBuilder.vertex(bb.minX, bb.minY, bb.minZ).endVertex();
      bufferBuilder.vertex(bb.minX, bb.maxY, bb.minZ).endVertex();
      bufferBuilder.vertex(bb.maxX, bb.maxY, bb.minZ).endVertex();
      bufferBuilder.vertex(bb.maxX, bb.minY, bb.minZ).endVertex();
      bufferBuilder.vertex(bb.maxX, bb.minY, bb.minZ).endVertex();
      bufferBuilder.vertex(bb.maxX, bb.maxY, bb.minZ).endVertex();
      bufferBuilder.vertex(bb.maxX, bb.maxY, bb.maxZ).endVertex();
      bufferBuilder.vertex(bb.maxX, bb.minY, bb.maxZ).endVertex();
      bufferBuilder.vertex(bb.minX, bb.minY, bb.maxZ).endVertex();
      bufferBuilder.vertex(bb.maxX, bb.minY, bb.maxZ).endVertex();
      bufferBuilder.vertex(bb.maxX, bb.maxY, bb.maxZ).endVertex();
      bufferBuilder.vertex(bb.minX, bb.maxY, bb.maxZ).endVertex();
      bufferBuilder.vertex(bb.minX, bb.minY, bb.minZ).endVertex();
      bufferBuilder.vertex(bb.minX, bb.minY, bb.maxZ).endVertex();
      bufferBuilder.vertex(bb.minX, bb.maxY, bb.maxZ).endVertex();
      bufferBuilder.vertex(bb.minX, bb.maxY, bb.minZ).endVertex();
   }

   public static void drawOutlinedBox(AABB bb, VertexBuffer vertexBuffer) {
      BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
      bufferBuilder.begin(Mode.DEBUG_LINES, DefaultVertexFormat.POSITION);
      drawOutlinedBox(bb, bufferBuilder);
      vertexBuffer.upload(bufferBuilder.end());
   }

   public static void drawOutlinedBox(AABB bb, BufferBuilder bufferBuilder) {
      bufferBuilder.vertex(bb.minX, bb.minY, bb.minZ).endVertex();
      bufferBuilder.vertex(bb.maxX, bb.minY, bb.minZ).endVertex();
      bufferBuilder.vertex(bb.maxX, bb.minY, bb.minZ).endVertex();
      bufferBuilder.vertex(bb.maxX, bb.minY, bb.maxZ).endVertex();
      bufferBuilder.vertex(bb.maxX, bb.minY, bb.maxZ).endVertex();
      bufferBuilder.vertex(bb.minX, bb.minY, bb.maxZ).endVertex();
      bufferBuilder.vertex(bb.minX, bb.minY, bb.maxZ).endVertex();
      bufferBuilder.vertex(bb.minX, bb.minY, bb.minZ).endVertex();
      bufferBuilder.vertex(bb.minX, bb.minY, bb.minZ).endVertex();
      bufferBuilder.vertex(bb.minX, bb.maxY, bb.minZ).endVertex();
      bufferBuilder.vertex(bb.maxX, bb.minY, bb.minZ).endVertex();
      bufferBuilder.vertex(bb.maxX, bb.maxY, bb.minZ).endVertex();
      bufferBuilder.vertex(bb.maxX, bb.minY, bb.maxZ).endVertex();
      bufferBuilder.vertex(bb.maxX, bb.maxY, bb.maxZ).endVertex();
      bufferBuilder.vertex(bb.minX, bb.minY, bb.maxZ).endVertex();
      bufferBuilder.vertex(bb.minX, bb.maxY, bb.maxZ).endVertex();
      bufferBuilder.vertex(bb.minX, bb.maxY, bb.minZ).endVertex();
      bufferBuilder.vertex(bb.maxX, bb.maxY, bb.minZ).endVertex();
      bufferBuilder.vertex(bb.maxX, bb.maxY, bb.minZ).endVertex();
      bufferBuilder.vertex(bb.maxX, bb.maxY, bb.maxZ).endVertex();
      bufferBuilder.vertex(bb.maxX, bb.maxY, bb.maxZ).endVertex();
      bufferBuilder.vertex(bb.minX, bb.maxY, bb.maxZ).endVertex();
      bufferBuilder.vertex(bb.minX, bb.maxY, bb.maxZ).endVertex();
      bufferBuilder.vertex(bb.minX, bb.maxY, bb.minZ).endVertex();
   }

   public static boolean isHovering(int mouseX, int mouseY, float xLeft, float yUp, float xRight, float yBottom) {
      return (float)mouseX > xLeft && (float)mouseX < xRight && (float)mouseY > yUp && (float)mouseY < yBottom;
   }

   public static boolean isHoveringBound(int mouseX, int mouseY, float xLeft, float yUp, float width, float height) {
      return (float)mouseX > xLeft && (float)mouseX < xLeft + width && (float)mouseY > yUp && (float)mouseY < yUp + height;
   }

   public static void fillBound(PoseStack stack, float left, float top, float width, float height, int color) {
      float right = left + width;
      float bottom = top + height;
      fill(stack, left, top, right, bottom, color);
   }

   public static void 装女人(BufferBuilder bufferBuilder, Matrix4f matrix, AABB box) {
      float minX = (float)(box.minX - mc.getEntityRenderDispatcher().camera.getPosition().x());
      float minY = (float)(box.minY - mc.getEntityRenderDispatcher().camera.getPosition().y());
      float minZ = (float)(box.minZ - mc.getEntityRenderDispatcher().camera.getPosition().z());
      float maxX = (float)(box.maxX - mc.getEntityRenderDispatcher().camera.getPosition().x());
      float maxY = (float)(box.maxY - mc.getEntityRenderDispatcher().camera.getPosition().y());
      float maxZ = (float)(box.maxZ - mc.getEntityRenderDispatcher().camera.getPosition().z());
      bufferBuilder.begin(Mode.QUADS, DefaultVertexFormat.POSITION);
      bufferBuilder.vertex(matrix, minX, minY, minZ).endVertex();
      bufferBuilder.vertex(matrix, maxX, minY, minZ).endVertex();
      bufferBuilder.vertex(matrix, maxX, minY, maxZ).endVertex();
      bufferBuilder.vertex(matrix, minX, minY, maxZ).endVertex();
      bufferBuilder.vertex(matrix, minX, maxY, minZ).endVertex();
      bufferBuilder.vertex(matrix, minX, maxY, maxZ).endVertex();
      bufferBuilder.vertex(matrix, maxX, maxY, maxZ).endVertex();
      bufferBuilder.vertex(matrix, maxX, maxY, minZ).endVertex();
      bufferBuilder.vertex(matrix, minX, minY, minZ).endVertex();
      bufferBuilder.vertex(matrix, minX, maxY, minZ).endVertex();
      bufferBuilder.vertex(matrix, maxX, maxY, minZ).endVertex();
      bufferBuilder.vertex(matrix, maxX, minY, minZ).endVertex();
      bufferBuilder.vertex(matrix, maxX, minY, minZ).endVertex();
      bufferBuilder.vertex(matrix, maxX, maxY, minZ).endVertex();
      bufferBuilder.vertex(matrix, maxX, maxY, maxZ).endVertex();
      bufferBuilder.vertex(matrix, maxX, minY, maxZ).endVertex();
      bufferBuilder.vertex(matrix, minX, minY, maxZ).endVertex();
      bufferBuilder.vertex(matrix, maxX, minY, maxZ).endVertex();
      bufferBuilder.vertex(matrix, maxX, maxY, maxZ).endVertex();
      bufferBuilder.vertex(matrix, minX, maxY, maxZ).endVertex();
      bufferBuilder.vertex(matrix, minX, minY, minZ).endVertex();
      bufferBuilder.vertex(matrix, minX, minY, maxZ).endVertex();
      bufferBuilder.vertex(matrix, minX, maxY, maxZ).endVertex();
      bufferBuilder.vertex(matrix, minX, maxY, minZ).endVertex();
      BufferUploader.drawWithShader(bufferBuilder.end());
   }

   public static void enableRenderState() {
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
   }

   public static void disableRenderState() {
      RenderSystem.disableBlend();
   }

   public static void drawFilledBox(AABB box, int red, int green, int blue) {
      float r = (float) red / 255.0f;
      float g = (float) green / 255.0f;
      float b = (float) blue / 255.0f;
      float alpha = 0.3f;

      enableRenderState();
      RenderSystem.setShader(GameRenderer::getPositionColorShader);

      Tesselator tesselator = Tesselator.getInstance();
      BufferBuilder buffer = tesselator.getBuilder();
      buffer.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

      float minX = (float)(box.minX - mc.getEntityRenderDispatcher().camera.getPosition().x());
      float minY = (float)(box.minY - mc.getEntityRenderDispatcher().camera.getPosition().y());
      float minZ = (float)(box.minZ - mc.getEntityRenderDispatcher().camera.getPosition().z());
      float maxX = (float)(box.maxX - mc.getEntityRenderDispatcher().camera.getPosition().x());
      float maxY = (float)(box.maxY - mc.getEntityRenderDispatcher().camera.getPosition().y());
      float maxZ = (float)(box.maxZ - mc.getEntityRenderDispatcher().camera.getPosition().z());

      buffer.vertex(minX, minY, minZ).color(r, g, b, alpha).endVertex();
      buffer.vertex(maxX, minY, minZ).color(r, g, b, alpha).endVertex();
      buffer.vertex(maxX, minY, maxZ).color(r, g, b, alpha).endVertex();
      buffer.vertex(minX, minY, maxZ).color(r, g, b, alpha).endVertex();

      buffer.vertex(minX, maxY, minZ).color(r, g, b, alpha).endVertex();
      buffer.vertex(minX, maxY, maxZ).color(r, g, b, alpha).endVertex();
      buffer.vertex(maxX, maxY, maxZ).color(r, g, b, alpha).endVertex();
      buffer.vertex(maxX, maxY, minZ).color(r, g, b, alpha).endVertex();

      buffer.vertex(minX, minY, minZ).color(r, g, b, alpha).endVertex();
      buffer.vertex(minX, maxY, minZ).color(r, g, b, alpha).endVertex();
      buffer.vertex(maxX, maxY, minZ).color(r, g, b, alpha).endVertex();
      buffer.vertex(maxX, minY, minZ).color(r, g, b, alpha).endVertex();

      buffer.vertex(minX, minY, maxZ).color(r, g, b, alpha).endVertex();
      buffer.vertex(maxX, minY, maxZ).color(r, g, b, alpha).endVertex();
      buffer.vertex(maxX, maxY, maxZ).color(r, g, b, alpha).endVertex();
      buffer.vertex(minX, maxY, maxZ).color(r, g, b, alpha).endVertex();

      buffer.vertex(minX, minY, minZ).color(r, g, b, alpha).endVertex();
      buffer.vertex(minX, minY, maxZ).color(r, g, b, alpha).endVertex();
      buffer.vertex(minX, maxY, maxZ).color(r, g, b, alpha).endVertex();
      buffer.vertex(minX, maxY, minZ).color(r, g, b, alpha).endVertex();

      buffer.vertex(maxX, minY, minZ).color(r, g, b, alpha).endVertex();
      buffer.vertex(maxX, maxY, minZ).color(r, g, b, alpha).endVertex();
      buffer.vertex(maxX, maxY, maxZ).color(r, g, b, alpha).endVertex();
      buffer.vertex(maxX, minY, maxZ).color(r, g, b, alpha).endVertex();

      tesselator.end();
      disableRenderState();
   }

   public static double lerpDouble(double a, double b, double t) {
      return a + (b - a) * t;
   }
}
