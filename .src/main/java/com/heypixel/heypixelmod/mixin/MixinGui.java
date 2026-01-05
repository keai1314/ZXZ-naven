package com.heypixel.heypixelmod.mixin;

import com.heypixel.heypixelmod.Naven;
import com.heypixel.heypixelmod.events.api.types.EventType;
import com.heypixel.heypixelmod.events.impl.EventRenderScoreboard;
import com.heypixel.heypixelmod.events.impl.EventSetTitle;
import com.heypixel.heypixelmod.modules.impl.render.NoRender;
import com.heypixel.heypixelmod.modules.impl.render.Scoreboard;
import com.heypixel.heypixelmod.ui.BetterHotBar;
import javax.annotation.Nullable;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Team;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
   value = {Gui.class},
   priority = 100
)
public class MixinGui {
   @Shadow
   @Nullable
   protected Component title;
   @Shadow
   protected int titleTime;
   @Shadow
   protected int titleFadeInTime;
   @Shadow
   protected int titleStayTime;
   @Shadow
   protected int titleFadeOutTime;
   @Shadow
   @Nullable
   protected Component subtitle;

@Inject(
   method = {"displayScoreboardSidebar"},
   at = {@At("HEAD")}
)
public void hookScoreboardHead(GuiGraphics pPoseStack, Objective pObjective, CallbackInfo ci) {
   pPoseStack.pose().pushPose();
   Naven naven = Naven.getInstance();
   if (naven != null && naven.getModuleManager() != null) {
      Scoreboard module = (Scoreboard)naven.getModuleManager().getModule(Scoreboard.class);
      if (module != null && module.isEnabled()) {
         module.clearScoreboardData();
         float xOffset = module.getXOffset();
         float yOffset = module.getYOffset();
         pPoseStack.pose().translate(xOffset, yOffset, 0.0F);
      }
   }
}

@Inject(
   method = {"displayScoreboardSidebar"},
   at = {@At("RETURN")}
)
public void hookScoreboardReturn(GuiGraphics pPoseStack, Objective pObjective, CallbackInfo ci) {
   pPoseStack.pose().popPose();
}

   @Redirect(
      method = {"displayScoreboardSidebar"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)I"
      )
   )
   public int hookRenderScore(GuiGraphics instance, Font p_283343_, String p_281896_, int p_283569_, int p_283418_, int p_281560_, boolean p_282130_) {
      Naven naven = Naven.getInstance();
      if (naven != null && naven.getModuleManager() != null) {
         Scoreboard module = (Scoreboard)naven.getModuleManager().getModule(Scoreboard.class);
         if (module != null && module.isEnabled() && module.hideScore.getCurrentValue()) {
            return 0;
         }
      }                                 
      return instance.drawString(p_283343_, p_281896_, p_283569_, p_283418_, p_281560_);
   }

   @Redirect(
      method = {"displayScoreboardSidebar"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/scores/PlayerTeam;formatNameForTeam(Lnet/minecraft/world/scores/Team;Lnet/minecraft/network/chat/Component;)Lnet/minecraft/network/chat/MutableComponent;"
      )
   )
   public MutableComponent hookScoreboardName(Team pPlayerTeam, Component pPlayerName) {
      MutableComponent mutableComponent = PlayerTeam.formatNameForTeam(pPlayerTeam, pPlayerName);
      EventRenderScoreboard event = new EventRenderScoreboard(mutableComponent);
      Naven naven = Naven.getInstance();
      if (naven != null && naven.getEventManager() != null) {
         naven.getEventManager().call(event);
      }
      return (MutableComponent)event.getComponent();
   }

   @Redirect(
      method = {"displayScoreboardSidebar"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/scores/Objective;getDisplayName()Lnet/minecraft/network/chat/Component;"
      )
   )
   public Component hookScoreboardTitle(Objective instance) {
      Component component = instance.getDisplayName();
      EventRenderScoreboard event = new EventRenderScoreboard(component);
      Naven naven = Naven.getInstance();
      if (naven != null && naven.getEventManager() != null) {
         naven.getEventManager().call(event);
      }
      return event.getComponent();
   }

   @Inject(
      method = {"setTitle"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void hookTitle(Component pTitle, CallbackInfo ci) {
      EventSetTitle event = new EventSetTitle(EventType.TITLE, pTitle);
      Naven naven = Naven.getInstance();
      if (naven != null && naven.getEventManager() != null) {
         naven.getEventManager().call(event);
         if (!event.isCancelled()) {
            this.title = event.getTitle();
            this.titleTime = this.titleFadeInTime + this.titleStayTime + this.titleFadeOutTime;
            ci.cancel();
         }
      }
   }

   @Inject(
      method = {"setSubtitle"},
      at = {@At("RETURN")},
      cancellable = true
   )
   public void hookSubtitle(Component pSubtitle, CallbackInfo ci) {
      EventSetTitle event = new EventSetTitle(EventType.SUBTITLE, pSubtitle);
      Naven naven = Naven.getInstance();
      if (naven != null && naven.getEventManager() != null) {
         naven.getEventManager().call(event);
         if (!event.isCancelled()) {
            this.subtitle = event.getTitle();
            ci.cancel();
         }
      }
   }

@Inject(
   method = {"renderEffects"},
   at = {@At("HEAD")},
   cancellable = true
)
public void hookRenderEffects(GuiGraphics pPoseStack, CallbackInfo ci) {
   Naven naven = Naven.getInstance();
   if (naven != null && naven.getModuleManager() != null) {
      NoRender noRender = (NoRender)naven.getModuleManager().getModule(NoRender.class);
      if (noRender != null && noRender.isEnabled()) {
         ci.cancel();
      }
   }
}

@Redirect(
   method = {"displayScoreboardSidebar"},
   at = @At(
      value = "INVOKE",
      target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V"
   )
)
public void hookScoreboardFill(GuiGraphics instance, int pX1, int pY1, int pX2, int pY2, int pColor) {
   Naven naven = Naven.getInstance();
   if (naven != null && naven.getModuleManager() != null) {
      Scoreboard module = (Scoreboard)naven.getModuleManager().getModule(Scoreboard.class);
      if (module != null && module.isEnabled()) {
         module.addScoreboardBounds(pX1, pY1, pX2, pY2, pColor);
         module.renderBackgroundIfNeeded(instance.pose());
         if (module.shouldRenderRoundedBackground()) {
            return;
         }
      }
   }
   instance.fill(pX1, pY1, pX2, pY2, pColor);
}

@Inject(
   method = {"renderHotbar"},
   at = {@At("HEAD")},
   cancellable = true
)
public void hookRenderHotbar(float pPartialTick, GuiGraphics pGuiGraphics, CallbackInfo ci) {
   if (BetterHotBar.shouldRenderCustomHotbar()) {
      Naven naven = Naven.getInstance();
      if (naven != null && naven.getModuleManager() != null) {
         com.heypixel.heypixelmod.modules.impl.render.HUD hudModule = (com.heypixel.heypixelmod.modules.impl.render.HUD)naven.getModuleManager().getModule(com.heypixel.heypixelmod.modules.impl.render.HUD.class);
         float animationSpeed = hudModule != null ? hudModule.hotbarAnimationSpeed.getCurrentValue() : 20.0F;
         BetterHotBar.renderCustomHotbar(pGuiGraphics, pPartialTick, animationSpeed);
         ci.cancel();
      }
   }
}
}
