package com.heypixel.heypixelmod.mixin.O;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.misc.NoItemSwitchAnimation;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ItemInHandRenderer.class)
public class MixinItemInHandRenderer2 {
    
    /**
     * 直接修改equipProgress的值，使其始终为1.0，从而跳过物品切换动画
     */
    @Redirect(
        method = {"renderHand"},
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;getAttackAnim(F)F"
        )
    )
    private float hookGetAttackAnim(LocalPlayer player, float partialTicks) {
        // 获取NoItemSwitchAnimation模块实例
        NoItemSwitchAnimation module = (NoItemSwitchAnimation) Naven.getInstance().getModuleManager().getModule(NoItemSwitchAnimation.class);
        
        // 如果模块启用，返回1.0以跳过动画
        if (module != null && module.isEnabled()) {
            return 1.0F;
        }
        
        // 否则调用原始方法
        return player.getAttackAnim(partialTicks);
    }
    
    /**
     * 修改主手物品渲染时的equipProgress
     */
    @Redirect(
        method = {"renderItem"},
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;getAttackAnim(F)F"
        )
    )
    private float hookRenderItemGetAttackAnim(LocalPlayer player, float partialTicks) {
        // 获取NoItemSwitchAnimation模块实例
        NoItemSwitchAnimation module = (NoItemSwitchAnimation) Naven.getInstance().getModuleManager().getModule(NoItemSwitchAnimation.class);
        
        // 如果模块启用，返回1.0以跳过动画
        if (module != null && module.isEnabled()) {
            return 1.0F;
        }
        
        // 否则调用原始方法
        return player.getAttackAnim(partialTicks);
    }
}