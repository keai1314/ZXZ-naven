package com.heypixel.heypixelmod.obsoverlay.commands.impl;

import com.heypixel.heypixelmod.obsoverlay.commands.Command;
import com.heypixel.heypixelmod.obsoverlay.commands.CommandInfo;
import com.heypixel.heypixelmod.obsoverlay.utils.ChatUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.HotSwapManager;

import java.io.File;

@CommandInfo(
   name = "inject",
   description = "从外部文件注入类",
   aliases = {"inj", "injection"}
)
public class CommandInject extends Command {
    @Override
    public void onCommand(String[] args) {
        if (args.length < 2) {
            ChatUtils.addChatMessage("用法: /inject <类名> <类文件路径>");
            ChatUtils.addChatMessage("例如: /inject com.example.MyClass ./mods/classes/MyClass.class");
            return;
        }

        try {
            String className = args[0];
            String classFilePath = args[1];
            
            File classFile = new File(classFilePath);
            if (!classFile.exists()) {
                ChatUtils.addChatMessage("类文件不存在: " + classFilePath);
                return;
            }
            
            // 初始化热重载系统
            HotSwapManager.getInstance().initialize();
            
            if (!HotSwapManager.getInstance().isHotSwapAvailable()) {
                ChatUtils.addChatMessage("热注入功能不可用，请检查配置。");
                return;
            }
            
            // 尝试重新定义类
            boolean success = HotSwapManager.getInstance().redefineClass(className, classFilePath);
            if (success) {
                ChatUtils.addChatMessage("成功注入类: " + className);
            } else {
                // 如果重新定义失败，尝试加载新类
                try {
                    Class<?> clazz = HotSwapManager.getInstance().loadClass(className, classFilePath);
                    ChatUtils.addChatMessage("成功加载新类: " + className);
                } catch (Exception loadException) {
                    ChatUtils.addChatMessage("注入类失败: " + className + ", 错误: " + loadException.getMessage());
                }
            }
        } catch (Exception e) {
            ChatUtils.addChatMessage("注入过程中发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public String[] onTab(String[] args) {
        // 简单的自动补全
        if (args.length == 1) {
            return HotSwapManager.getInstance()
                .getClassNames()
                .stream()
                .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                .toArray(String[]::new);
        }
        return new String[0];
    }
}