package com.heypixel.heypixelmod.obsoverlay.modules;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.render.ClickGUIModule;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.render.DynamicIsland;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.render.HUD;
import com.heypixel.heypixelmod.obsoverlay.ui.notification.Notification;
import com.heypixel.heypixelmod.obsoverlay.ui.notification.NotificationLevel;
import com.heypixel.heypixelmod.obsoverlay.utils.SmoothAnimationTimer;
import com.heypixel.heypixelmod.obsoverlay.values.HasValue;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvents;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.render.NewHUD;
import com.heypixel.heypixelmod.obsoverlay.ui.LanguageManager;

public class Module extends HasValue {
   protected static final Minecraft mc = Minecraft.getInstance();
   public static boolean update = true;
   private final SmoothAnimationTimer animation = new SmoothAnimationTimer(100.0F);
   private String name;
   private String prettyName;
   private String description;
   private String suffix;
   private Category category;
   private boolean enabled;
   private int minPermission = 0;
   private int key;

   public Module(String name, String description, Category category) {
      this.name = name;
      this.description = description;
      this.category = category;
      super.setName(name);
      this.setPrettyName();
   }

   public void setSuffix(String suffix) {
      if (suffix == null) {
         this.suffix = null;
         update = true;
      } else if (!suffix.equals(this.suffix)) {
         this.suffix = suffix;
         update = true;
      }
   }

   private void setPrettyName() {
      StringBuilder builder = new StringBuilder();
      char[] chars = this.name.toCharArray();

      for (int i = 0; i < chars.length - 1; i++) {
         if (Character.isLowerCase(chars[i]) && Character.isUpperCase(chars[i + 1])) {
            builder.append(chars[i]).append(" ");
         } else {
            builder.append(chars[i]);
         }
      }

      builder.append(chars[chars.length - 1]);
      this.prettyName = builder.toString();
   }

   protected void initModule() {
      if (this.getClass().isAnnotationPresent(ModuleInfo.class)) {
         ModuleInfo moduleInfo = this.getClass().getAnnotation(ModuleInfo.class);
         this.name = moduleInfo.name();
         this.description = moduleInfo.description();
         this.category = moduleInfo.category();
         super.setName(this.name);
         this.setPrettyName();
         Naven.getInstance().getHasValueManager().registerHasValue(this);
      }
   }

   public void onEnable() {
   }

   public void onDisable() {
   }

   public void setEnabled(boolean enabled) {
      try {
         Naven naven = Naven.getInstance();
         if (enabled) {
            this.enabled = true;
            naven.getEventManager().register(this);
            this.onEnable();
            if (!(this instanceof ClickGUIModule)) {
               HUD module = (HUD)Naven.getInstance().getModuleManager().getModule(HUD.class);
               if (module.moduleToggleSound.getCurrentValue()) {
                  mc.player.playSound(SoundEvents.WOODEN_BUTTON_CLICK_ON, 0.5F, 1.3F);
               }

               // 添加更好的模块开关提示
               NewHUD newHud = (NewHUD)Naven.getInstance().getModuleManager().getModule(NewHUD.class);
               // 处理通知消息的本地化 - 动态检查当前语言设置
               String notificationMessage = this.name + " 开启了!";
               if (!LanguageManager.getInstance().isChinese()) {
                   notificationMessage = this.getLocalizedName() + " Enabled!";
               }
               newHud.addModuleToggleNotification(notificationMessage, this);

               // 只有当"更好的功能开关提示"未启用时才显示右下角通知
               if (!newHud.betterModuleToggleNotification.getCurrentValue()) {
                  Notification notification = new Notification(NotificationLevel.SUCCESS, notificationMessage, 3000L);
                  naven.getNotificationManager().addNotification(notification);
               }
                
               // 通知DynamicIsland模块
               DynamicIsland dynamicIsland = (DynamicIsland)Naven.getInstance().getModuleManager().getModule(DynamicIsland.class);
               if (dynamicIsland != null && dynamicIsland.isEnabled()) {
                   dynamicIsland.onModuleEnable(this);
               }
            }
         } else {
            this.enabled = false;
            naven.getEventManager().unregister(this);
            this.onDisable();
            if (!(this instanceof ClickGUIModule)) {
               HUD module = (HUD)Naven.getInstance().getModuleManager().getModule(HUD.class);
               if (module.moduleToggleSound.getCurrentValue()) {
                  mc.player.playSound(SoundEvents.WOODEN_BUTTON_CLICK_OFF, 0.5F, 0.8F);
               }

               // 添加更好的模块开关提示
               NewHUD newHud = (NewHUD)Naven.getInstance().getModuleManager().getModule(NewHUD.class);
               // 处理通知消息的本地化 - 动态检查当前语言设置
               String notificationMessage = this.name + " 关闭了!";
               if (!LanguageManager.getInstance().isChinese()) {
                   notificationMessage = this.getLocalizedName() + " Disabled!";
               }
               newHud.addModuleToggleNotification(notificationMessage, this);

               // 只有当"更好的功能开关提示"未启用时才显示右下角通知
               if (!newHud.betterModuleToggleNotification.getCurrentValue()) {
                  Notification notification = new Notification(NotificationLevel.ERROR, notificationMessage, 3000L);
                  naven.getNotificationManager().addNotification(notification);
               }
                
               // 通知DynamicIsland模块
               DynamicIsland dynamicIsland = (DynamicIsland)Naven.getInstance().getModuleManager().getModule(DynamicIsland.class);
               if (dynamicIsland != null && dynamicIsland.isEnabled()) {
                   dynamicIsland.onModuleDisable(this);
               }
            }
         }
      } catch (Exception var5) {
         var5.printStackTrace();
      }
   }

   // 添加获取本地化名称的方法
   public String getLocalizedName() {
      // 使用反射调用模块的getLocalizedName方法（如果存在）
      try {
         java.lang.reflect.Method method = this.getClass().getMethod("getLocalizedName");
         if (method != null) {
            return (String) method.invoke(this);
         }
      } catch (Exception e) {
         // 方法不存在或调用失败，使用默认名称
      }
      
      // 默认返回模块名称
      return this.getName();
   }

   public void toggle() {
      this.setEnabled(!this.enabled);
   }

   public SmoothAnimationTimer getAnimation() {
      return this.animation;
   }

   @Override
   public String getName() {
      return this.name;
   }

   public String getPrettyName() {
      return this.prettyName;
   }

   public String getDescription() {
      return this.description;
   }

   public String getSuffix() {
      return this.suffix;
   }

   public Category getCategory() {
      return this.category;
   }

   public boolean isEnabled() {
      return this.enabled;
   }

   public int getMinPermission() {
      return this.minPermission;
   }

   public int getKey() {
      return this.key;
   }

   public Module() {
   }

   public void setMinPermission(int minPermission) {
      this.minPermission = minPermission;
   }

   public void setKey(int key) {
      this.key = key;
   }
}
