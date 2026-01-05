package com.heypixel.heypixelmod.modules;

import com.heypixel.heypixelmod.Naven;
import com.heypixel.heypixelmod.events.api.EventTarget;
import com.heypixel.heypixelmod.events.impl.EventKey;
import com.heypixel.heypixelmod.events.impl.EventMouseClick;
import com.heypixel.heypixelmod.exceptions.NoSuchModuleException;
import com.heypixel.heypixelmod.modules.impl.combat.*;
import com.heypixel.heypixelmod.modules.impl.crystalpvp.*;
import com.heypixel.heypixelmod.modules.impl.misc.*;
import com.heypixel.heypixelmod.modules.impl.move.*;
import com.heypixel.heypixelmod.modules.impl.render.*;
import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class ModuleManager {
   private static final Logger log = LogManager.getLogger(ModuleManager.class);
   private final List<Module> modules = new ArrayList<>();
   private final Map<Class<? extends Module>, Module> classMap = new HashMap<>();
   private final Map<String, Module> nameMap = new HashMap<>();
   private final Map<Integer, Boolean> pressedState = new HashMap<>();
   private final Set<Integer> pressedKeys = new HashSet<>();

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
         new AutoBan(),
         new NameTags(),
         new ChestStealer(),
         new HackerCheck(),
         new InventoryCleaner(),
         new Scaffold(),
         new AntiBots(),
         new Sprint(),
         new FlagCheck(),
         new ChestESP(),
         new ClickGUIModule(),
         new Teams(),
         new Glow(),
         new Velocity(),
         new AutoPlay(),
         new AntiVoid(),
         new ItemTracker(),
         new AutoMLG(),
         new ClientFriend(),
         new NoJumpDelay(),
         new LagRange(),
         new FastPlace(),
         new AntiFireball(),
         new Stuck(),
         new AntiWeb(),
         new ScoreboardSpoof(),
         new AutoTools(),
         new Camera(),
         new AnchorExploder(),
         new AutoShieldBreaker(),
         new TriggerBot(),
         new AutoTotem(),
         new AutoHitCrystal(),
         new AutoHeal(),
         new TargetStrafe(),
         new AnchorMacro(),
         new Disabler(),
         new Projectile(),
         new TimeChanger(),
         new FullBright(),
         new NameProtect(),
         new NoHurtCam(),
         new AutoClicker(),
         new AntiBlindness(),
         new AntiNausea(),
         new PearlPrediction(),
         new Scoreboard(),
         new Compass(),
         new Spammer(),
         new KillSay(),
         new Blink(),
         new FastWeb(),
         new PostProcess(),
         new CrystalAura(),
         new ItemsCounter(),
         new ArmorRender(),
         new EffectDisplay(),
         new NoRender(),
         new Weather(),
         new BackTrack(),
         new AutoThrowable(),
         new SuperKB(),
         new ItemTags(),
         new ResetOpenGL(),
         new SafeWalk(),
         new AimAssist(),
         new MotionBlur(),
         new Animations(),
         new AttackEffect(),
         new KillEffect(),
         new Helper(),
         new Speed(),
         new InventoryMove(),
         new FontSelect(),
         new LagBase(),
         new NoSlow(),
         new NoFall(),
         new LongJump(),
         new GhostHand(),
         new PreferWeapon(),
         new TNTWarning(),
         new BowAimbot(),
         new CrystalOptimizer(),
         new Island(),
         new AutoCrystal(),
         new MidPearl(),
         new SessionInfo()

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

   @EventTarget
   public void onKey(EventKey e) {
      if (e.isState() && Minecraft.getInstance().screen == null) {
         if (pressedKeys.add(e.getKey())) {
            for (Module module : this.modules) {
               if (module.getKey() == e.getKey() && module.isToggleableWithKey()) {
                  module.toggle();
               }
            }
         }
      } else {
         pressedKeys.remove(e.getKey());
      }
   }

   @EventTarget
   public void onKey(EventMouseClick e) {
      if (!e.isState() && (e.getKey() == 3 || e.getKey() == 4)) {
         for (Module module : this.modules) {
            if (module.getKey() == -e.getKey() && module.isToggleableWithKey()) {
               module.toggle();
            }
         }
      }
   }


   public List<Module> getModules() {
      return this.modules;
   }
}
