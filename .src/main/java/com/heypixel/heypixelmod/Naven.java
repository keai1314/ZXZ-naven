package com.heypixel.heypixelmod;

import com.heypixel.heypixelmod.commands.CommandManager;
import com.heypixel.heypixelmod.events.api.EventManager;
import com.heypixel.heypixelmod.events.api.EventTarget;
import com.heypixel.heypixelmod.events.api.types.EventType;
import com.heypixel.heypixelmod.events.impl.EventRunTicks;
import com.heypixel.heypixelmod.events.impl.EventShutdown;
import com.heypixel.heypixelmod.files.FileManager;
import com.heypixel.heypixelmod.modules.ModuleManager;
import com.heypixel.heypixelmod.modules.impl.render.ClickGUIModule;
import com.heypixel.heypixelmod.ui.HUDEditor;
import com.heypixel.heypixelmod.ui.notification.NotificationManager;
import com.heypixel.heypixelmod.utils.*;
import com.heypixel.heypixelmod.utils.renderer.Fonts;
import com.heypixel.heypixelmod.utils.renderer.PostProcessRenderer;
import com.heypixel.heypixelmod.utils.renderer.Shaders;
import com.heypixel.heypixelmod.utils.rotation.RotationManager;
import com.heypixel.heypixelmod.values.HasValueManager;
import com.heypixel.heypixelmod.values.ValueManager;
import com.heypixel.heypixelmod.utils.renderer.text.CustomTextRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.awt.*;
import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Naven {
   public static final String CLIENT_NAME = "Naven-XD";
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

         if (Fonts.icons == null) {
             System.err.println("Icons font failed to load, attempting to reload");
             Fonts.icons = new CustomTextRenderer("PublicSans-Bold", 32, 59648, 59652, 512);
         }
      } catch (IOException var2) {
         System.err.println("Failed to load fonts due to IOException");
         var2.printStackTrace();

         ensureFontsLoaded();
      } catch (FontFormatException var3) {
         System.err.println("Failed to load fonts due to FontFormatException");
         var3.printStackTrace();

         ensureFontsLoaded();
      } catch (Exception var4) {
         System.err.println("Failed to load fonts due to unexpected error");
         var4.printStackTrace();

         ensureFontsLoaded();
      }

      if (Fonts.opensans == null) {
          System.err.println("Warning: opensans font is null after initialization");
      }

      if (Fonts.harmony == null) {
          System.err.println("Warning: harmony font is null after initialization");
      }

      if (Fonts.icons == null) {
          System.err.println("Warning: icons font is null after initialization");
      }
      this.eventWrapper = new EventWrapper();
      this.valueManager = new ValueManager();
      this.hasValueManager = new HasValueManager();
      this.moduleManager = new ModuleManager();
      this.rotationManager = new RotationManager();
      this.commandManager = new CommandManager();
      this.fileManager = new FileManager();
      this.notificationManager = new NotificationManager();

      HUDEditor.getInstance();
      this.fileManager.load();

      LoadFontOnStart.loadUserSelectedFont();

      this.moduleManager.getModule(ClickGUIModule.class).setEnabled(false);
      this.eventManager.register(getInstance());
      this.eventManager.register(this.eventWrapper);
      this.eventManager.register(new RotationManager());
      this.eventManager.register(new NetworkUtils());
      this.eventManager.register(new ServerUtils());
      this.eventManager.register(new EntityWatcher());
      this.eventManager.register(this.notificationManager);
      MinecraftForge.EVENT_BUS.register(this.eventWrapper);
      MinecraftForge.EVENT_BUS.register(this);
   }

   public static void modRegister() {
      try {
         new Naven();
      } catch (Exception var1) {
         System.err.println("Failed to load client");
         var1.printStackTrace(System.err);
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

    private void ensureFontsLoaded() {
        try {
            if (Fonts.opensans == null) {
                Fonts.opensans = new CustomTextRenderer("opensans", 32, 0, 255, 512);
            }

            if (Fonts.harmony == null) {
                Fonts.harmony = new CustomTextRenderer("harmony", 32, 0, 65535, 16384);
            }

            if (Fonts.icons == null) {
                Fonts.icons = new CustomTextRenderer("PublicSans-Bold", 32, 59648, 59652, 512);
            }
        } catch (Exception e) {
            System.err.println("Failed to load fallback fonts");
            e.printStackTrace();
        }
    }
}