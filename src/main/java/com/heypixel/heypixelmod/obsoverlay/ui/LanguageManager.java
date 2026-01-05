package com.heypixel.heypixelmod.obsoverlay.ui;

public class LanguageManager {
    private static LanguageManager instance;
    private boolean isChinese = true;

    private LanguageManager() {}

    public static LanguageManager getInstance() {
        if (instance == null) {
            instance = new LanguageManager();
        }
        return instance;
    }

    public void setLanguage(boolean chinese) {
        this.isChinese = chinese;
    }

    public boolean isChinese() {
        return isChinese;
    }

    public String getLocalizedString(String chineseText, String englishText) {
        return isChinese ? chineseText : englishText;
    }
}