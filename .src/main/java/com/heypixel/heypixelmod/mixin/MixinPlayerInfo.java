package com.heypixel.heypixelmod.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.multiplayer.PlayerInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerInfo.class)
public class MixinPlayerInfo {
    @Inject(method = "<init>", at = @At("TAIL"))
    private void inject_init_tail(GameProfile profile, boolean rejectAll, CallbackInfo ci) {
    }
}
