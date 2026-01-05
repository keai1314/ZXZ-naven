package com.heypixel.heypixelmod.obsoverlay.commands.impl;

import com.heypixel.heypixelmod.obsoverlay.commands.Command;
import com.heypixel.heypixelmod.obsoverlay.commands.CommandInfo;
import com.heypixel.heypixelmod.obsoverlay.utils.ChatUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.HotSwapManager;

@CommandInfo(
   name = "hotreload",
   description = "触发热重载功能",
   aliases = {"hr", "hotswap"}
)
public class CommandHotReload extends Command {
    @Override
    public void onCommand(String[] args) {
        try {
            // 尝试初始化热重载系统
            HotSwapManager.getInstance().initialize();
            
            if (!HotSwapManager.getInstance().isHotSwapAvailable()) {
                ChatUtils.addChatMessage("热重载功能不可用，请检查配置。");
                ChatUtils.addChatMessage("请确保已正确配置DCEVM和HotswapAgent。");
                return;
            }

            if (args.length > 0) {
                // 如果提供了参数，尝试重新加载指定的类
                String className = args[0];
                if (args.length > 1) {
                    String classFile = args[1];
                    boolean success = HotSwapManager.getInstance().redefineClass(className, classFile);
                    if (success) {
                        ChatUtils.addChatMessage("类 " + className + " 热重载成功。");
                    } else {
                        ChatUtils.addChatMessage("类 " + className + " 热重载失败。");
                    }
                } else {
                    // 仅提供了类名，尝试从已加载的类中重新加载
                    // 设置默认类文件目录
                    HotSwapManager.getInstance().setClassDirectory("./mods/classes");
                    
                    Class<?> clazz = HotSwapManager.getInstance().getLoadedClass(className);
                    if (clazz != null) {
                        // 从预设目录加载新的类文件
                        try {
                            boolean success = HotSwapManager.getInstance().redefineClass(className, 
                                "./mods/classes/" + className.replace('.', '/') + ".class");
                            if (success) {
                                ChatUtils.addChatMessage("类 " + className + " 热重载成功。");
                            } else {
                                ChatUtils.addChatMessage("类 " + className + " 热重载失败。");
                            }
                        } catch (Exception e) {
                            ChatUtils.addChatMessage("类 " + className + " 热重载异常: " + e.getMessage());
                        }
                    } else {
                        // 尝试从目录加载新类
                        try {
                            HotSwapManager.getInstance().setClassDirectory("./mods/classes");
                            Class<?> loadedClass = HotSwapManager.getInstance().loadClassFromDirectory(className);
                            ChatUtils.addChatMessage("成功加载新类: " + className);
                        } catch (Exception e) {
                            ChatUtils.addChatMessage("未找到已加载的类: " + className + "，加载新类也失败: " + e.getMessage());
                        }
                    }
                }
            } else {
                // 没有参数，重新加载所有类
                HotSwapManager.getInstance().setClassDirectory("./mods/classes");
                HotSwapManager.getInstance().reloadAllClasses();
                ChatUtils.addChatMessage("已触发热重载，正在重新加载所有类...");
            }
        } catch (Exception e) {
            ChatUtils.addChatMessage("热重载过程中发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public String[] onTab(String[] args) {
        // 提供类名的自动补全
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