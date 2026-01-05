package com.heypixel.heypixelmod.obsoverlay.utils;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * 简化版热重载管理器
 */
public class SimpleHotSwapManager {
    private static SimpleHotSwapManager instance;
    private Instrumentation instrumentation;
    private String classDirectory = "./mods/classes";
    private final Map<String, Class<?>> loadedClasses = new HashMap<>();
    
    private SimpleHotSwapManager() {
    }
    
    public static SimpleHotSwapManager getInstance() {
        if (instance == null) {
            instance = new SimpleHotSwapManager();
        }
        return instance;
    }
    
    public void setInstrumentation(Instrumentation instrumentation) {
        this.instrumentation = instrumentation;
    }
    
    public void setClassDirectory(String directory) {
        this.classDirectory = directory;
    }
    
    public void reloadAllClasses() {
        System.out.println("[SimpleHotSwapManager] 尝试重新加载所有类");
        
        File classDir = new File(classDirectory);
        if (!classDir.exists() || !classDir.isDirectory()) {
            System.err.println("[SimpleHotSwapManager] 类目录不存在或不是目录: " + classDirectory);
            return;
        }
        
        try {
            Path rootPath = Paths.get(classDirectory);
            Files.walk(rootPath)
                .filter(path -> path.toString().endsWith(".class"))
                .forEach(this::processClassFile);
        } catch (Exception e) {
            System.err.println("[SimpleHotSwapManager] 遍历类目录失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void processClassFile(Path classFile) {
        try {
            Path rootPath = Paths.get(classDirectory);
            String relativePath = rootPath.relativize(classFile).toString();
            String className = relativePath.replace('/', '.').replace('\\', '.')
                .substring(0, relativePath.length() - 6);
            
            System.out.println("[SimpleHotSwapManager] 处理类文件: " + className);
            
            if (instrumentation != null && instrumentation.isRedefineClassesSupported()) {
                try {
                    Class<?> clazz = Class.forName(className);
                    if (clazz != null) {
                        byte[] classBytes = Files.readAllBytes(classFile);
                        java.lang.instrument.ClassDefinition classDefinition = 
                            new java.lang.instrument.ClassDefinition(clazz, classBytes);
                        instrumentation.redefineClasses(classDefinition);
                        System.out.println("[SimpleHotSwapManager] 成功重定义类: " + className);
                    }
                } catch (Exception e) {
                    System.err.println("[SimpleHotSwapManager] 重定义类失败: " + className + ", 错误: " + e.getMessage());
                }
            } else {
                System.out.println("[SimpleHotSwapManager] Instrumentation不可用，跳过类重定义: " + className);
            }
        } catch (Exception e) {
            System.err.println("[SimpleHotSwapManager] 处理类文件失败: " + classFile + ", 错误: " + e.getMessage());
        }
    }
}