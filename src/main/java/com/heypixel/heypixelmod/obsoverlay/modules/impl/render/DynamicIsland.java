package com.heypixel.heypixelmod.obsoverlay.modules.impl.render;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRender2D;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventShader;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleManager;
import com.heypixel.heypixelmod.obsoverlay.utils.RenderUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.SmoothAnimationTimer;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.Fonts;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.text.CustomTextRenderer;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.ModeValue;
import net.minecraft.client.Minecraft;
import org.joml.Vector4f;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@ModuleInfo(
        name = "DynamicIsland",
        description = "苹果灵动岛样式的通知组件",
        category = Category.RENDER
)
public class DynamicIsland extends Module {
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");
    
    // 灵动岛配置
    private static final float DEFAULT_WIDTH = 200.0F;
    private static final float DEFAULT_HEIGHT = 40.0F;
    private static final float ITEM_HEIGHT = 30.0F;
    private static final float MAX_HEIGHT = 150.0F;
    private static final float CORNER_RADIUS = 20.0F;
    private static final float PADDING = 8.0F;
    private static final float SWITCH_WIDTH = 40.0F;
    private static final float SWITCH_HEIGHT = 16.0F;
    
    // 颜色配置
    private static final int SWITCH_ON_COLOR = new Color(0, 200, 0, 255).getRGB();
    private static final int SWITCH_OFF_COLOR = new Color(200, 0, 0, 255).getRGB();
    
    // 动画配置
    private static final float ANIMATION_SPEED = 0.2F;
    
    // 通知项
    private static class NotificationItem {
        private final Module module;
        private final boolean enabled;
        private final long timestamp;
        private final SmoothAnimationTimer heightAnimation;
        private final SmoothAnimationTimer switchAnimation;
        
        public NotificationItem(Module module, boolean enabled) {
            this.module = module;
            this.enabled = enabled;
            this.timestamp = System.currentTimeMillis();
            this.heightAnimation = new SmoothAnimationTimer(0.0F, ANIMATION_SPEED);
            this.switchAnimation = new SmoothAnimationTimer(enabled ? 100.0F : 0.0F, ANIMATION_SPEED);
        }
        
        public Module getModule() {
            return module;
        }
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
        
        public SmoothAnimationTimer getHeightAnimation() {
            return heightAnimation;
        }
        
        public SmoothAnimationTimer getSwitchAnimation() {
            return switchAnimation;
        }
    }
    
    // Minecraft实例
    private static final Minecraft mc = Minecraft.getInstance();
    
    // 模块实例
    private final ModuleManager moduleManager = Naven.getInstance().getModuleManager();
    
    // 状态变量
    private final List<NotificationItem> notifications = new ArrayList<>();
    private final ConcurrentHashMap<String, NotificationItem> activeNotifications = new ConcurrentHashMap<>();
    private final SmoothAnimationTimer mainHeightAnimation = new SmoothAnimationTimer(DEFAULT_HEIGHT, ANIMATION_SPEED);
    
    // 渲染变量
    private float currentWidth = DEFAULT_WIDTH;
    private float currentHeight = DEFAULT_HEIGHT;
    private float watermarkHeight;
    private List<Vector4f> blurMatrices = new ArrayList<>();
    
    // 配置选项
    public BooleanValue showTime = ValueBuilder.create(this, "显示时间").setDefaultBooleanValue(true).build().getBooleanValue();
    public BooleanValue showModuleName = ValueBuilder.create(this, "显示模块名称").setDefaultBooleanValue(true).build().getBooleanValue();
    public FloatValue animationSpeed = ValueBuilder.create(this, "动画速度")
            .setDefaultFloatValue(0.2F)
            .setMinFloatValue(0.05F)
            .setMaxFloatValue(1.0F)
            .setFloatStep(0.05F)
            .build()
            .getFloatValue();
    public FloatValue islandWidth = ValueBuilder.create(this, "宽度")
            .setDefaultFloatValue(200.0F)
            .setMinFloatValue(100.0F)
            .setMaxFloatValue(400.0F)
            .setFloatStep(10.0F)
            .build()
            .getFloatValue();
    public FloatValue islandHeight = ValueBuilder.create(this, "高度")
            .setDefaultFloatValue(40.0F)
            .setMinFloatValue(30.0F)
            .setMaxFloatValue(100.0F)
            .setFloatStep(5.0F)
            .build()
            .getFloatValue();
    public FloatValue notificationTime = ValueBuilder.create(this, "通知时间")
            .setDefaultFloatValue(10.0F)
            .setMinFloatValue(3.0F)
            .setMaxFloatValue(30.0F)
            .setFloatStep(1.0F)
            .build()
            .getFloatValue();
    
    @EventTarget
    public void onRender(EventRender2D e) {
        CustomTextRenderer font = Fonts.opensans;
        
        // 更新动画
        updateAnimations();
        
        // 清空模糊矩阵
        blurMatrices.clear();
        
        e.getStack().pushPose();
        
        // 渲染灵动岛
        renderDynamicIsland(e, font);
        
        // 渲染通知
        renderNotifications(e, font);
        
        e.getStack().popPose();
    }
    
    @EventTarget
    public void onShader(EventShader e) {
        if (e.getType() == EventType.SHADOW) {
            // 渲染阴影效果
            float centerX = (mc.getWindow().getGuiScaledWidth() - currentWidth) / 2.0F;
            float centerY = 20.0F;
            // 使用与主体相同的圆角半径8.0F
            RenderUtils.drawRoundedRect(e.getStack(), centerX + 2.0F, centerY + 2.0F, currentWidth, currentHeight, 8.0F, Integer.MIN_VALUE);
        }
        
        if (e.getType() == EventType.BLUR) {
            // 渲染模糊区域
            for (Vector4f blurMatrix : blurMatrices) {
                RenderUtils.fillBound(e.getStack(), blurMatrix.x(), blurMatrix.y(), blurMatrix.z(), blurMatrix.w(), 1073741824);
            }
        }
    }
    
    private void updateAnimations() {
        // 更新主高度动画
        mainHeightAnimation.update(true);
        
        // 更新通知项动画
        for (NotificationItem item : notifications) {
            item.getHeightAnimation().update(true);
            item.getSwitchAnimation().update(true);
        }
        
        // 移除过期通知（使用配置的时间，转换为毫秒）
        long currentTime = System.currentTimeMillis();
        long notificationTimeMs = (long) (notificationTime.getCurrentValue() * 1000);
        notifications.removeIf(item -> currentTime - item.getTimestamp() > notificationTimeMs);
        
        // 更新当前高度，使用配置的基础高度
        float baseHeight = islandHeight.getCurrentValue();
        float targetHeight = baseHeight + Math.min(notifications.size() * ITEM_HEIGHT, MAX_HEIGHT - baseHeight);
        mainHeightAnimation.target = targetHeight;
        currentHeight = mainHeightAnimation.value;
    }
    
    private void renderDynamicIsland(EventRender2D e, CustomTextRenderer font) {
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        currentWidth = islandWidth.getCurrentValue();
        float centerX = (screenWidth - currentWidth) / 2.0F;
        float centerY = 20.0F;
        
        // 黑灰色35%透明度的毛玻璃效果背景
        int glassColor = new Color(50, 50, 50, 89).getRGB(); // 35%透明度的黑灰色
        
        // 添加模糊矩阵
        blurMatrices.add(new Vector4f(centerX, centerY, currentWidth, currentHeight));
        
        // 绘制主背景
        RenderUtils.drawRoundedRect(e.getStack(), centerX, centerY, currentWidth, currentHeight, Math.min(CORNER_RADIUS, 8.0F), glassColor);
        
        // 绘制顶部强调色条
        RenderUtils.fill(e.getStack(), centerX, centerY, centerX + currentWidth, centerY + 2.0F, HUD.accentColor);
        
        // 绘制左侧模块名称
        if (showModuleName.getCurrentValue()) {
            String moduleName = "Dynamic Island";
            font.render(e.getStack(), moduleName, centerX + PADDING, centerY + 12.0F, Color.WHITE, true, 0.35);
        }
        
        // 绘制右侧时间
        if (showTime.getCurrentValue()) {
            String time = TIME_FORMAT.format(new Date());
            float timeWidth = font.getWidth(time, 0.35);
            font.render(e.getStack(), time, centerX + currentWidth - timeWidth - PADDING, centerY + 12.0F, Color.WHITE, true, 0.35);
        }
    }
    
    private void renderResizeHandle(EventRender2D e, float centerX, float centerY) {
        // 滚动条位置：右下角，更大的尺寸使其更明显
        float handleX = centerX + currentWidth - 30.0F;
        float handleY = centerY + currentHeight - 30.0F;
        float handleSize = 25.0F;
        
        // 绘制滚动条背景 - 使用更明显的颜色
        RenderUtils.drawRoundedRect(e.getStack(), handleX, handleY, handleSize, handleSize, 5.0F, new Color(80, 80, 80, 230).getRGB());
        
        // 绘制滚动条边框 - 使其更明显
        RenderUtils.drawRoundedRectOutline(e.getStack(), handleX, handleY, handleSize, handleSize, 5.0F, 1.5F, new Color(120, 120, 120, 255).getRGB());
        
        // 绘制滚动条图标（箭头图标）
        float arrowSize = 6.0F;
        float arrowCenterX = handleX + handleSize / 2.0F;
        float arrowCenterY = handleY + handleSize / 2.0F;
        
        // 向右下角的箭头
        RenderUtils.fill(e.getStack(), arrowCenterX - 2.0F, arrowCenterY - arrowSize, arrowCenterX + 2.0F, arrowCenterY + arrowSize, Color.WHITE.getRGB());
        RenderUtils.fill(e.getStack(), arrowCenterX - arrowSize, arrowCenterY - 2.0F, arrowCenterX + arrowSize, arrowCenterY + 2.0F, Color.WHITE.getRGB());
        
        // 添加标签文字
        CustomTextRenderer font = Fonts.opensans;
        String resizeText = "▼";
        float textWidth = font.getWidth(resizeText, 0.3);
        font.render(e.getStack(), resizeText, arrowCenterX - textWidth / 2.0F, arrowCenterY - 4.0F, new Color(200, 200, 200, 255), true, 0.3);
    }
    
    private void renderNotifications(EventRender2D e, CustomTextRenderer font) {
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        float centerX = (screenWidth - currentWidth) / 2.0F;
        float centerY = 20.0F;
        
        float yOffset = islandHeight.getCurrentValue(); // 使用配置的基础高度
        
        // 黑灰色35%透明度的毛玻璃效果背景
        int glassColor = new Color(50, 50, 50, 89).getRGB(); // 35%透明度的黑灰色
        
        // 渲染通知项
        for (int i = 0; i < notifications.size(); i++) {
            NotificationItem item = notifications.get(i);
            float itemHeight = ITEM_HEIGHT * (item.getHeightAnimation().value / 100.0F);
            
            if (itemHeight > 0.0F) {
                // 添加通知项的模糊矩阵
                blurMatrices.add(new Vector4f(centerX, centerY + yOffset, currentWidth, itemHeight));
                
                // 绘制通知项背景，使用相同的黑灰色35%毛玻璃效果
                RenderUtils.drawRoundedRect(e.getStack(), centerX, centerY + yOffset, currentWidth, itemHeight, Math.min(CORNER_RADIUS, 8.0F), glassColor);
                
                // 绘制模块名称
                String moduleName = item.getModule().getName();
                font.render(e.getStack(), moduleName, centerX + PADDING, centerY + yOffset + 10.0F, Color.WHITE, true, 0.35);
                
                // 绘制开关
                float switchX = centerX + PADDING + font.getWidth(moduleName, 0.35) + 10.0F;
                float switchY = centerY + yOffset + (itemHeight - SWITCH_HEIGHT) / 2.0F;
                renderSwitch(e, switchX, switchY, item.getSwitchAnimation().value, item.isEnabled());
                
                yOffset += itemHeight;
            }
        }
    }
    
    private void renderSwitch(EventRender2D e, float x, float y, float value, boolean enabled) {
        // 开关颜色配置 - 更鲜艳的颜色
        int onColor = new Color(46, 204, 113, 255).getRGB();  // 鲜绿色
        int offColor = new Color(231, 76, 60, 255).getRGB();  // 鲜红色
        
        // 背景颜色根据动画值渐变
        int bgColor = interpolateColor(offColor, onColor, value / 100.0F);
        RenderUtils.drawRoundedRect(e.getStack(), x, y, SWITCH_WIDTH, SWITCH_HEIGHT, SWITCH_HEIGHT / 2.0F, bgColor);
        
        // 绘制开关滑块 - 带阴影效果
        float sliderX = x + (value / 100.0F) * (SWITCH_WIDTH - SWITCH_HEIGHT);
        
        // 滑块阴影
        RenderUtils.drawRoundedRect(e.getStack(), sliderX + 1.0F, y + 1.0F, SWITCH_HEIGHT, SWITCH_HEIGHT, SWITCH_HEIGHT / 2.0F, new Color(0, 0, 0, 80).getRGB());
        
        // 滑块主体 - 白色
        RenderUtils.drawRoundedRect(e.getStack(), sliderX, y, SWITCH_HEIGHT, SWITCH_HEIGHT, SWITCH_HEIGHT / 2.0F, Color.WHITE.getRGB());
        
        // 滑块内部小点
        float dotSize = 4.0F;
        float dotX = sliderX + SWITCH_HEIGHT / 2.0F - dotSize / 2.0F;
        float dotY = y + SWITCH_HEIGHT / 2.0F - dotSize / 2.0F;
        RenderUtils.fill(e.getStack(), dotX, dotY, dotX + dotSize, dotY + dotSize, enabled ? onColor : offColor);
    }
    
    private int interpolateColor(int color1, int color2, float ratio) {
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;
        
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;
        
        int r = (int) (r1 + (r2 - r1) * ratio);
        int g = (int) (g1 + (g2 - g1) * ratio);
        int b = (int) (b1 + (b2 - b1) * ratio);
        
        return (255 << 24) | (r << 16) | (g << 8) | b;
    }
    
    // 处理模块启用事件
    public void onModuleEnable(Module module) {
        addNotification(module, true);
    }
    
    // 处理模块禁用事件
    public void onModuleDisable(Module module) {
        addNotification(module, false);
    }
    
    private void addNotification(Module module, boolean enabled) {
        String moduleName = module.getName();
        
        // 移除现有通知（如果存在）
        notifications.removeIf(item -> item.getModule().getName().equals(moduleName));
        
        // 创建新通知
        NotificationItem newItem = new NotificationItem(module, enabled);
        notifications.add(0, newItem);
        
        // 限制最大通知数量
        if (notifications.size() > 5) {
            notifications.remove(notifications.size() - 1);
        }
        
        // 触发高度动画
        for (NotificationItem item : notifications) {
            item.getHeightAnimation().target = 100.0F;
        }
    }
    
    @Override
    public void onEnable() {
        // 注册模块状态变化监听器
        Naven.getInstance().getEventManager().register(this);
        
        // 初始通知
        addNotification(this, true);
    }
    
    @Override
    public void onDisable() {
        // 取消注册模块状态变化监听器
        Naven.getInstance().getEventManager().unregister(this);
        
        // 清空通知
        notifications.clear();
    }
}