package com.heypixel.heypixelmod.obsoverlay.utils;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Agent入口类，用于动态注入
 */
public class AgentMain {
    
    /**
     * Agent入口方法
     * @param agentArgs agent参数
     * @param inst Instrumentation实例
     */
    public static void agentmain(String agentArgs, Instrumentation inst) {
        System.out.println("Agent已加载，参数: " + agentArgs);
        System.out.println("Instrumentation: " + inst);
        
        try {
            // 初始化热重载管理器并设置Instrumentation实例
            HotSwapManager hotSwapManager = HotSwapManager.getInstance();
            hotSwapManager.setInstrumentation(inst);
            hotSwapManager.initialize();
            
            // 检查热重载是否可用
            if (!hotSwapManager.isHotSwapAvailable()) {
                System.err.println("热重载功能不可用，请检查配置。");
                // 即使热重载不可用，我们也继续执行，因为可能有部分功能可用
            }
            
            // 设置类目录并重新加载所有类
            String classDir = "./mods/classes";
            hotSwapManager.setClassDirectory(classDir);
            
            // 检查类目录是否存在
            File dir = new File(classDir);
            if (!dir.exists()) {
                System.out.println("警告: 类目录不存在: " + classDir + "，将创建该目录");
                dir.mkdirs();
            }
            
            if (!dir.isDirectory()) {
                System.err.println("错误: 类路径不是目录: " + classDir);
                return;
            }
            
            System.out.println("类目录存在: " + dir.getAbsolutePath());
            
            // 启动一个线程来执行热重载
            Thread reloadThread = new Thread(() -> {
                try {
                    System.out.println("等待3秒后开始执行热重载...");
                    Thread.sleep(3000); // 等待3秒确保游戏完全加载
                    System.out.println("开始执行热重载...");
                    
                    // 检查是否有类文件需要加载
                    Path classPath = Paths.get(classDir);
                    if (Files.exists(classPath) && Files.isDirectory(classPath)) {
                        System.out.println("类目录内容:");
                        Files.walk(classPath, 10)
                            .filter(Files::isRegularFile)
                            .filter(path -> path.toString().endsWith(".class"))
                            .forEach(path -> System.out.println("  " + path));
                    } else {
                        System.out.println("类目录不存在或不是目录: " + classPath);
                    }
                    
                    hotSwapManager.reloadAllClasses();
                    System.out.println("热重载执行完成");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.err.println("热重载线程被中断");
                } catch (Exception e) {
                    System.err.println("热重载执行失败: " + e.getMessage());
                    e.printStackTrace();
                }
            });
            
            reloadThread.setDaemon(true);
            reloadThread.start();
            System.out.println("自定义任务线程已启动");
            
        } catch (Exception e) {
            System.err.println("Agent初始化失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}