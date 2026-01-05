package com.heypixel.heypixelmod.obsoverlay.modules.impl.misc;

import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraft.world.item.ItemStack;
import com.heypixel.heypixelmod.obsoverlay.ui.LanguageManager;

@ModuleInfo(
        name = "无物品切换动画",
        description = "取消物品切换时的动画效果，实现瞬间切换",
        category = Category.MISC
)
public class NoItemSwitchAnimation extends Module {
    
    @Override
    public void onEnable() {
        super.onEnable();
        // 启用时不需要注册事件，因为我们将使用Mixin来修改动画行为
    }
    
    @Override
    public void onDisable() {
        super.onDisable();
        // 禁用时不需要注销事件
    }
    
    /**
     * 用于判断是否应该跳过物品切换动画
     * @return 如果模块启用则返回false，否则返回true
     */
    public static boolean shouldSkipReequipAnimation() {
        // 这里将通过Mixin调用，判断模块是否启用
        return false;
    }
    
    public String getLocalizedName() {
        return LanguageManager.getInstance().getLocalizedString("无物品切换动画", "No Item Switch Animation");
    }
    
    public String getLocalizedDescription() {
        return LanguageManager.getInstance().getLocalizedString("取消物品切换时的动画效果，实现瞬间切换", "Disable item switching animation for instant switching");
    }
}