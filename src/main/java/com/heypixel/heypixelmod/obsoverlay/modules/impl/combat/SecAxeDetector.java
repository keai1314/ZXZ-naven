package com.heypixel.heypixelmod.obsoverlay.modules.impl.combat;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMotion;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRender2D;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.utils.RenderUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.Fonts;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.text.CustomTextRenderer;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.entity.player.Player;
import net.minecraft.client.multiplayer.PlayerInfo;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import com.heypixel.heypixelmod.obsoverlay.ui.LanguageManager;

@ModuleInfo(
    name = "秒人斧检测",
    description = "检测持有秒人斧的玩家",
    category = Category.COMBAT
)
public class SecAxeDetector extends Module {
    // 设置选项
    private final BooleanValue enableNotification = ValueBuilder.create(this, "启用通知")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();
    
    public final BooleanValue testMode = ValueBuilder.create(this, "测试")
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();
    
    private final FloatValue notificationDuration = ValueBuilder.create(this, "通知持续时间")
            .setDefaultFloatValue(3000.0F)
            .setMinFloatValue(500.0F)
            .setMaxFloatValue(10000.0F)
            .setFloatStep(100.0F)
            .build()
            .getFloatValue();
            
    private final FloatValue notificationSmoothness = ValueBuilder.create(this, "圆滑度")
            .setDefaultFloatValue(0.0F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(15.0F)
            .setFloatStep(0.5F)
            .build()
            .getFloatValue();
            
    private final BooleanValue showBackground = ValueBuilder.create(this, "显示背景")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();
            
    private final FloatValue backgroundAlpha = ValueBuilder.create(this, "背景透明度")
            .setDefaultFloatValue(0.8F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(1.0F)
            .setFloatStep(0.05F)
            .build()
            .getFloatValue();

    // 用于存储检测到的玩家
    private final List<SecAxeNotification> notifications = new CopyOnWriteArrayList<>();
    
    // 存储当前持有秒人斧的玩家UUID，用于检测切换行为
    private final Set<UUID> currentSecAxePlayers = new HashSet<>();
    // 存储上一次持有秒人斧的玩家UUID
    private final Set<UUID> previousSecAxePlayers = new HashSet<>();

    @EventTarget
    public void onMotion(EventMotion event) {
        if (mc.level == null || mc.player == null) return;
        
        // 清理过期的通知
        long currentTime = System.currentTimeMillis();
        notifications.removeIf(notification -> currentTime - notification.getCreateTime() > (long)notificationDuration.getCurrentValue());
        
        // 更新当前持有秒人斧的玩家列表
        updateSecAxePlayers();
    }
    
    /**
     * 更新持有秒人斧的玩家列表
     */
    private void updateSecAxePlayers() {
        // 保存当前列表作为上一次的列表
        previousSecAxePlayers.clear();
        previousSecAxePlayers.addAll(currentSecAxePlayers);
        
        // 清空当前列表
        currentSecAxePlayers.clear();
        
        // 检查所有玩家
        for (Player player : mc.level.players()) {
            // 在测试模式下，也检查自己
            if (!testMode.getCurrentValue() && player == mc.player) continue;
            
            // 检查玩家是否持有秒人斧
            ItemStack heldItem = player.getMainHandItem();
            if (isSecAxe(heldItem)) {
                UUID playerUUID = player.getUUID();
                currentSecAxePlayers.add(playerUUID);
                
                // 如果该玩家之前没有持有秒人斧，则添加通知
                if (!previousSecAxePlayers.contains(playerUUID)) {
                    String playerName = player.getName().getString();
                    addNotification("玩家 " + playerName + " 有秒人斧！");
                }
            }
        }
    }
    
    @EventTarget
    public void onRender(EventRender2D event) {
        if (!enableNotification.getCurrentValue() || notifications.isEmpty()) return;
        
        Minecraft mc = Minecraft.getInstance();
        CustomTextRenderer font = Fonts.opensans;
        
        // 获取屏幕中心位置
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        
        // 渲染通知
        for (SecAxeNotification notification : notifications) {
            long lifeTime = System.currentTimeMillis() - notification.getCreateTime();
            float alphaFactor = Math.min(1.0f, lifeTime / 500.0f); // 淡入效果
            alphaFactor = Math.min(alphaFactor, (notificationDuration.getCurrentValue() - lifeTime) / 500.0f); // 淡出效果
            
            String message = notification.getMessage();
            float textWidth = font.getWidth(message, 0.6);
            float x = (screenWidth - textWidth) / 2.0f;
            float y = screenHeight / 4.0f; // 屏幕上方1/4位置
            
            // 背景框尺寸
            float padding = 6.0F;
            float width = textWidth + padding * 2;
            float height = (float)font.getHeight(true, 0.6) + padding * 2;
            float bgX = x - padding;
            float bgY = y - padding;
            
            // 绘制背景
            if (showBackground.getCurrentValue()) {
                float bgAlpha = backgroundAlpha.getCurrentValue() * alphaFactor;
                int bgColor = new Color(0, 0, 0, (int) (255 * bgAlpha)).getRGB();
                float roundnessValue = notificationSmoothness.getCurrentValue();
                if (roundnessValue > 0) {
                    RenderUtils.drawRoundedRect(event.getStack(), bgX, bgY, width, height, roundnessValue, bgColor);
                } else {
                    RenderUtils.fillBound(event.getStack(), bgX, bgY, width, height, bgColor);
                }
            }
            
            // 绘制文本
            int textColor = new Color(255, 255, 255, (int) (255 * alphaFactor)).getRGB();
            font.render(event.getStack(), message, x, y, new Color(textColor), true, 0.6);
        }
    }
    
    /**
     * 检查物品是否为秒人斧
     * @param itemStack 物品堆
     * @return 是否为秒人斧
     */
    public boolean isSecAxe(ItemStack itemStack) {
        if (itemStack.isEmpty()) {
            return false;
        }
        
        // 检查是否为金斧
        if (itemStack.getItem() != Items.GOLDEN_AXE) {
            return false;
        }
        
        // 如果启用测试模式，任何金斧都会触发
        if (testMode.getCurrentValue()) {
            return true;
        }
        
        // 检查是否有锋利附魔
        int sharpnessLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SHARPNESS, itemStack);
        
        // 秒人斧通常有极高的锋利附魔（如999级）
        return sharpnessLevel >= 100;
    }
    
    /**
     * 添加通知
     * @param message 通知消息
     */
    private void addNotification(String message) {
        notifications.add(new SecAxeNotification(message));
    }
    
    /**
     * 通知类
     */
    public static class SecAxeNotification {
        private final String message;
        private final long createTime;
        
        public SecAxeNotification(String message) {
            this.message = message;
            this.createTime = System.currentTimeMillis();
        }
        
        public String getMessage() {
            return message;
        }
        
        public long getCreateTime() {
            return createTime;
        }
    }
    
    @Override
    public void onDisable() {
        // 清理数据
        notifications.clear();
        currentSecAxePlayers.clear();
        previousSecAxePlayers.clear();
    }
    
    public String getLocalizedName() {
        return LanguageManager.getInstance().getLocalizedString("秒人斧检测", "SecAxe Detector");
    }

    public String getLocalizedDescription() {
        return LanguageManager.getInstance().getLocalizedString("检测持有秒人斧的玩家", "Detect players with sec axe");
    }
}