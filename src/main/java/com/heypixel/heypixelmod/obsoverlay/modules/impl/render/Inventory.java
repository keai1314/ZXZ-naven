package com.heypixel.heypixelmod.obsoverlay.modules.impl.render;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRender2D;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.utils.RenderUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.Fonts;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.text.CustomTextRenderer;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.ItemStack;
import java.awt.Color;
import com.heypixel.heypixelmod.obsoverlay.ui.LanguageManager;

@ModuleInfo(
        name = "背包显示",
        description = "显示你的背包物品",
        category = Category.RENDER
)
public class Inventory extends Module {
    // 位置设置
    public FloatValue xPos = ValueBuilder.create(this, "X坐标")
            .setDefaultFloatValue(100.0F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(1000.0F)
            .setFloatStep(1.0F)
            .build()
            .getFloatValue();

    public FloatValue yPos = ValueBuilder.create(this, "Y坐标")
            .setDefaultFloatValue(100.0F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(1000.0F)
            .setFloatStep(1.0F)
            .build()
            .getFloatValue();

    // 大小设置
    public FloatValue scale = ValueBuilder.create(this, "大小")
            .setDefaultFloatValue(1.0F)
            .setMinFloatValue(0.1F)
            .setMaxFloatValue(3.0F)
            .setFloatStep(0.1F)
            .build()
            .getFloatValue();

    // 透明度设置
    public FloatValue alpha = ValueBuilder.create(this, "透明度")
            .setDefaultFloatValue(0.8F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(1.0F)
            .setFloatStep(0.05F)
            .build()
            .getFloatValue();

    // 圆滑度设置
    public FloatValue roundness = ValueBuilder.create(this, "圆滑度")
            .setDefaultFloatValue(0.0F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(15.0F)
            .setFloatStep(0.5F)
            .build()
            .getFloatValue();

    // 显示背景
    public BooleanValue showBackground = ValueBuilder.create(this, "显示背景")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();

    public Inventory() {
        super("背包显示", "显示玩家背包物品的可视化辅助模块", Category.RENDER);
    }

    @EventTarget
    public void onRender2D(EventRender2D event) {
        if (mc.player == null) return;

        // 检查当前屏幕类型，只在特定屏幕下隐藏显示
        Screen currentScreen = mc.screen;
        if (currentScreen != null &&
                !(currentScreen instanceof ChatScreen) &&
                !isClickGuiScreen(currentScreen)) {
            return;
        }

        GuiGraphics guiGraphics = event.getGuiGraphics();
        float x = xPos.getCurrentValue();
        float y = yPos.getCurrentValue();
        float size = scale.getCurrentValue();
        float bgAlpha = alpha.getCurrentValue();
        float roundnessValue = roundness.getCurrentValue();

        // 计算整体背景的尺寸
        float totalWidth = 176 * size;
        float titleHeight = 15;
        float contentHeight = 64 * size;
        float totalHeight = titleHeight + contentHeight;

        // 绘制整体圆角背景（包含标题栏和内容区域）
        if (showBackground.getCurrentValue()) {
            int bgColor = new Color(0, 0, 0, (int) (255 * bgAlpha)).getRGB();
            if (roundnessValue > 0) {
                RenderUtils.drawRoundedRect(guiGraphics.pose(), x, y - titleHeight, totalWidth, totalHeight, roundnessValue, bgColor);
            } else {
                RenderUtils.fillBound(guiGraphics.pose(), x, y - titleHeight, totalWidth, totalHeight, bgColor);
            }
        } else if (roundnessValue > 0) {
            // 即使不显示背景，如果设置了圆角也需要绘制圆角边框
            int borderColor = new Color(240, 240, 240, (int) (100 * bgAlpha)).getRGB();
            RenderUtils.drawRoundedRect(guiGraphics.pose(), x, y - titleHeight, totalWidth, totalHeight, roundnessValue, borderColor);
        }

        // 绘制标题栏背景，使用与NewHUD中"Naven"水印相似的颜色方案
        int titleBarColor = new Color(240, 240, 240, (int) (100 * bgAlpha)).getRGB();
        if (roundnessValue > 0) {
            // 使用圆角矩形绘制标题栏背景，但只保留顶部圆角
            RenderUtils.drawRoundedRect(guiGraphics.pose(), x, y - titleHeight, totalWidth, titleHeight, roundnessValue, titleBarColor);
        } else {
            RenderUtils.fillBound(guiGraphics.pose(), x, y - titleHeight, totalWidth, titleHeight, titleBarColor);
        }

        // 绘制标题文本"玩家背包"，使用与NewHUD中"Naven"水印相同的字体和颜色
        CustomTextRenderer font = Fonts.opensans;
        // 调整Y坐标使文本不与背包框重叠，并使用与NewHUD相同的颜色
        font.render(guiGraphics.pose(), "玩家背包", x + 5, y - 16, new Color(255, 255, 255, 200), true, 0.5);

        // 绘制物品栏网格 (玩家背包有27个物品槽位，3行9列)
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        for (int i = 9; i < 36; i++) { // 只显示背包物品，不包括快捷栏
            ItemStack itemStack = mc.player.getInventory().getItem(i);
            if (!itemStack.isEmpty()) {
                // 计算物品在网格中的位置
                int row = (i - 9) / 9;
                int col = (i - 9) % 9;

                float itemX = x + (8 + col * 18) * size;
                float itemY = y + (8 + row * 18) * size;

                // 绘制物品
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(itemX, itemY, 0);
                guiGraphics.pose().scale(size, size, 1);
                guiGraphics.renderItem(itemStack, 0, 0);
                guiGraphics.renderItemDecorations(mc.font, itemStack, 0, 0);
                guiGraphics.pose().popPose();
            }
        }

        RenderSystem.disableBlend();
    }

    /**
     * 检查当前屏幕是否为ClickGUI界面
     * @param screen 当前屏幕
     * @return 是否为ClickGUI界面
     */
    private boolean isClickGuiScreen(Screen screen) {
        // 根据ClickGUIModule类名判断
        return screen.getClass().getSimpleName().contains("Click") ||
                screen.getClass().getName().contains("Click") ||
                screen.getClass().getSimpleName().equals("ClickGUI");
    }
    
    public String getLocalizedName() {
      return LanguageManager.getInstance().getLocalizedString("背包显示", "Inventory Display");
   }

   public String getLocalizedDescription() {
      return LanguageManager.getInstance().getLocalizedString("显示你的背包物品", "Display your inventory items");
   }
}
