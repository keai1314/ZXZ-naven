package com.heypixel.heypixelmod.modules.impl.render;

import com.heypixel.heypixelmod.events.api.EventTarget;
import com.heypixel.heypixelmod.events.impl.EventRender2D;
import com.heypixel.heypixelmod.events.impl.EventShader;
import com.heypixel.heypixelmod.modules.Category;
import com.heypixel.heypixelmod.modules.Module;
import com.heypixel.heypixelmod.modules.ModuleInfo;
import com.heypixel.heypixelmod.ui.HUDEditor;
import com.heypixel.heypixelmod.utils.InventoryUtils;
import com.heypixel.heypixelmod.utils.RenderUtils;
import com.heypixel.heypixelmod.utils.StencilUtils;
import com.heypixel.heypixelmod.utils.renderer.Fonts;
import com.heypixel.heypixelmod.utils.renderer.text.CustomTextRenderer;
import com.heypixel.heypixelmod.values.ValueBuilder;
import com.heypixel.heypixelmod.values.impl.BooleanValue;
import com.heypixel.heypixelmod.values.impl.FloatValue;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static com.heypixel.heypixelmod.modules.impl.render.HUD.bodyColor;
import static com.heypixel.heypixelmod.modules.impl.render.HUD.headerColor;

@ModuleInfo(
        name = "Items",
        description = "Displays the number of specific items on your screen",
        category = Category.RENDER
)
public class ItemsCounter extends Module {

    public FloatValue counterSize = ValueBuilder.create(this, "Counter Size").setDefaultFloatValue(0.4F).setFloatStep(0.01F).setMinFloatValue(0.1F).setMaxFloatValue(1.0F).build().getFloatValue();
    public BooleanValue showQuantity = ValueBuilder.create(this, "Show Quantity").setDefaultBooleanValue(true).build().getBooleanValue();

    private float finalWidth = 0;
    private float finalHeight = 0;
    private float currentWidth = 0;
    private float currentHeight = 0;
    private long lastUpdateTime = 0;
    private final List<String> textLines = new ArrayList<>();
    private float textHeight;

    @EventTarget
    public void onShader(EventShader e) {
        if (!this.isEnabled()) {
            if (currentWidth != 0 || currentHeight != 0) {
                currentWidth = 0;
                finalWidth = 0;
                currentHeight = 0;
                finalHeight = 0;
            }
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        CustomTextRenderer font = Fonts.opensans;

        int totemCount = InventoryUtils.getItemCount(Items.TOTEM_OF_UNDYING);
        int crystalCount = InventoryUtils.getItemCount(Items.END_CRYSTAL);
        int enderPearlCount = InventoryUtils.getItemCount(Items.ENDER_PEARL);
        int gAppleCount = InventoryUtils.getItemCount(Items.ENCHANTED_GOLDEN_APPLE);
        int godAxeCount = 0;
        int kbBallCount = 0;
        int sharpnessAxeCount = 0;
        if (mc.player != null) {
            var inventory = mc.player.getInventory();
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                ItemStack itemStack = inventory.getItem(i);
                if (InventoryUtils.isGodAxe(itemStack)) godAxeCount++;
                if (InventoryUtils.isKBBall(itemStack)) kbBallCount++;
                if (InventoryUtils.isSharpnessAxe(itemStack)) sharpnessAxeCount++;
            }
        }

        boolean hasItems = totemCount > 0 || crystalCount > 0 || enderPearlCount > 0 || gAppleCount > 0 || godAxeCount > 0 || kbBallCount > 0 || sharpnessAxeCount > 0;

        textLines.clear();
        if (hasItems) {
            textHeight = (float) font.getHeight(true, this.counterSize.getCurrentValue());
            float maxWidth = 0.0F;
            textLines.add("Items");
            if (totemCount > 0) textLines.add(showQuantity.getCurrentValue() ? "Totem of Undying x" + totemCount : "Totem of Undying");
            if (crystalCount > 0) textLines.add(showQuantity.getCurrentValue() ? "End Crystal x" + crystalCount : "End Crystal");
            if (enderPearlCount > 0) textLines.add(showQuantity.getCurrentValue() ? "Ender Pearl x" + enderPearlCount : "Ender Pearl");
            if (gAppleCount > 0) textLines.add(showQuantity.getCurrentValue() ? "Enchanted Golden Apple x" + gAppleCount : "Enchanted Golden Apple");
            if (godAxeCount > 0) textLines.add(showQuantity.getCurrentValue() ? "God Axe x" + godAxeCount : "God Axe");
            if (kbBallCount > 0) textLines.add(showQuantity.getCurrentValue() ? "KB Ball x" + kbBallCount : "KB Ball");
            if (sharpnessAxeCount > 0) textLines.add(showQuantity.getCurrentValue() ? "Sharpness Axe x" + sharpnessAxeCount : "Sharpness Axe");

            textLines.sort(Comparator.comparingDouble(line -> font.getWidth(line, this.counterSize.getCurrentValue())));

            for (String line : textLines) {
                float lineWidth = font.getWidth(line, this.counterSize.getCurrentValue());
                if (lineWidth > maxWidth) maxWidth = lineWidth;
            }
            finalWidth = maxWidth + 8.0F;
            finalHeight = (textLines.size() * textHeight) + 6.0F;
            if (textLines.size() > 2) {
                finalHeight -= (textLines.size() - 2) * (textHeight * 0.125F);
            }
        } else {
            finalWidth = 0;
            finalHeight = 0;
        }

        long currentTime = System.currentTimeMillis();
        if (lastUpdateTime == 0) lastUpdateTime = currentTime;
        float deltaTime = (currentTime - lastUpdateTime) / 1000.0F;
        float animationSpeed = 10.0F;
        currentWidth += (finalWidth - currentWidth) * animationSpeed * deltaTime;
        currentHeight += (finalHeight - currentHeight) * animationSpeed * deltaTime;
        lastUpdateTime = currentTime;
        if (Math.abs(finalWidth - currentWidth) < 0.01f) currentWidth = finalWidth;
        if (Math.abs(finalHeight - currentHeight) < 0.01f) currentHeight = finalHeight;

        if (HUDEditor.getInstance() != null && HUDEditor.getInstance().isEditMode()) {
            HUDEditor.HUDElement element = HUDEditor.getInstance().getHUDElement("itemscounter");
            if (element != null) {
                element.width = Math.max(currentWidth, 100);
                element.height = Math.max(currentHeight, 80);
            }
        }

        if (currentWidth > 0.1f && currentHeight > 0.1f) {
            float x = 10.0F;
            float y = 10.0F;

            if (HUDEditor.getInstance() != null) {
                HUDEditor.HUDElement element = HUDEditor.getInstance().getHUDElement("itemscounter");
                if (element != null) {
                    x = (float) element.x;
                    y = (float) element.y;
                }
            }

            RenderUtils.drawRoundedRect(e.getStack(), x, y, currentWidth, currentHeight, 5.0F, 1073741824);
        }
    }

    @EventTarget
    public void onRender(EventRender2D e) {
        if (!this.isEnabled()) return;

        if (currentWidth > 0.1f && currentHeight > 0.1f && !textLines.isEmpty()) {
            float x = 10.0F;
            float y = 10.0F;

            if (HUDEditor.getInstance() != null) {
                HUDEditor.HUDElement element = HUDEditor.getInstance().getHUDElement("itemscounter");
                if (element != null) {
                    x = (float) element.x;
                    y = (float) element.y;
                }
            }

            CustomTextRenderer font = Fonts.opensans;

            e.getStack().pushPose();

            StencilUtils.write(false);
            RenderUtils.drawRoundedRect(e.getStack(), x, y, currentWidth, currentHeight, 5.0F, Integer.MIN_VALUE);
            StencilUtils.erase(true);
            RenderUtils.drawRoundedRect(e.getStack(), x, y, currentWidth, currentHeight, 5.0F, bodyColor);
            RenderUtils.drawRoundedRect(e.getStack(), x, y, currentWidth, 3.0F, 5.0F, headerColor);
            RenderUtils.fill(e.getStack(), x, y + 2.0F, x + currentWidth, y + 3.0F, headerColor);

            float currentTextY = y + 3.0F;

            font.drawString(e.getStack(), textLines.get(0), (double) (x + 4.0F), (double) currentTextY, Color.WHITE, true, (double) this.counterSize.getCurrentValue());
            currentTextY += textHeight;

            for (int i = 1; i < textLines.size(); i++) {
                font.drawString(e.getStack(), textLines.get(i), (double) (x + 4.0F), (double) currentTextY, Color.WHITE, true, (double) this.counterSize.getCurrentValue());
                currentTextY += textHeight * 0.875F;
            }

            StencilUtils.dispose();
            e.getStack().popPose();
        }
    }
}