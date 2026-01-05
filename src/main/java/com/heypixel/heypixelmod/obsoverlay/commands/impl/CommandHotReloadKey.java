package com.heypixel.heypixelmod.obsoverlay.commands.impl;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.commands.Command;
import com.heypixel.heypixelmod.obsoverlay.commands.CommandInfo;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventKey;
import com.heypixel.heypixelmod.obsoverlay.utils.ChatUtils;
import com.mojang.blaze3d.platform.InputConstants;

@CommandInfo(
   name = "hotreloadkey",
   description = "设置热重载触发按键",
   aliases = {"hrk", "hotswapkey"}
)
public class CommandHotReloadKey extends Command {
    @Override
    public void onCommand(String[] args) {
        if (args.length == 0) {
            // 显示当前设置的按键
            int currentKey = Naven.getInstance().getModuleManager().getHotReloadKey();
            if (currentKey == -1) {
                ChatUtils.addChatMessage("当前未设置热重载按键。");
            } else {
                InputConstants.Key key = InputConstants.getKey(currentKey, 0);
                String keyName = key.getDisplayName().getString().toUpperCase();
                ChatUtils.addChatMessage("当前热重载按键: " + keyName);
            }
            ChatUtils.addChatMessage("用法: .hotreloadkey <key> 或 .hotreloadkey none (取消设置)");
            ChatUtils.addChatMessage("示例: .hotreloadkey f1, .hotreloadkey grave (`键)");
            return;
        }

        String keyName = args[0];
        if (keyName.equalsIgnoreCase("none")) {
            // 取消设置
            Naven.getInstance().getModuleManager().setHotReloadKey(-1);
            ChatUtils.addChatMessage("已取消热重载按键设置。");
            Naven.getInstance().getFileManager().save();
            return;
        }

        try {
            // 设置新的按键
            InputConstants.Key key = InputConstants.getKey("key.keyboard." + keyName.toLowerCase());
            if (key != InputConstants.UNKNOWN) {
                Naven.getInstance().getModuleManager().setHotReloadKey(key.getValue());
                ChatUtils.addChatMessage("热重载按键已设置为: " + keyName.toUpperCase());
                ChatUtils.addChatMessage("按 " + keyName.toUpperCase() + " 键即可触发热重载功能。");
                Naven.getInstance().getFileManager().save();
            } else {
                ChatUtils.addChatMessage("无效的按键名称: " + keyName);
                ChatUtils.addChatMessage("请使用标准按键名称，如: f1, f2, grave, lshift 等");
                ChatUtils.addChatMessage("提示: grave 是键盘上数字1左边的`键");
            }
        } catch (Exception e) {
            ChatUtils.addChatMessage("设置热重载按键时发生错误: " + e.getMessage());
        }
    }

    @Override
    public String[] onTab(String[] args) {
        if (args.length == 1) {
            return new String[]{"none", "f1", "f2", "f3", "f4", "f5", "f6", "f7", "f8", "f9", "f10", "f11", "f12", 
                               "grave", "1", "2", "3", "4", "5", "6", "7", "8", "9", "0", 
                               "minus", "equals", "backspace", "tab", "q", "w", "e", "r", "t", 
                               "y", "u", "i", "o", "p", "lbracket", "rbracket", "backslash", 
                               "capslock", "a", "s", "d", "f", "g", "h", "j", "k", "l", 
                               "semicolon", "apostrophe", "enter", "lshift", "z", "x", "c", "v", 
                               "b", "n", "m", "comma", "period", "slash", "rshift", "lcontrol", 
                               "lalt", "space", "ralt", "rcontrol"};
        }
        return new String[0];
    }
}