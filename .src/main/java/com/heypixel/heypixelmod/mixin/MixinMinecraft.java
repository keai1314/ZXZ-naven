package com.heypixel.heypixelmod.mixin;

import com.heypixel.heypixelmod.Naven;
import com.heypixel.heypixelmod.ui.Welcome;
import com.heypixel.heypixelmod.Version;
import com.heypixel.heypixelmod.events.api.types.EventType;
import com.heypixel.heypixelmod.events.impl.EventClick;
import com.heypixel.heypixelmod.events.impl.EventRunTicks;
import com.heypixel.heypixelmod.events.impl.EventShutdown;
import com.heypixel.heypixelmod.modules.impl.render.Glow;
import com.heypixel.heypixelmod.utils.AnimationUtils;
import java.util.ArrayList;
import java.util.List;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.mojang.realmsclient.client.RealmsClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.main.GameConfig;
import net.minecraft.server.packs.resources.ReloadInstance;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.IModFileInfo;
import net.minecraftforge.forgespi.language.IModInfo;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Minecraft.class})
public abstract class MixinMinecraft {

    @Overwrite
    private void setInitialScreen(RealmsClient p_279285_, ReloadInstance p_279164_, GameConfig.QuickPlayData p_279146_) {
        setScreen(new Welcome());
    }

    @Unique
    private String province = null;

    @Unique
    private boolean hasRetried = false;

    @Unique
    private long lastQueryTime = 0;

    @Unique
    private static final long QUERY_INTERVAL = 5000;

    @Inject(
            method = "createTitle",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onCreateTitle(CallbackInfoReturnable<String> cir) {
        long currentTime = System.currentTimeMillis();

        if (province == null || (province.equals("神秘人") && !hasRetried && (currentTime - lastQueryTime) >= QUERY_INTERVAL)) {
            if (province != null && province.equals("神秘人")) {
                hasRetried = true;
            }
            province = getProvinceByIP();
            lastQueryTime = currentTime;
        }

        cir.setReturnValue("Naven-XD | " + Version.getVersion() + " | 哈基米 | " + province);
    }

    private String getProvinceByIP() {
        try {
            String ip = getPublicIP();

            if (ip == null || ip.isEmpty()) {
                return "神秘人";
            }

            String province = getProvinceFromAPI(ip);

            if (province != null && !province.isEmpty() && !province.equals("未知")) {
                return province + "人";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "神秘人";
    }

    private String getPublicIP() throws Exception {
        URL url = new URL("https://httpbin.org/ip");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        connection.setRequestProperty("User-Agent", "Mozilla/5.0");

        int responseCode = connection.getResponseCode();
        if (responseCode != 200) {
            return null;
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            response.append(line);
        }

        reader.close();

        JsonObject jsonObject = JsonParser.parseString(response.toString()).getAsJsonObject();
        return jsonObject.has("origin") ? jsonObject.get("origin").getAsString() : null;
    }

    private String getProvinceFromAPI(String ip) throws Exception {
        String apiUrl = "https://ip9.com.cn/get?ip=" + ip;
        URL url = new URL(apiUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        connection.setRequestProperty("User-Agent", "Mozilla/5.0");

        int responseCode = connection.getResponseCode();
        if (responseCode != 200) {
            return "未知";
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
        StringBuilder response = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            response.append(line);
        }

        reader.close();

        JsonObject jsonObject = JsonParser.parseString(response.toString()).getAsJsonObject();
        if (jsonObject.has("ret") && jsonObject.get("ret").getAsInt() == 200) {
            if (jsonObject.has("data")) {
                JsonObject data = jsonObject.getAsJsonObject("data");
                if (data.has("prov")) {
                    return data.get("prov").getAsString();
                }
            }
        }

        return "未知";
    }

    @Shadow
    public abstract void setScreen(@Nullable Screen p_91153_);

   @Unique
   private int skipTicks;
   @Unique
   private long naven_Modern$lastFrame;

   @Inject(
      method = {"<init>"},
      at = {@At("TAIL")}
   )
   private void onInit(CallbackInfo info) {
      Naven.modRegister();
   }

   @Inject(
      method = {"<init>"},
      at = {@At("RETURN")}
   )
   public void onInit(GameConfig pGameConfig, CallbackInfo ci) {
      System.setProperty("java.awt.headless", "false");
      ModList.get().getMods().removeIf(modInfox -> modInfox.getModId().contains("naven"));
      List<IModFileInfo> fileInfoToRemove = new ArrayList<>();

      for (IModFileInfo fileInfo : ModList.get().getModFiles()) {
         for (IModInfo modInfo : fileInfo.getMods()) {
            if (modInfo.getModId().contains("naven")) {
               fileInfoToRemove.add(fileInfo);
            }
         }
      }

      ModList.get().getModFiles().removeAll(fileInfoToRemove);
   }

   @Inject(
      method = {"close"},
      at = {@At("HEAD")},
      remap = false
   )
   private void shutdown(CallbackInfo ci) {
      if (Naven.getInstance() != null && Naven.getInstance().getEventManager() != null) {
         Naven.getInstance().getEventManager().call(new EventShutdown());
      }
   }

   @Inject(
      method = {"tick"},
      at = {@At("HEAD")}
   )
   private void tickPre(CallbackInfo ci) {
      if (Naven.getInstance() != null && Naven.getInstance().getEventManager() != null) {
         Naven.getInstance().getEventManager().call(new EventRunTicks(EventType.PRE));
      }
   }

   @Inject(
      method = {"tick"},
      at = {@At("TAIL")}
   )
   private void tickPost(CallbackInfo ci) {
      if (Naven.getInstance() != null && Naven.getInstance().getEventManager() != null) {
         Naven.getInstance().getEventManager().call(new EventRunTicks(EventType.POST));
      }
   }

   @Inject(
      method = {"shouldEntityAppearGlowing"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void shouldEntityAppearGlowing(Entity pEntity, CallbackInfoReturnable<Boolean> cir) {
      if (Glow.shouldGlow(pEntity)) {
         cir.setReturnValue(true);
      }
   }

   @Inject(
      method = {"runTick"},
      at = {@At("HEAD")}
   )
   private void runTick(CallbackInfo ci) {
      long currentTime = System.nanoTime() / 1000000L;
      int deltaTime = (int)(currentTime - this.naven_Modern$lastFrame);
      this.naven_Modern$lastFrame = currentTime;
      AnimationUtils.delta = deltaTime;
   }

   @ModifyArg(
      method = {"runTick"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/GameRenderer;render(FJZ)V"
      )
   )
   private float fixSkipTicks(float g) {
      if (this.skipTicks > 0) {
         g = 0.0F;
      }

      return g;
   }

   @Inject(
      method = {"handleKeybinds"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/player/LocalPlayer;isUsingItem()Z",
         ordinal = 0,
         shift = Shift.BEFORE
      )},
      cancellable = true
   )
   private void clickEvent(CallbackInfo ci) {
      if (Naven.getInstance() != null && Naven.getInstance().getEventManager() != null) {
         EventClick event = new EventClick();
         Naven.getInstance().getEventManager().call(event);
         if (event.isCancelled()) {
            ci.cancel();
         }
      }
   }
}