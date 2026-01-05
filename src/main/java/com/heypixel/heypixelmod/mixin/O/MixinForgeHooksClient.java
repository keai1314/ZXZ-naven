package com.heypixel.heypixelmod.mixin.O;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.misc.NoItemSwitchAnimation;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(net.minecraftforge.client.ForgeHooksClient.class)
public class MixinForgeHooksClient {
    
    @Inject(
        method = {"shouldCauseReequipAnimation"},
        at = @At("HEAD"),
        cancellable = true,
        remap = false
    )
    private static void hookShouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, int slot, CallbackInfoReturnable<Boolean> cir) {
        // 获取NoItemSwitchAnimation模块实例
        NoItemSwitchAnimation module = (NoItemSwitchAnimation) Naven.getInstance().getModuleManager().getModule(NoItemSwitchAnimation.class);
        
        // 如果模块启用，取消物品切换动画
        if (module != null && module.isEnabled()) {
            // 返回false表示不应该触发物品切换动画
            cir.setReturnValue(false);
        }
    }
}