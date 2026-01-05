package com.heypixel.heypixelmod.modules.impl.render;

import com.heypixel.heypixelmod.events.api.EventTarget;
import com.heypixel.heypixelmod.events.api.types.EventType;
import com.heypixel.heypixelmod.events.impl.EventShader;
import com.heypixel.heypixelmod.modules.Category;
import com.heypixel.heypixelmod.modules.Module;
import com.heypixel.heypixelmod.modules.ModuleInfo;
import com.heypixel.heypixelmod.utils.RenderUtils;
import com.heypixel.heypixelmod.values.ValueBuilder;
import com.heypixel.heypixelmod.values.impl.BooleanValue;
import com.heypixel.heypixelmod.values.impl.FloatValue;

@ModuleInfo(
   name = "Scoreboard",
   description = "Modifies the scoreboard",
   category = Category.RENDER
)
public class Scoreboard extends Module {
   public BooleanValue hideScore = ValueBuilder.create(this, "Hide Red Score").setDefaultBooleanValue(true).build().getBooleanValue();

   public FloatValue down = ValueBuilder.create(this, "Down")
      .setDefaultFloatValue(120.0F)
      .setFloatStep(1.0F)
      .setMinFloatValue(0.0F)
      .setMaxFloatValue(300.0F)
      .build()
      .getFloatValue();

   public BooleanValue blur = ValueBuilder.create(this, "Blur Background")
      .setDefaultBooleanValue(true)
      .build()
      .getBooleanValue();

   public BooleanValue roundedCorner = ValueBuilder.create(this, "Rounded Corner")
      .setDefaultBooleanValue(true)
      .build()
      .getBooleanValue();

   public FloatValue cornerRadius = ValueBuilder.create(this, "Corner Radius")
      .setDefaultFloatValue(5.0F)
      .setFloatStep(0.5F)
      .setMinFloatValue(0.0F)
      .setMaxFloatValue(10.5F)
      .build()
      .getFloatValue();

   private float scoreboardMinX = Float.MAX_VALUE;
   private float scoreboardMinY = Float.MAX_VALUE;
   private float scoreboardMaxX = Float.MIN_VALUE;
   private float scoreboardMaxY = Float.MIN_VALUE;
   private boolean hasScoreboardData = false;
   private int backgroundColor = 0;
   private boolean backgroundRendered = false;

   public float getXOffset() {
      return 0.0F;
   }

   public float getYOffset() {
      return this.down.getCurrentValue();
   }

   public float getScreenX() {
      return this.scoreboardMinX + getXOffset();
   }

   public float getScreenY() {
      return this.scoreboardMinY + getYOffset();
   }

   public float getWidth() {
      return this.scoreboardMaxX - this.scoreboardMinX;
   }

   public float getHeight() {
      return this.scoreboardMaxY - this.scoreboardMinY;
   }

   public void addScoreboardBounds(float x1, float y1, float x2, float y2, int color) {
      this.scoreboardMinX = Math.min(this.scoreboardMinX, Math.min(x1, x2));
      this.scoreboardMinY = Math.min(this.scoreboardMinY, Math.min(y1, y2));
      this.scoreboardMaxX = Math.max(this.scoreboardMaxX, Math.max(x1, x2));
      this.scoreboardMaxY = Math.max(this.scoreboardMaxY, Math.max(y1, y2));
      this.backgroundColor = color;
      this.hasScoreboardData = true;
   }

   public boolean shouldRenderRoundedBackground() {
      return this.isEnabled() && this.roundedCorner.getCurrentValue() && this.hasScoreboardData;
   }

   public void clearScoreboardData() {
      this.hasScoreboardData = false;
      this.backgroundRendered = false;
      this.scoreboardMinX = Float.MAX_VALUE;
      this.scoreboardMinY = Float.MAX_VALUE;
      this.scoreboardMaxX = Float.MIN_VALUE;
      this.scoreboardMaxY = Float.MIN_VALUE;
   }

   public void renderBackgroundIfNeeded(com.mojang.blaze3d.vertex.PoseStack poseStack) {
      if (!this.backgroundRendered && this.hasScoreboardData && this.roundedCorner.getCurrentValue()) {

         this.backgroundRendered = true;
      }
   }

   @EventTarget
   public void onShader(EventShader e) {
      if (!this.hasScoreboardData) {
         return;
      }

      if (e.getType() == EventType.BLUR && this.blur.getCurrentValue()) {
         float radius = this.roundedCorner.getCurrentValue() ? this.cornerRadius.getCurrentValue() : 0.0F;
         float width = this.scoreboardMaxX - this.scoreboardMinX;
         float height = this.scoreboardMaxY - this.scoreboardMinY;

         float xOffset = this.getXOffset();
         float yOffset = this.getYOffset();
         RenderUtils.drawRoundedRect(
            e.getStack(),
            this.scoreboardMinX + xOffset,
            this.scoreboardMinY + yOffset,
            width,
            height,
            radius,
            Integer.MIN_VALUE
         );
      }
   }

   public void renderRoundedBackground(com.mojang.blaze3d.vertex.PoseStack poseStack) {
      if (!this.hasScoreboardData || !this.roundedCorner.getCurrentValue()) {
         return;
      }

      float radius = this.cornerRadius.getCurrentValue();
      float width = this.scoreboardMaxX - this.scoreboardMinX;
      float height = this.scoreboardMaxY - this.scoreboardMinY;

      RenderUtils.drawRoundedRect(
         poseStack,
         this.scoreboardMinX,
         this.scoreboardMinY,
         width,
         height,
         radius,
         this.backgroundColor
      );
   }
}
