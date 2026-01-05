package com.heypixel.heypixelmod.obsoverlay;

import com.heypixel.heypixelmod.obsoverlay.commands.CommandManager;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventManager;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRunTicks;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventShutdown;
import com.heypixel.heypixelmod.obsoverlay.files.FileManager;
import com.heypixel.heypixelmod.obsoverlay.managers.LagManager;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleManager;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.render.ClickGUIModule;
import com.heypixel.heypixelmod.obsoverlay.ui.notification.NotificationManager;
import com.heypixel.heypixelmod.obsoverlay.utils.EntityWatcher;
import com.heypixel.heypixelmod.obsoverlay.utils.EventWrapper;
import com.heypixel.heypixelmod.obsoverlay.utils.LogUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.NetworkUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.ProcessNameModifier;
import com.heypixel.heypixelmod.obsoverlay.utils.ServerUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.TickTimeHelper;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.Fonts;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.PostProcessRenderer;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.Shaders;
import com.heypixel.heypixelmod.obsoverlay.utils.rotation.RotationManager;
import com.heypixel.heypixelmod.obsoverlay.values.HasValueManager;
import com.heypixel.heypixelmod.obsoverlay.values.ValueManager;
import java.awt.FontFormatException;
import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraftforge.common.MinecraftForge;

public class Naven {
   public static final String CLIENT_NAME = "Naven-Modern";
   public static final String CLIENT_DISPLAY_NAME = "Naven";
   private static Naven instance;
   private final EventManager eventManager;
   private final EventWrapper eventWrapper;
   private final ValueManager valueManager;
   private final HasValueManager hasValueManager;
   private final RotationManager rotationManager;
   public final ModuleManager moduleManager;
   private final CommandManager commandManager;
   private final FileManager fileManager;
   private final NotificationManager notificationManager;
   private final LagManager lagManager;
   public static float TICK_TIMER = 1.0F;
   public static Queue<Runnable> skipTasks = new ConcurrentLinkedQueue<>();

   private Naven() {
      System.out.println("Naven Init");
      instance = this;
      this.eventManager = new EventManager();
      Shaders.init();
      PostProcessRenderer.init();

      try {
         Fonts.loadFonts();
      } catch (IOException var2) {
         throw new RuntimeException(var2);
      } catch (FontFormatException var3) {
         throw new RuntimeException(var3);
      }

      // 在初始化早期调用进程名称修改器
      ProcessNameModifier.modifyProcessName();
      System.out.println("进程名称修改完成");

      this.eventWrapper = new EventWrapper();
      this.valueManager = new ValueManager();
      this.hasValueManager = new HasValueManager();
      this.moduleManager = new ModuleManager();
      this.rotationManager = new RotationManager();
      this.commandManager = new CommandManager();
      this.fileManager = new FileManager();
      this.notificationManager = new NotificationManager();
      this.lagManager = new LagManager();
      this.fileManager.load();
      this.moduleManager.getModule(ClickGUIModule.class).setEnabled(false);
      this.eventManager.register(getInstance());
      this.eventManager.register(this.eventWrapper);
      this.eventManager.register(new RotationManager());
      this.eventManager.register(new NetworkUtils());
      this.eventManager.register(new ServerUtils());
      this.eventManager.register(new EntityWatcher());
      this.eventManager.register(this.lagManager);
      MinecraftForge.EVENT_BUS.register(this.eventWrapper);
   }

   public static void modRegister() {
      try {
         new Naven();
      } catch (Exception var1) {
         System.err.println("Failed to load client");
         var1.printStackTrace(System.err);
      }
   }

   /**
     * Agent入口点，用于热注入
     */
    public static void agentmain(String agentArgs, java.lang.instrument.Instrumentation inst) {
        System.out.println("Naven mod agent loaded!");
        System.out.println("Agent args: " + agentArgs);
        
        try {
            // 初始化模组
            Naven.getInstance().init();
            System.out.println("Naven mod initialized successfully through agent!");
        } catch (Exception e) {
            System.err.println("Error initializing Naven mod through agent: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
   @EventTarget
   public void onShutdown(EventShutdown e) {
      this.fileManager.save();
      LogUtils.close();
   }

   @EventTarget(0)
   public void onEarlyTick(EventRunTicks e) {
      if (e.getType() == EventType.PRE) {
         TickTimeHelper.update();
      }
   }

   public static Naven getInstance() {
      return instance;
   }

   public EventManager getEventManager() {
      return this.eventManager;
   }

   public EventWrapper getEventWrapper() {
      return this.eventWrapper;
   }

   public ValueManager getValueManager() {
      return this.valueManager;
   }

   public HasValueManager getHasValueManager() {
      return this.hasValueManager;
   }

   public RotationManager getRotationManager() {
      return this.rotationManager;
   }

   public ModuleManager getModuleManager() {
      return this.moduleManager;
   }

   public CommandManager getCommandManager() {
      return this.commandManager;
   }

   public FileManager getFileManager() {
      return this.fileManager;
   }

   public NotificationManager getNotificationManager() {
      return this.notificationManager;
   }
   
   public LagManager getLagManager() {
      return this.lagManager;
   }
   
   /**
    * 初始化模组功能
    */
   public void init() {
       System.out.println("Initializing Naven mod through agent...");
       // 这里可以添加任何需要的初始化逻辑
       // 目前Naven构造函数中已经完成了大部分初始化工作
       System.out.println("Naven mod initialization completed!");
   }
}