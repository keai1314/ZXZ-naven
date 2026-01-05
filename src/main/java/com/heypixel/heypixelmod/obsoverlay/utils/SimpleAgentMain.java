package com.heypixel.heypixelmod.obsoverlay.utils;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * 简化版Agent入口类 - 修改为直接加载模组
 */
public class SimpleAgentMain {
    
    public static void agentmain(String agentArgs, Instrumentation inst) {
        System.out.println("SimpleAgent已加载，参数: " + agentArgs);
        System.out.println("Instrumentation: " + inst);
        
        try {
            // 获取当前工作目录
            String currentDir = System.getProperty("user.dir");
            System.out.println("Current directory: " + currentDir);
            
            // 查找模组JAR文件
            File modJar = findModJar();
            
            if (modJar != null && modJar.exists()) {
                System.out.println("Found mod jar: " + modJar.getAbsolutePath());
                
                // 使用URLClassLoader加载模组
                URL modUrl = modJar.toURI().toURL();
                URLClassLoader classLoader = new URLClassLoader(new URL[]{modUrl}, 
                    SimpleAgentMain.class.getClassLoader());
                
                // 尝试加载并初始化模组
                initializeMod(classLoader);
                
            } else {
                System.err.println("Mod jar not found!");
                debugJarLocations();
            }
            
        } catch (Exception e) {
            System.err.println("Error in agent: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static File findModJar() {
        // 尝试多个可能的位置
        String[] possiblePaths = {
            "build/libs/Naven-Modern-1337.jar",
            "G:/yuanma/new naven/naven3/build/libs/Naven-Modern-1337.jar",
            "../build/libs/Naven-Modern-1337.jar",
            "./Naven-Modern-1337.jar"
        };
        
        for (String path : possiblePaths) {
            File file = new File(path);
            if (file.exists()) {
                return file;
            }
        }
        
        return null;
    }
    
    private static void initializeMod(URLClassLoader classLoader) {
        try {
            // 尝试加载模组主类
            Class<?> mainClass = classLoader.loadClass("com.heypixel.heypixelmod.obsoverlay.Naven");
            System.out.println("Loaded main class: " + mainClass.getName());
            
            // 尝试获取实例并初始化
            try {
                // 查找getInstance方法
                Method getInstanceMethod = mainClass.getMethod("getInstance");
                Object instance = getInstanceMethod.invoke(null);
                System.out.println("Got mod instance: " + instance);
                
                // 如果有初始化方法，尝试调用
                try {
                    Method initMethod = mainClass.getMethod("init");
                    initMethod.invoke(instance);
                    System.out.println("Mod initialized successfully!");
                } catch (NoSuchMethodException e) {
                    System.out.println("No init method found, mod may initialize automatically");
                }
                
            } catch (NoSuchMethodException e) {
                System.out.println("No getInstance method found, trying direct instantiation");
                // 尝试直接创建实例（使用推荐的方式替代过时的newInstance()）
                try {
                    Object instance = mainClass.getDeclaredConstructor().newInstance();
                    System.out.println("Created mod instance: " + instance);
                } catch (Exception ex) {
                    System.out.println("Could not create instance: " + ex.getMessage());
                }
            }
            
        } catch (ClassNotFoundException e) {
            System.err.println("Could not load main class: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error initializing mod: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void debugJarLocations() {
        System.out.println("Searching for mod jar in common locations:");
        
        String currentDir = System.getProperty("user.dir");
        File currentDirFile = new File(currentDir);
        listDirectoryContents(currentDirFile);
        
        File projectDir = new File("G:/yuanma/new naven/naven3");
        if (projectDir.exists()) {
            listDirectoryContents(projectDir);
            
            File buildDir = new File(projectDir, "build/libs");
            if (buildDir.exists()) {
                listDirectoryContents(buildDir);
            }
        }
    }
    
    private static void listDirectoryContents(File dir) {
        System.out.println("Contents of directory: " + dir.getAbsolutePath());
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    System.out.println("  " + file.getName() + (file.isDirectory() ? "/" : ""));
                }
            }
        } else {
            System.out.println("  Directory does not exist or is not a directory");
        }
    }
}