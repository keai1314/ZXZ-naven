package com.heypixel.heypixelmod.mixin;

import com.heypixel.heypixelmod.Naven;
import com.heypixel.heypixelmod.events.impl.EventKey;
import net.minecraft.client.KeyboardHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({KeyboardHandler.class})
public class MixinKeyboardHandler {
   @Inject(
      at = {@At("HEAD")},
      method = {"keyPress"}
   )
   private void onKeyPress(long pWindowPointer, int pKey, int pScanCode, int pAction, int pModifiers, CallbackInfo ci) {
      if (pKey != -1 && Naven.getInstance() != null && Naven.getInstance().getEventManager() != null) {
         Naven.getInstance().getEventManager().call(new EventKey(pKey, pAction != 0));
      }
   }
}
