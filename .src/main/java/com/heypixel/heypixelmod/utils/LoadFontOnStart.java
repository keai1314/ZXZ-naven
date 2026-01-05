package com.heypixel.heypixelmod.utils;

import com.heypixel.heypixelmod.Naven;
import com.heypixel.heypixelmod.modules.impl.misc.FontSelect;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "naven", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class LoadFontOnStart {

    public static void loadUserSelectedFont() {
        try {

            Naven naven = Naven.getInstance();
            if (naven != null) {

                FontSelect fontSelectModule = (FontSelect) naven.getModuleManager().getModule(FontSelect.class);
                if (fontSelectModule != null && fontSelectModule.isEnabled()) {

                    fontSelectModule.updateFont();
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading user selected font on startup");
            e.printStackTrace();
        }
    }
}