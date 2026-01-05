package com.heypixel.heypixelmod.ui.MainUI;

import com.heypixel.heypixelmod.utils.RenderUtils;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;
import java.awt.Color;

public class MainUI extends Screen {
    private static final Minecraft mc = Minecraft.getInstance();
    private static final ResourceLocation BACKGROUND_TEXTURE = new ResourceLocation("heypixel", "textures/images/background.png");

    private static final int BUTTON_WIDTH = 200;
    private static final int BUTTON_HEIGHT = 35;
    private static final int BUTTON_SPACING = 15;

    private boolean textureLoaded = false;
    private Button[] buttons;

    public MainUI() {
        super(Component.literal("Naven-XD Alpha"));
    }

    @Override
    protected void init() {
        super.init();
        textureLoaded = checkTextureLoaded();

        int panelWidth = 240;
        int panelX = this.width - panelWidth;

        int startY = this.height / 2 - (4 * BUTTON_HEIGHT + 3 * BUTTON_SPACING) / 2 + 30;

        buttons = new Button[] {
                new Button(panelX + (panelWidth - BUTTON_WIDTH) / 2, startY, BUTTON_WIDTH, BUTTON_HEIGHT, "Singleplayer", this::openSingleplayer),
                new Button(panelX + (panelWidth - BUTTON_WIDTH) / 2, startY + (BUTTON_HEIGHT + BUTTON_SPACING), BUTTON_WIDTH, BUTTON_HEIGHT, "Multiplayer", this::openMultiplayer),
                new Button(panelX + (panelWidth - BUTTON_WIDTH) / 2, startY + (BUTTON_HEIGHT + BUTTON_SPACING) * 2, BUTTON_WIDTH, BUTTON_HEIGHT, "Settings", this::openSettings),
                new Button(panelX + (panelWidth - BUTTON_WIDTH) / 2, startY + (BUTTON_HEIGHT + BUTTON_SPACING) * 3, BUTTON_WIDTH, BUTTON_HEIGHT, "Exit", this::quit, true)
        };
    }

    private boolean checkTextureLoaded() {
        try {
            mc.getResourceManager().getResourceOrThrow(BACKGROUND_TEXTURE);
            return true;
        } catch (Exception e) {
            System.err.println("Failed to load background texture: " + e.getMessage());
            return false;
        }
    }

    @Override
    public void render(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        drawBackground(guiGraphics);
        renderSidePanel(guiGraphics);
        renderTitle(guiGraphics);

        for (Button button : buttons) {
            button.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void renderSidePanel(GuiGraphics guiGraphics) {
        int panelWidth = 240;
        int panelX = this.width - panelWidth;
        int panelY = 0;
        int panelHeight = this.height;

        guiGraphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0x80000000);

        renderGlowEffect(guiGraphics, panelX, panelY, panelWidth, panelHeight);
    }

    private void renderGlowEffect(GuiGraphics guiGraphics, int x, int y, int width, int height) {

        for (int i = 0; i < 5; i++) {
            int alpha = 30 - i * 6;
            int color = (alpha << 24) | 0xFFFFFF;
            RenderUtils.drawRoundedRect(
                guiGraphics.pose(),
                x - i,
                y - i,
                width + 2 * i,
                height + 2 * i,
                5,
                color
            );
        }
    }

    private void drawBackground(@Nonnull GuiGraphics guiGraphics) {
        guiGraphics.fill(0, 0, this.width, this.height, 0xFF000000);

        if (textureLoaded) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            guiGraphics.blit(BACKGROUND_TEXTURE, 0, 0, 0, 0, this.width, this.height, this.width, this.height);
            RenderSystem.disableBlend();
        }

        guiGraphics.fill(0, 0, this.width, this.height, 0x50000000);
    }

    private void renderTitle(GuiGraphics guiGraphics) {
        String title = "Naven-XD";
        int titleWidth = this.font.width(title);
        int panelWidth = 240;
        int panelX = this.width - panelWidth;

        int titleX = panelX + (panelWidth - titleWidth * 2) / 2;
        int titleY = this.height / 2 - (4 * BUTTON_HEIGHT + 3 * BUTTON_SPACING) / 2 - 40;

        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(2.0f, 2.0f, 1.0f);
        guiGraphics.drawString(this.font, title, titleX / 2, titleY / 2, 0xFFFFFFFF, true);
        guiGraphics.pose().popPose();
    }

    private void openSingleplayer() {
        mc.setScreen(new SelectWorldScreen(this));
    }

    private void openMultiplayer() {
        mc.setScreen(new JoinMultiplayerScreen(this));
    }

    private void openSettings() {
        try {
            Options options = mc.options;
            Class<?> clazz = Class.forName("net.minecraft.client.gui.screens.options.OptionsScreen");
            java.lang.reflect.Constructor<?> ctor = clazz.getConstructor(Screen.class, Options.class);
            Screen screen = (Screen) ctor.newInstance(this, options);
            mc.setScreen(screen);
        } catch (Throwable t) {
            try {
                Class<?> clazz = Class.forName("net.minecraft.client.gui.screens.OptionsScreen");
                java.lang.reflect.Constructor<?> ctor = clazz.getConstructor(Screen.class, Options.class);
                Screen screen = (Screen) ctor.newInstance(this, mc.options);
                mc.setScreen(screen);
            } catch (Throwable ignored) {
                System.err.println("Failed to open options screen");
            }
        }
    }

    private void quit() {
        mc.stop();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (Button btn : buttons) {
                if (btn.isHovered((int)mouseX, (int)mouseY)) {
                    btn.onClick();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private static class Button {
        private final int x, y, width, height;
        private final String text;
        private final Runnable action;
        private final boolean isDanger;
        private float hoverProgress = 0.0f;

        public Button(int x, int y, int width, int height, String text, Runnable action) {
            this(x, y, width, height, text, action, false);
        }

        public Button(int x, int y, int width, int height, String text, Runnable action, boolean isDanger) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.text = text;
            this.action = action;
            this.isDanger = isDanger;
        }

        public boolean isHovered(int mouseX, int mouseY) {
            return RenderUtils.isHoveringBound(mouseX, mouseY, x, y, width, height);
        }

        public void onClick() {
            action.run();
        }

        public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            boolean hovered = isHovered(mouseX, mouseY);

            float speed = 5.0f;
            hoverProgress += (hovered ? 1 : -1) * speed * (1.0f / Minecraft.getInstance().getFps());
            hoverProgress = Math.max(0.0f, Math.min(1.0f, hoverProgress));

            float easedProgress = (float) (1 - Math.pow(1 - hoverProgress, 4));

            renderButton(guiGraphics, easedProgress);

            Minecraft mc = Minecraft.getInstance();
            int textWidth = mc.font.width(text);
            int textX = x + (width - textWidth) / 2;
            int textY = y + (height - 8) / 2;

            int textColor = 0xFFFFFFFF;
            guiGraphics.drawString(mc.font, text, textX, textY, textColor, true);

            RenderSystem.disableBlend();
        }

        private void renderButton(GuiGraphics guiGraphics, float hoverProgress) {

            float cornerRadius = 5.0f;

            int baseColor = 0x40FFFFFF;

            int hoverColor = 0x60FFFFFF;

            int buttonColor = interpolateColor(baseColor, hoverColor, hoverProgress);

            RenderUtils.drawRoundedRect(
                guiGraphics.pose(),
                x,
                y,
                width,
                height,
                cornerRadius,
                buttonColor
            );

            if (hoverProgress > 0) {

                long time = System.currentTimeMillis();
                for (int i = 0; i < 5; i++) {

                    int rainbowColor = getRainbowColor(time + i * 100);
                    int alpha = (int) (hoverProgress * (50 - i * 10));
                    int glowColor = (alpha << 24) | (rainbowColor & 0x00FFFFFF);

                    RenderUtils.drawRoundedRect(
                        guiGraphics.pose(),
                        x - i,
                        y - i,
                        width + 2 * i,
                        height + 2 * i,
                        cornerRadius + i,
                        glowColor
                    );
                }
            }
        }

        private int getRainbowColor(long time) {

            float hue = (time % 5000) / 5000.0f;
            int rgb = Color.HSBtoRGB(hue, 1.0f, 1.0f);
            return rgb & 0x00FFFFFF;
        }

        private int rgba(int r, int g, int b, int a) {
            return (a << 24) | (r << 16) | (g << 8) | b;
        }

        private int interpolateColor(int color1, int color2, float progress) {
            int a1 = (color1 >> 24) & 0xFF;
            int r1 = (color1 >> 16) & 0xFF;
            int g1 = (color1 >> 8) & 0xFF;
            int b1 = color1 & 0xFF;

            int a2 = (color2 >> 24) & 0xFF;
            int r2 = (color2 >> 16) & 0xFF;
            int g2 = (color2 >> 8) & 0xFF;
            int b2 = color2 & 0xFF;

            int a = (int)(a1 + (a2 - a1) * progress);
            int r = (int)(r1 + (r2 - r1) * progress);
            int g = (int)(g1 + (g2 - g1) * progress);
            int b = (int)(b1 + (b2 - b1) * progress);

            return rgba(r, g, b, a);
        }
    }
}