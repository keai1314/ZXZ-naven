package com.heypixel.heypixelmod.modules.impl.misc;

import com.heypixel.heypixelmod.modules.Category;
import com.heypixel.heypixelmod.modules.Module;
import com.heypixel.heypixelmod.modules.ModuleInfo;
import com.heypixel.heypixelmod.utils.FontLoader;
import com.heypixel.heypixelmod.utils.renderer.Fonts;
import com.heypixel.heypixelmod.values.ValueBuilder;
import com.heypixel.heypixelmod.values.impl.BooleanValue;
import com.heypixel.heypixelmod.values.impl.ModeValue;
import com.heypixel.heypixelmod.utils.renderer.text.CustomTextRenderer;

@ModuleInfo(
        name = "FontSelect",
        description = "Select custom font for UI rendering",
        category = Category.MISC
)
public class FontSelect extends Module {

    private final ModeValue fontOption = ValueBuilder.create(this, "Font")
            .setModes(FontLoader.getAvailableFonts())
            .setDefaultModeIndex(0)
            .build()
            .getModeValue();

    private final BooleanValue customChineseFonts = ValueBuilder.create(this, "Custom Chinese Fonts")
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();

    private final ModeValue chineseFontOption = ValueBuilder.create(this, "Chinese Font")
            .setVisibility(this.customChineseFonts::getCurrentValue)
            .setModes(FontLoader.getCJKFonts())
            .setDefaultModeIndex(0)
            .build()
            .getModeValue();

    public FontSelect() {
        super("FontSelect", "Select custom font for UI rendering", Category.MISC);

        if (this.isEnabled()) {
            updateFont();
        }
    }
    @Override
    public void onEnable() {
        updateFont();
    }
    @Override
    public void onDisable() {
        try {
            Fonts.reloadFonts("opensans");
        } catch (Exception e) {
            System.err.println("Error resetting fonts to default");
            e.printStackTrace();
        }
    }
    public void onValueChange() {
        if (this.isEnabled()) {
            updateFont();
        }
    }

    public void updateFont() {
        try {
            String selectedFont = fontOption != null ? fontOption.getCurrentMode() : null;
            if (selectedFont != null && !selectedFont.isEmpty()) {
                Fonts.reloadFonts(selectedFont);
            } else {
                Fonts.reloadFonts("opensans");
            }
            if (customChineseFonts != null && customChineseFonts.getCurrentValue()) {
                String selectedChinese = chineseFontOption != null ? chineseFontOption.getCurrentMode() : null;
                if (selectedChinese == null || selectedChinese.isEmpty()) {
                    selectedChinese = "HYWenHei 85W";
                }
                Fonts.harmony = new CustomTextRenderer(selectedChinese, 32, 0, 65535, 16384);
            } else if (selectedFont != null && isChineseFont(selectedFont)) {
                Fonts.harmony = new CustomTextRenderer(selectedFont, 32, 0, 65535, 16384);
            }
        } catch (Exception e) {
            System.err.println("Error updating font");
            e.printStackTrace();

            try {
                Fonts.reloadFonts("opensans");
                Fonts.harmony = new CustomTextRenderer("HYWenHei 85W", 32, 0, 65535, 16384);
            } catch (Exception ex) {
                System.err.println("Error resetting to default font");
                ex.printStackTrace();
            }
        }
    }

    private boolean isChineseFont(String fontName) {
        return "HYWenHei 85W".equals(fontName) || "harmony".equals(fontName) || fontName.contains("中") || fontName.contains("汉");
    }

    public String getSelectedFont() {
        if (fontOption != null) {
            return fontOption.getCurrentMode();
        }
        return "opensans";
    }

    public String getSelectedChineseFont() {
        if (customChineseFonts != null && customChineseFonts.getCurrentValue()) {

            if (chineseFontOption != null) {
                return chineseFontOption.getCurrentMode();
            }
        }

        return "HYWenHei 85W";
    }

    public boolean isCustomChineseFontsEnabled() {
        return customChineseFonts != null && customChineseFonts.getCurrentValue();
    }
}