package com.heypixel.heypixelmod.obsoverlay.modules;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventKey;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMouseClick;
import com.heypixel.heypixelmod.obsoverlay.exceptions.NoSuchModuleException;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.combat.*;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.misc.*;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.move.LongJump;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.move.AutoMLG;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.move.Blink;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.move.FastWeb;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.move.NoJumpDelay;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.move.NoSlow;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.move.SafeWalk;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.move.Scaffold;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.move.Sprint;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.move.Stuck;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.render.*;
import com.heypixel.heypixelmod.obsoverlay.ui.notification.Notification;
import com.heypixel.heypixelmod.obsoverlay.ui.notification.NotificationLevel;
import com.heypixel.heypixelmod.obsoverlay.utils.ChatUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.HotSwapManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.heypixel.heypixelmod.obsoverlay.ui.LanguageManager;

public class ModuleManager {
   private static final Logger log = LogManager.getLogger(ModuleManager.class);
   private final List<Module> modules = new ArrayList<>();
   private final Map<Class<? extends Module>, Module> classMap = new HashMap<>();
   private final Map<String, Module> nameMap = new HashMap<>();
   private int hotReloadKey = -1; // 默认不设置热重载按键

   public ModuleManager() {
      try {
         this.initModules();
         this.modules.sort((o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()));
      } catch (Exception var2) {
         log.error("Failed to initialize modules", var2);
         throw new RuntimeException(var2);
      }

      Naven.getInstance().getEventManager().register(this);
   }

   private void initModules() {
      this.registerModule(
              new Aura(),
              new HUD(),
              new Velocity(),
              new NameTags(),
              new ChestStealer(),
              new ChestStealerSkiptick(),
              new InventoryCleaner(),
              new Scaffold(),
              new AntiBots(),
              new Sprint(),
              new ChestESP(),
              new ClickGUIModule(),
              new Teams(),
              new Glow(),
              new ItemTracker(),
              new AutoMLG(),
              new ClientFriend(),
              new NoJumpDelay(),
              new FastPlace(),
              new AntiFireball(),
              new Stuck(),
              new ScoreboardSpoof(),
              new FakeLag(),
              new AutoTools(),
              new LagRange(),
              new DynamicIsland(),
              new ViewClip(),
              new Disabler(),
              new Projectile(),
              new TimeChanger(),
              new NoItemSwitchAnimation(),
              new FullBright(),
              new NameProtect(),
              new NoHurtCam(),
              new AutoClicker(),
              new AntiBlindness(),
              new AntiNausea(),
              new Scoreboard(),
              new Compass(),
              new Spammer(),
              new KillSay(),
              new Blink(),
              new FastWeb(),
              new PostProcess(),
              new AttackCrystal(),
              new EffectDisplay(),
              new NoRender(),
              new ItemTags(),
              new SafeWalk(),
              new AimAssist(),
              new MotionBlur(),
              new Helper(),
              new NoSlow(),
              new BackTrack(),
              new AutoHeal(),
              new Inventory(),
              new NewHUD(),
              new Animations(),
              new SecAxeDetector(),
              new NewVelocity(),
              new MusicPlayer(),
              new LongJump()
      );
   }

   private void registerModule(Module... modules) {
      for (Module module : modules) {
         this.registerModule(module);
      }
   }

   private void registerModule(Module module) {
      module.initModule();
      this.modules.add(module);
      this.classMap.put((Class<? extends Module>)module.getClass(), module);
      this.nameMap.put(module.getName().toLowerCase(), module);
   }

   public List<Module> getModulesByCategory(Category category) {
      List<Module> modules = new ArrayList<>();

      for (Module module : this.modules) {
         if (module.getCategory() == category) {
            modules.add(module);
         }
      }

      return modules;
   }

   public Module getModule(Class<? extends Module> clazz) {
      Module module = this.classMap.get(clazz);
      if (module == null) {
         throw new NoSuchModuleException();
      } else {
         return module;
      }
   }

   public Module getModule(String name) {
      Module module = this.nameMap.get(name.toLowerCase());
      if (module == null) {
         throw new NoSuchModuleException();
      } else {
         return module;
      }
   }

   public void setHotReloadKey(int key) {
      this.hotReloadKey = key;
   }

   public int getHotReloadKey() {
      return this.hotReloadKey;
   }

   @EventTarget
   public void onKey(EventKey event) {
      if (event.isState() && Minecraft.getInstance().screen == null) {
         // 检查是否触发了热重载按键
         if (hotReloadKey != -1 && event.getKey() == hotReloadKey) {
            // 尝试初始化热重载系统
            HotSwapManager.getInstance().initialize();
            
            // 触发热重载
            if (HotSwapManager.getInstance().isHotSwapAvailable()) {
                HotSwapManager.getInstance().reloadAllClasses();
                ChatUtils.addChatMessage("已触发热重载，正在重新加载所有类...");
            } else {
                ChatUtils.addChatMessage("热重载功能不可用，请检查配置。");
                ChatUtils.addChatMessage("请确保已正确配置DCEVM和HotswapAgent。");
            }
            return;
         }
         
         List<Module> triggeredModules = new ArrayList<>();
         for (Module module : this.modules) {
            if (module.getKey() == event.getKey()) {
               module.toggle();
               triggeredModules.add(module);
            }
         }

         // 处理模块切换通知
         handleModuleToggleNotifications(triggeredModules);
      }
   }

   @EventTarget
   public void onKey(EventMouseClick event) {
      if (!event.isState() && (event.getKey() == 3 || event.getKey() == 4)) {
         List<Module> triggeredModules = new ArrayList<>();
         for (Module module : this.modules) {
            if (module.getKey() == -event.getKey()) {
               module.toggle();
               triggeredModules.add(module);
            }
         }

         // 处理模块切换通知
         handleModuleToggleNotifications(triggeredModules);
      }
   }

   private void handleModuleToggleNotifications(List<Module> triggeredModules) {
      if (triggeredModules.isEmpty()) {
         return;
      }

      // 检查NewHUD是否启用
      Module newHudModule = this.getModule(NewHUD.class);
      boolean isNewHudEnabled = newHudModule != null && newHudModule.isEnabled();

      if (isNewHudEnabled) {
         // 当NewHUD启用时，将所有触发模块的信息发送给NewHUD处理
         if (triggeredModules.size() == 1) {
            // 单个模块
            Module module = triggeredModules.get(0);
            String message = module.getLocalizedName() + " 已" + (module.isEnabled() ? "开启" : "关闭") + "！";
            ((NewHUD)newHudModule).addModuleToggleNotification(message);
         } else {
            // 多个模块 - 构建组合消息
            StringBuilder message = new StringBuilder();
            for (int i = 0; i < triggeredModules.size(); i++) {
               if (i > 0) {
                  message.append(", ");
               }
               Module module = triggeredModules.get(i);
               message.append(module.getLocalizedName())
                       .append(" 已")
                       .append(module.isEnabled() ? "开启" : "关闭")
                       .append("！");
            }
            ((NewHUD)newHudModule).addModuleToggleNotification(message.toString());
         }
      } else {
         // 显示所有触发模块的状态提示
         showModuleToggleNotification(triggeredModules);
      }
   }

   private void showModuleToggleNotification(List<Module> modules) {
      if (modules.size() == 1) {
         // 单个模块提示
         Module module = modules.get(0);
         String notificationMessage = module.getLocalizedName() + " has been " + (module.isEnabled() ? "enabled" : "disabled");
         if (!LanguageManager.getInstance().isChinese()) {
             notificationMessage = module.getLocalizedName() + " has been " + (module.isEnabled() ? "enabled" : "disabled");
         } else {
             notificationMessage = module.getLocalizedName() + " 已" + (module.isEnabled() ? "开启" : "关闭");
         }
         Notification notification = new Notification(
                 NotificationLevel.INFO,
                 notificationMessage,
                 2000L
         );
         Naven.getInstance().getNotificationManager().addNotification(notification);
      } else {
         // 多个模块组合提示
         StringBuilder message = new StringBuilder();
         for (int i = 0; i < modules.size(); i++) {
            if (i > 0) {
               message.append(", ");
            }
            Module module = modules.get(i);
            if (!LanguageManager.getInstance().isChinese()) {
                message.append(module.getLocalizedName())
                        .append(": ")
                        .append(module.isEnabled() ? "ON" : "OFF");
            } else {
                message.append(module.getLocalizedName())
                        .append(" 已")
                        .append(module.isEnabled() ? "开启" : "关闭");
            }
         }

         Notification notification = new Notification(
                 NotificationLevel.INFO,
                 message.toString(),
                 3000L
         );
         Naven.getInstance().getNotificationManager().addNotification(notification);
      }
   }

   private void showMultiModuleNotification(List<Module> modules) {
      // 构建包含所有模块状态的提示信息
      StringBuilder message = new StringBuilder();
      for (int i = 0; i < modules.size(); i++) {
         if (i > 0) {
            message.append(", ");
         }
         Module module = modules.get(i);
         message.append(module.getName())
                 .append(": ")
                 .append(module.isEnabled() ? "ON" : "OFF");
      }

      // 发送通知事件，让NewHUD或其他系统处理显示
      // 或者直接调用NewHUD的显示方法
   }

   public List<Module> getModules() {
      return this.modules;
   }
}
