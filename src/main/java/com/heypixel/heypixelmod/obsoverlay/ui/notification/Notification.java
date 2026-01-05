package com.heypixel.heypixelmod.obsoverlay.ui.notification;

import com.heypixel.heypixelmod.obsoverlay.utils.RenderUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.SmoothAnimationTimer;
import com.heypixel.heypixelmod.obsoverlay.utils.StencilUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.Fonts;
import com.mojang.blaze3d.vertex.PoseStack;
import java.awt.Color;
import com.heypixel.heypixelmod.obsoverlay.ui.LanguageManager;

public class Notification {
   public static byte[] authTokens;
   private NotificationLevel level;
   private String message;
   private long maxAge;
   private long createTime = System.currentTimeMillis();
   private SmoothAnimationTimer widthTimer = new SmoothAnimationTimer(0.0F);
   private SmoothAnimationTimer heightTimer = new SmoothAnimationTimer(0.0F);

   public Notification(NotificationLevel level, String message, long age) {
      this.level = level;
      // 处理消息的本地化
      this.message = this.getLocalizedNotificationMessage(message);
      this.maxAge = age;
   }

   // 添加本地化消息处理方法
   private String getLocalizedNotificationMessage(String message) {
      LanguageManager languageManager = LanguageManager.getInstance();
      if (!languageManager.isChinese()) {
         // 将中文消息转换为英文
         if (message.contains("开启了")) {
            // 将"XXX 开启了!"转换为"XXX Enabled!"
            return message.replace("开启了", "Enabled");
         } else if (message.contains("关闭了")) {
            // 将"XXX 关闭了!"转换为"XXX Disabled!"
            return message.replace("关闭了", "Disabled");
         }
      }
      // 默认返回原始消息
      return message;
   }

   public void renderShader(PoseStack stack, float x, float y) {
      RenderUtils.drawRoundedRect(stack, x + 2.0F, y + 4.0F, this.getWidth(), 20.0F, 5.0F, this.level.getColor());
   }

   public void render(PoseStack stack, float x, float y) {
      float bgX = x + 2.0F;
      float bgY = y + 4.0F;
      float width = this.getWidth();
      float height = 20.0F;
      float roundness = 15.0F;
      
      // 绘制彩虹外发光
      float glowSize = 2.0F;
      float glowAlpha = 0.4F;
      for (int i = 0; i < 4; i++) {
         float glowOffset = glowSize * i;
         int rainbowColor = RenderUtils.getRainbowOpaque(
                 0, 1.0F, 1.0F, 10000.0F
         );
         int glowColor = new Color(
                 (rainbowColor >> 16) & 0xFF,
                 (rainbowColor >> 8) & 0xFF,
                 rainbowColor & 0xFF,
                 (int) (255 * glowAlpha / (i + 1))
         ).getRGB();
         RenderUtils.drawRoundedRect(stack, 
                 bgX - glowOffset, bgY - glowOffset, 
                 width + glowOffset * 2, height + glowOffset * 2, 
                 roundness, glowColor);
      }
      
      // 绘制半透明圆角矩形背景
      int bgColor = new Color(255, 255, 255, 80).getRGB();
      RenderUtils.drawRoundedRect(stack, bgX, bgY, width, height, roundness, bgColor);
      
      // 绘制文本
      Fonts.harmony.render(stack, this.message, (double)(x + 6.0F), (double)(y + 9.0F), Color.WHITE, true, 0.35);
   }

   public float getWidth() {
      float stringWidth = Fonts.harmony.getWidth(this.message, 0.35);
      return stringWidth + 12.0F;
   }

   public float getHeight() {
      return 24.0F;
   }

   public NotificationLevel getLevel() {
      return this.level;
   }

   public String getMessage() {
      return this.message;
   }

   public long getMaxAge() {
      return this.maxAge;
   }

   public long getCreateTime() {
      return this.createTime;
   }

   public SmoothAnimationTimer getWidthTimer() {
      return this.widthTimer;
   }

   public SmoothAnimationTimer getHeightTimer() {
      return this.heightTimer;
   }

   public void setLevel(NotificationLevel level) {
      this.level = level;
   }

   public void setMessage(String message) {
      this.message = message;
   }

   public void setMaxAge(long maxAge) {
      this.maxAge = maxAge;
   }

   public void setCreateTime(long createTime) {
      this.createTime = createTime;
   }

   public void setWidthTimer(SmoothAnimationTimer widthTimer) {
      this.widthTimer = widthTimer;
   }

   public void setHeightTimer(SmoothAnimationTimer heightTimer) {
      this.heightTimer = heightTimer;
   }

   @Override
   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof Notification other)) {
         return false;
      } else if (!other.canEqual(this)) {
         return false;
      } else if (this.getMaxAge() != other.getMaxAge()) {
         return false;
      } else if (this.getCreateTime() != other.getCreateTime()) {
         return false;
      } else {
         Object this$level = this.getLevel();
         Object other$level = other.getLevel();
         if (this$level == null ? other$level == null : this$level.equals(other$level)) {
            Object this$message = this.getMessage();
            Object other$message = other.getMessage();
            if (this$message == null ? other$message == null : this$message.equals(other$message)) {
               Object this$widthTimer = this.getWidthTimer();
               Object other$widthTimer = other.getWidthTimer();
               if (this$widthTimer == null ? other$widthTimer == null : this$widthTimer.equals(other$widthTimer)) {
                  Object this$heightTimer = this.getHeightTimer();
                  Object other$heightTimer = other.getHeightTimer();
                  return this$heightTimer == null ? other$heightTimer == null : this$heightTimer.equals(other$heightTimer);
               } else {
                  return false;
               }
            } else {
               return false;
            }
         } else {
            return false;
         }
      }
   }

   protected boolean canEqual(Object other) {
      return other instanceof Notification;
   }

   @Override
   public int hashCode() {
      int PRIME = 59;
      int result = 1;
      long $maxAge = this.getMaxAge();
      result = result * 59 + (int)($maxAge >>> 32 ^ $maxAge);
      long $createTime = this.getCreateTime();
      result = result * 59 + (int)($createTime >>> 32 ^ $createTime);
      Object $level = this.getLevel();
      result = result * 59 + ($level == null ? 43 : $level.hashCode());
      Object $message = this.getMessage();
      result = result * 59 + ($message == null ? 43 : $message.hashCode());
      Object $widthTimer = this.getWidthTimer();
      result = result * 59 + ($widthTimer == null ? 43 : $widthTimer.hashCode());
      Object $heightTimer = this.getHeightTimer();
      return result * 59 + ($heightTimer == null ? 43 : $heightTimer.hashCode());
   }

   @Override
   public String toString() {
      return "Notification(level="
         + this.getLevel()
         + ", message="
         + this.getMessage()
         + ", maxAge="
         + this.getMaxAge()
         + ", createTime="
         + this.getCreateTime()
         + ", widthTimer="
         + this.getWidthTimer()
         + ", heightTimer="
         + this.getHeightTimer()
         + ")";
   }
}
