package com.heypixel.heypixelmod.obsoverlay.utils;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

public class FinalModAgent {
    public static void agentmain(String agentArgs, Instrumentation inst) {
        System.out.println("FinalModAgent loaded successfully!");
        System.out.println("Agent args: " + agentArgs);
        System.out.println("Instrumentation: " + inst);
        
        try {
            // 查找模组JAR文件 (使用绝对路径)
            String modJarPath = "G:/yuanma/new naven/naven3/build/libs/Naven-Modern-1337.jar";
            File modJar = new File(modJarPath);
            
            if (modJar.exists()) {
                System.out.println("Found mod jar: " + modJar.getAbsolutePath());
                
                // 使用URLClassLoader加载模组
                URL modUrl = modJar.toURI().toURL();
                URLClassLoader classLoader = new URLClassLoader(new URL[]{modUrl}, 
                    FinalModAgent.class.getClassLoader());
                
                // 加载并初始化模组
                System.out.println("Loading Naven class...");
                Class<?> navenClass = classLoader.loadClass("com.heypixel.heypixelmod.obsoverlay.Naven");
                System.out.println("Loaded Naven class: " + navenClass.getName());
                
                // 获取实例
                System.out.println("Getting Naven instance...");
                Method getInstanceMethod = navenClass.getMethod("getInstance");
                Object navenInstance = getInstanceMethod.invoke(null);
                System.out.println("Got Naven instance: " + navenInstance);
                
                // 检查实例是否为null
                if (navenInstance == null) {
                    System.err.println("Naven instance is null!");
                    return;
                }
                
                // 初始化模组
                System.out.println("Initializing Naven mod...");
                try {
                    Method initMethod = navenClass.getMethod("init");
                    initMethod.invoke(navenInstance);
                    System.out.println("Naven mod initialized successfully!");
                } catch (NoSuchMethodException e) {
                    System.out.println("No init method found, trying alternative initialization...");
                    // 尝试调用modRegister方法
                    try {
                        Method modRegisterMethod = navenClass.getMethod("modRegister");
                        modRegisterMethod.invoke(null);
                        System.out.println("Naven mod registered successfully!");
                    } catch (NoSuchMethodException e2) {
                        System.out.println("No modRegister method found either. Mod may initialize automatically.");
                    }
                }
                
            } else {
                System.err.println("Mod jar not found at: " + modJar.getAbsolutePath());
                // 列出目录内容帮助调试
                File buildDir = new File("G:/yuanma/new naven/naven3/build/libs");
                if (buildDir.exists() && buildDir.isDirectory()) {
                    System.out.println("Files in build/libs directory:");
                    for (File file : buildDir.listFiles()) {
                        System.out.println("  " + file.getName());
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error loading mod: " + e.getMessage());
            e.printStackTrace();
        }
    }
}