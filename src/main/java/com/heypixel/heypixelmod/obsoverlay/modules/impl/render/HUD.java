package com.heypixel.heypixelmod.obsoverlay.modules.impl.render;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.Version;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRender2D;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventShader;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleManager;
import com.heypixel.heypixelmod.obsoverlay.utils.RenderUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.SmoothAnimationTimer;
import com.heypixel.heypixelmod.obsoverlay.utils.StencilUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.Fonts;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.text.CustomTextRenderer;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.ModeValue;
import java.awt.Color;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.joml.Vector4f;
import com.heypixel.heypixelmod.obsoverlay.ui.LanguageManager;

@ModuleInfo(
        name = "HUD",
        description = "在屏幕上显示信息",
        category = Category.RENDER
)
public class HUD extends Module {
   public static final int headerColor = new Color(30, 30, 30, 220).getRGB(); // 深灰色标题栏
   public static final int bodyColor = new Color(20, 20, 20, 160).getRGB(); // 半透明深色背景
   public static final int backgroundColor = new Color(0, 0, 0, 60).getRGB();
   public static final int accentColor = new Color(0, 200, 255, 255).getRGB(); // 蓝青色强调色
   private static final SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
   public BooleanValue waterMark = ValueBuilder.create(this, "水印").setDefaultBooleanValue(true).build().getBooleanValue();
   public FloatValue watermarkSize = ValueBuilder.create(this, "水印大小")
           .setVisibility(this.waterMark::getCurrentValue)
           .setDefaultFloatValue(0.4F)
           .setFloatStep(0.01F)
           .setMinFloatValue(0.1F)
           .setMaxFloatValue(1.0F)
           .build()
           .getFloatValue();
   public BooleanValue moduleToggleSound = ValueBuilder.create(this, "模块切换声音").setDefaultBooleanValue(true).build().getBooleanValue();
   public BooleanValue notification = ValueBuilder.create(this, "通知").setDefaultBooleanValue(true).build().getBooleanValue();
   public BooleanValue arrayList = ValueBuilder.create(this, "功能列表").setDefaultBooleanValue(true).build().getBooleanValue();
   public BooleanValue prettyModuleName = ValueBuilder.create(this, "漂亮的模块名称")
           .setOnUpdate(value -> Module.update = true)
           .setVisibility(this.arrayList::getCurrentValue)
           .setDefaultBooleanValue(false)
           .build()
           .getBooleanValue();
   public BooleanValue hideRenderModules = ValueBuilder.create(this, "隐藏渲染模块")
           .setOnUpdate(value -> Module.update = true)
           .setVisibility(this.arrayList::getCurrentValue)
           .setDefaultBooleanValue(false)
           .build()
           .getBooleanValue();
   public BooleanValue rainbow = ValueBuilder.create(this, "彩虹")
           .setDefaultBooleanValue(true)
           .setVisibility(this.arrayList::getCurrentValue)
           .build()
           .getBooleanValue();
   public FloatValue rainbowSpeed = ValueBuilder.create(this, "彩虹变色速度")
           .setVisibility(this.arrayList::getCurrentValue)
           .setMinFloatValue(1.0F)
           .setMaxFloatValue(20.0F)
           .setDefaultFloatValue(10.0F)
           .setFloatStep(0.1F)
           .build()
           .getFloatValue();
   public FloatValue rainbowOffset = ValueBuilder.create(this, "彩虹偏移")
           .setVisibility(this.arrayList::getCurrentValue)
           .setMinFloatValue(1.0F)
           .setMaxFloatValue(20.0F)
           .setDefaultFloatValue(10.0F)
           .setFloatStep(0.1F)
           .build()
           .getFloatValue();
   public ModeValue arrayListDirection = ValueBuilder.create(this, "功能列表方向")
           .setVisibility(this.arrayList::getCurrentValue)
           .setDefaultModeIndex(0)
           .setModes("向右", "向左")
           .build()
           .getModeValue();
   public FloatValue xOffset = ValueBuilder.create(this, "X偏移")
           .setVisibility(this.arrayList::getCurrentValue)
           .setMinFloatValue(-100.0F)
           .setMaxFloatValue(100.0F)
           .setDefaultFloatValue(1.0F)
           .setFloatStep(1.0F)
           .build()
           .getFloatValue();
   public FloatValue yOffset = ValueBuilder.create(this, "Y偏移")
           .setVisibility(this.arrayList::getCurrentValue)
           .setMinFloatValue(1.0F)
           .setMaxFloatValue(100.0F)
           .setDefaultFloatValue(1.0F)
           .setFloatStep(1.0F)
           .build()
           .getFloatValue();
   public FloatValue arrayListSize = ValueBuilder.create(this, "功能表列大小")
           .setVisibility(this.arrayList::getCurrentValue)
           .setDefaultFloatValue(0.4F)
           .setFloatStep(0.01F)
           .setMinFloatValue(0.1F)
           .setMaxFloatValue(1.0F)
           .build()
           .getFloatValue();
   List<Module> renderModules;
   float width;
   float watermarkHeight;
   List<Vector4f> blurMatrices = new ArrayList<>();

   public String getLocalizedName() {
      return LanguageManager.getInstance().getLocalizedString("HUD", "HUD");
   }

   public String getLocalizedDescription() {
      return LanguageManager.getInstance().getLocalizedString("在屏幕上显示信息", "Display information on the screen");
   }

   public String getModuleDisplayName(Module module) {
      String name = this.prettyModuleName.getCurrentValue() ? module.getPrettyName() : module.getName();
      return name + (module.getSuffix() == null ? "" : " §7" + module.getSuffix());
   }

   @EventTarget
   public void notification(EventRender2D e) {
      if (this.notification.getCurrentValue()) {
         Naven.getInstance().getNotificationManager().onRender(e);
      }
   }

   @EventTarget
   public void onShader(EventShader e) {
      if (this.notification.getCurrentValue() && e.getType() == EventType.SHADOW) {
         Naven.getInstance().getNotificationManager().onRenderShadow(e);
      }

      if (this.waterMark.getCurrentValue()) {
         RenderUtils.drawRoundedRect(e.getStack(), 5.0F, 5.0F, this.width, this.watermarkHeight + 8.0F, 5.0F, Integer.MIN_VALUE);
      }

      if (this.arrayList.getCurrentValue()) {
         for (Vector4f blurMatrix : this.blurMatrices) {
            RenderUtils.fillBound(e.getStack(), blurMatrix.x(), blurMatrix.y(), blurMatrix.z(), blurMatrix.w(), 1073741824);
         }
      }
   }

   @EventTarget
   public void onRender(EventRender2D e) {
      CustomTextRenderer font = Fonts.opensans;
      if (this.waterMark.getCurrentValue()) {
         e.getStack().pushPose();
         String text = "Naven | " + Version.getVersion() + " | GPT5 | " + StringUtils.split(mc.fpsString, " ")[0] + " FPS | " + format.format(new Date());
         this.width = font.getWidth(text, (double)this.watermarkSize.getCurrentValue()) + 14.0F;
         this.watermarkHeight = (float)font.getHeight(true, (double)this.watermarkSize.getCurrentValue());

         // 绘制现代化的水印背景
         RenderUtils.drawRoundedRect(e.getStack(), 5.0F, 5.0F, this.width, this.watermarkHeight + 8.0F, 3.0F, bodyColor);

         // 绘制顶部强调色条
         RenderUtils.fill(e.getStack(), 5.0F, 5.0F, 5.0F + this.width, 7.0F, accentColor);

         // 绘制文本
         font.render(e.getStack(), text, 12.0, 10.0, Color.WHITE, true, (double)this.watermarkSize.getCurrentValue());
         e.getStack().popPose();
      }

      this.blurMatrices.clear();
      if (this.arrayList.getCurrentValue()) {
         e.getStack().pushPose();
         ModuleManager moduleManager = Naven.getInstance().getModuleManager();
         if (update || this.renderModules == null) {
            this.renderModules = new ArrayList<>(moduleManager.getModules());
            if (this.hideRenderModules.getCurrentValue()) {
               this.renderModules.removeIf(modulex -> modulex.getCategory() == Category.RENDER);
            }

            this.renderModules.sort((o1, o2) -> {
               float o1Width = font.getWidth(this.getModuleDisplayName(o1), (double)this.arrayListSize.getCurrentValue());
               float o2Width = font.getWidth(this.getModuleDisplayName(o2), (double)this.arrayListSize.getCurrentValue());
               return Float.compare(o2Width, o1Width);
            });
         }

         float maxWidth = this.renderModules.isEmpty()
                 ? 0.0F
                 : font.getWidth(this.getModuleDisplayName(this.renderModules.get(0)), (double)this.arrayListSize.getCurrentValue());
         float arrayListX = this.arrayListDirection.isCurrentMode("向右")
                 ? (float)mc.getWindow().getGuiScaledWidth() - maxWidth - 6.0F + this.xOffset.getCurrentValue()
                 : 3.0F + this.xOffset.getCurrentValue();
         float arrayListY = this.yOffset.getCurrentValue();
         float height = 0.0F;
         double fontHeight = font.getHeight(true, (double)this.arrayListSize.getCurrentValue());

         for (Module module : this.renderModules) {
            SmoothAnimationTimer animation = module.getAnimation();
            if (module.isEnabled()) {
               animation.target = 100.0F;
            } else {
               animation.target = 0.0F;
            }

            animation.update(true);
            if (animation.value > 0.0F) {
               String displayName = this.getModuleDisplayName(module);
               float stringWidth = font.getWidth(displayName, (double)this.arrayListSize.getCurrentValue());
               float left = -stringWidth * (1.0F - animation.value / 100.0F);
               float right = maxWidth - stringWidth * (animation.value / 100.0F);
               float innerX = this.arrayListDirection.isCurrentMode("向左") ? left : right;

               // 绘制模块背景
               RenderUtils.fillBound(
                       e.getStack(),
                       arrayListX + innerX,
                       arrayListY + height + 1.0F,
                       stringWidth + 4.0F,
                       (float)((double)(animation.value / 100.0F) * fontHeight) + 1.0F,
                       bodyColor
               );

               // 绘制左侧强调色条
               if (!this.rainbow.getCurrentValue()) {
                  RenderUtils.fillBound(
                          e.getStack(),
                          arrayListX + innerX,
                          arrayListY + height + 1.0F,
                          2.0F,
                          (float)((double)(animation.value / 100.0F) * fontHeight) + 1.0F,
                          accentColor
                  );
               }

               this.blurMatrices
                       .add(
                               new Vector4f(arrayListX + innerX, arrayListY + height + 1.0F, stringWidth + 4.0F, (float)((double)(animation.value / 100.0F) * fontHeight) + 1.0F)
                       );

               int color = -1;
               if (this.rainbow.getCurrentValue()) {
                  color = RenderUtils.getRainbowOpaque(
                          (int)(-height * this.rainbowOffset.getCurrentValue()), 1.0F, 1.0F, (21.0F - this.rainbowSpeed.getCurrentValue()) * 1000.0F
                  );
               }

               float alpha = animation.value / 100.0F;
               font.setAlpha(alpha);
               font.render(
                       e.getStack(),
                       displayName,
                       (double)(arrayListX + innerX + 3.0F),
                       (double)(arrayListY + height + 1.5F),
                       new Color(color),
                       true,
                       (double)this.arrayListSize.getCurrentValue()
               );
               height += (float)((double)(animation.value / 100.0F) * fontHeight);
            }
         }

         font.setAlpha(1.0F);
         e.getStack().popPose();
      }
   }

   public Color getColor(long time) {
      float hue = (float)(time % (long)((int)((21.0F - this.rainbowSpeed.getCurrentValue()) * 1000.0F))) / ((21.0F - this.rainbowSpeed.getCurrentValue()) * 1000.0F);
      return Color.getHSBColor(hue, 1.0F, 1.0F);
   }
}
