package com.heypixel.heypixelmod.modules.impl.render;

import com.heypixel.heypixelmod.events.api.EventTarget;
import com.heypixel.heypixelmod.events.impl.EventRender2D;
import com.heypixel.heypixelmod.events.impl.EventShader;
import com.heypixel.heypixelmod.modules.Category;
import com.heypixel.heypixelmod.modules.Module;
import com.heypixel.heypixelmod.modules.ModuleInfo;
import com.heypixel.heypixelmod.ui.HUDEditor;
import com.heypixel.heypixelmod.utils.RenderUtils;
import com.heypixel.heypixelmod.utils.StencilUtils;
import com.heypixel.heypixelmod.utils.renderer.Fonts;
import com.heypixel.heypixelmod.utils.renderer.text.CustomTextRenderer;
import com.heypixel.heypixelmod.values.ValueBuilder;
import com.heypixel.heypixelmod.values.impl.BooleanValue;
import com.heypixel.heypixelmod.values.impl.FloatValue;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;

import java.awt.*;

import static com.heypixel.heypixelmod.modules.impl.render.HUD.bodyColor;
import static com.heypixel.heypixelmod.modules.impl.render.HUD.headerColor;

@ModuleInfo(
        name = "ArmorRender",
        description = "Displays your armor and their durability horizontally",
        category = Category.RENDER
)
public class ArmorRender extends Module {

    public FloatValue scale = ValueBuilder.create(this, "Scale")
            .setDefaultFloatValue(1.0F)
            .setFloatStep(0.1F)
            .setMinFloatValue(0.5F)
            .setMaxFloatValue(2.0F)
            .build()
            .getFloatValue();

    public BooleanValue showDurability = ValueBuilder.create(this, "Show Durability")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();

    public FloatValue textSize = ValueBuilder.create(this, "Text Size")
            .setVisibility(this.showDurability::getCurrentValue)
            .setDefaultFloatValue(0.35F)
            .setFloatStep(0.05F)
            .setMinFloatValue(0.2F)
            .setMaxFloatValue(0.8F)
            .build()
            .getFloatValue();

    private float currentWidth = 0;
    private float currentHeight = 0;
    private long lastUpdateTime = 0;

    @EventTarget
    public void onShader(EventShader e) {
        if (!this.isEnabled()) {
            if (currentWidth != 0 || currentHeight != 0) {
                currentWidth = 0;
                currentHeight = 0;
            }
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        float itemSize = 16.0F * this.scale.getCurrentValue();
        float spacing = 4.0F * this.scale.getCurrentValue();
        float padding = 8.0F * this.scale.getCurrentValue();

        float contentWidth = (itemSize * 4) + (spacing * 3);
        float totalWidth = contentWidth + (padding * 2);

        float contentHeight = itemSize;
        if (this.showDurability.getCurrentValue()) {
            CustomTextRenderer font = Fonts.opensans;
            float textHeight = (float) font.getHeight(true, this.textSize.getCurrentValue());
            contentHeight += textHeight + (spacing * 0.5F);
        }
        float totalHeight = contentHeight + (padding * 2);

        float finalWidth = totalWidth;
        float finalHeight = totalHeight;

        long currentTime = System.currentTimeMillis();
        if (lastUpdateTime == 0) lastUpdateTime = currentTime;
        float deltaTime = (currentTime - lastUpdateTime) / 1000.0F;
        float animationSpeed = 10.0F;

        currentWidth += (finalWidth - currentWidth) * animationSpeed * deltaTime;
        currentHeight += (finalHeight - currentHeight) * animationSpeed * deltaTime;
        lastUpdateTime = currentTime;

        if (Math.abs(finalWidth - currentWidth) < 0.01f) currentWidth = finalWidth;
        if (Math.abs(finalHeight - currentHeight) < 0.01f) currentHeight = finalHeight;

        if (HUDEditor.getInstance() != null) {
            HUDEditor.HUDElement element = HUDEditor.getInstance().getHUDElement("armorrender");
            if (element != null) {
                element.width = currentWidth;
                element.height = currentHeight;
            }
        }

        float x = getX();
        float y = getY();

        if (currentWidth > 0.1f && currentHeight > 0.1f) {
            RenderUtils.drawRoundedRect(e.getStack(), x, y, currentWidth, currentHeight, 5.0F, 1073741824);
        }
    }

    @EventTarget
    @SuppressWarnings("null")
    public void onRender(EventRender2D e) {
        if (!this.isEnabled()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        if (currentWidth <= 0.1f || currentHeight <= 0.1f) return;

        float x = getX();
        float y = getY();

        e.getStack().pushPose();

        StencilUtils.write(false);
        RenderUtils.drawRoundedRect(e.getStack(), x, y, currentWidth, currentHeight, 5.0F, Integer.MIN_VALUE);
        StencilUtils.erase(true);

        RenderUtils.drawRoundedRect(e.getStack(), x, y, currentWidth, currentHeight, 5.0F, bodyColor);
        RenderUtils.drawRoundedRect(e.getStack(), x, y, currentWidth, 3.0F, 5.0F, headerColor);
        RenderUtils.fill(e.getStack(), x, y + 2.0F, x + currentWidth, y + 3.0F, headerColor);

        float itemSize = 16.0F * this.scale.getCurrentValue();
        float spacing = 4.0F * this.scale.getCurrentValue();
        float padding = 8.0F * this.scale.getCurrentValue();

        float startX = x + padding;
        float startY = y + padding;

        CustomTextRenderer font = Fonts.opensans;

        for (int i = 3; i >= 0; i--) {
            ItemStack armorStack = mc.player.getInventory().getArmor(i);

            float currentX = startX + ((3 - i) * (itemSize + spacing));

            if (!armorStack.isEmpty()) {

                float slotPadding = 2.0F * this.scale.getCurrentValue();
                RenderUtils.drawRoundedRect(e.getStack(),
                        currentX - slotPadding,
                        startY - slotPadding,
                        itemSize + (slotPadding * 2),
                        itemSize + (slotPadding * 2),
                        3.0F,
                        new Color(255, 255, 255, 40).getRGB());

                e.getStack().pushPose();
                float scale = itemSize / 16.0F;
                e.getStack().scale(scale, scale, 1.0F);
                e.getGuiGraphics().renderItem(armorStack, (int)(currentX / scale), (int)(startY / scale));
                e.getStack().popPose();

                if (this.showDurability.getCurrentValue() && armorStack.getItem() instanceof ArmorItem) {
                    int maxDamage = armorStack.getMaxDamage();
                    int damage = armorStack.getDamageValue();
                    int durability = maxDamage - damage;

                    float durabilityPercent = (float) durability / (float) maxDamage;

                    Color durabilityColor;
                    if (durabilityPercent > 0.6F) {
                        durabilityColor = new Color(0, 255, 0);
                    } else if (durabilityPercent > 0.3F) {
                        durabilityColor = new Color(255, 255, 0);
                    } else {
                        durabilityColor = new Color(255, 0, 0);
                    }

                    String durabilityText = durability + "/" + maxDamage;
                    float textWidth = font.getWidth(durabilityText, this.textSize.getCurrentValue());
                    float textX = currentX + (itemSize / 2) - (textWidth / 2);
                    float textY = startY + itemSize + (spacing * 0.5F);

                    float textHeight = (float) font.getHeight(true, this.textSize.getCurrentValue());
                    float textBgPadding = 1.0F * this.scale.getCurrentValue();
                    RenderUtils.drawRoundedRect(e.getStack(),
                            textX - textBgPadding,
                            textY - textBgPadding,
                            textWidth + (textBgPadding * 2),
                            textHeight + (textBgPadding * 2),
                            2.0F,
                            new Color(0, 0, 0, 100).getRGB());

                    font.drawString(e.getStack(), durabilityText, textX, textY, durabilityColor, true, this.textSize.getCurrentValue());
                }
            } else {

                RenderUtils.drawRoundedRect(e.getStack(), currentX, startY, itemSize, itemSize, 3.0F,
                        new Color(255, 255, 255, 30).getRGB());
            }
        }

        StencilUtils.dispose();
        e.getStack().popPose();
    }

    private float getX() {
        if (HUDEditor.getInstance() != null) {
            HUDEditor.HUDElement element = HUDEditor.getInstance().getHUDElement("armorrender");
            if (element != null) {
                return (float) element.x;
            }
        }

        return 10.0F;
    }

    private float getY() {
        if (HUDEditor.getInstance() != null) {
            HUDEditor.HUDElement element = HUDEditor.getInstance().getHUDElement("armorrender");
            if (element != null) {
                return (float) element.y;
            }
        }

        Minecraft mc = Minecraft.getInstance();
        return mc.getWindow().getGuiScaledHeight() - 80.0F;
    }
}
