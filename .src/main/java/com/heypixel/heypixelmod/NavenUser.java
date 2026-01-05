package com.heypixel.heypixelmod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Objects;

public class NavenUser {
    private static final String APPDATA = System.getenv("AppData");
    private static final String RELATIVE_PATH = "naven" + File.separator + "username" + File.separator + "name.txt";
    private static final File NAME_FILE = new File((APPDATA != null ? APPDATA : System.getProperty("user.home")), RELATIVE_PATH);
    private static String cachedUsername;
    private static boolean checkingOrOpeningScreen = false;
    public static void ensureUserLoaded() {
        if (cachedUsername != null && !cachedUsername.isBlank()) return;
        cachedUsername = readUsernameFromFile();
        if (cachedUsername != null && !cachedUsername.isBlank()) return;
        if (!checkingOrOpeningScreen) {
            checkingOrOpeningScreen = true;
            Minecraft mc = Minecraft.getInstance();
            if (mc != null) {
                mc.execute(() -> mc.setScreen(new UsernameScreen()));
            }
        }
    }
    public static String getUsername() {
        if (cachedUsername == null || cachedUsername.isBlank()) {
            cachedUsername = readUsernameFromFile();
        }
        return cachedUsername;
    }
    private static void saveUsernameToFile(String name) {
        if (name == null) return;
        String trimmed = name.trim();
        if (trimmed.isEmpty()) return;
        File parent = NAME_FILE.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        try (BufferedWriter bw = Files.newBufferedWriter(NAME_FILE.toPath(), StandardCharsets.UTF_8)) {
            bw.write(trimmed);
            bw.flush();
            cachedUsername = trimmed;
        } catch (IOException e) {
            System.err.println("[NavenUser]  write user name failed: " + e.getMessage());
        }
    }
    private static String readUsernameFromFile() {
        if (!NAME_FILE.exists()) return null;
        try (BufferedReader br = Files.newBufferedReader(NAME_FILE.toPath(), StandardCharsets.UTF_8)) {
            String line = br.readLine();
            return line != null ? line.trim() : null;
        } catch (IOException e) {
            System.err.println("[NavenUser] red user name failed: " + e.getMessage());
            return null;
        }
    }
    public static class UsernameScreen extends Screen {
        private EditBox input;
        private Button confirmBtn;
        public UsernameScreen() {
            super(Component.literal("InputYourUserName ( Register Or Login )"));
        }
        @Override
        protected void init() {
            int centerX = this.width / 2;
            int centerY = this.height / 2;
            int boxWidth = 220;
            int boxHeight = 20;
            input = new EditBox(this.font, centerX - boxWidth / 2, centerY - 10, boxWidth, boxHeight, Component.literal("username"));
            input.setMaxLength(64);
            input.setValue(Objects.toString(cachedUsername, ""));
            this.addRenderableWidget(input);
            confirmBtn = Button.builder(Component.literal("Confirm"), b -> onConfirm())
                    .bounds(centerX - 40, centerY + 20, 80, 20)
                    .build();
            this.addRenderableWidget(confirmBtn);
            setInitialFocus(input);
        }
        private void onConfirm() {
            String val = input.getValue();
            if (val != null && !val.trim().isEmpty()) {
                saveUsernameToFile(val.trim());
                Minecraft.getInstance().setScreen(null);
            }
        }
        @Override
        public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            guiGraphics.fill(0, 0, this.width, this.height, 0xFF101010);
            String title = "InputYourUserName ( Register Or Login )";
            int titleWidth = this.font.width(title);
            guiGraphics.drawString(this.font, title, (this.width - titleWidth) / 2, this.height / 2 - 40, 0xFFFFFFFF);
            super.render(guiGraphics, mouseX, mouseY, partialTick);
        }
        @Override
        public boolean isPauseScreen() {
            return true;
        }
    }
}