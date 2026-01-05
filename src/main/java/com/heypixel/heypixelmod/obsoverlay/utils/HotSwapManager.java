package com.heypixel.heypixelmod.obsoverlay.utils;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hotswap.agent.HotswapAgent;
import java.io.*;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;

/**
 * 热注入管理器类，负责动态加载和注入代码
 * 支持在运行时动态重新加载类文件以实现热重载功能
 */
public class HotSwapManager {
    private static final Logger logger = LogManager.getLogger(HotSwapManager.class);
    private static HotSwapManager instance;
    private final Map<String, Class<?>> loadedClasses = new HashMap<>();
    private final Map<String, ReloadableClassLoader> classLoaders = new HashMap<>();
    private Instrumentation instrumentation;
    private boolean initialized = false;
    
    // 类文件目录，用于动态加载
    private String classDirectory = "./classes";
    
    private HotSwapManager() {
        // 私有构造函数，确保单例模式
    }
    
    /**
     * 获取HotSwapManager的单例实例
     * @return HotSwapManager实例
     */
    public static HotSwapManager getInstance() {
        if (instance == null) {
            instance = new HotSwapManager();
        }
        return instance;
    }
    
    /**
     * 初始化热重载代理
     */
    public void initialize() {
        try {
            // 尝试通过反射获取已存在的Instrumentation实例
            initializeInstrumentation();
            
            // 初始化HotswapAgent
            try {
                HotswapAgent hotswapAgent = new HotswapAgent();
                logger.info("[HotSwapManager] HotswapAgent 初始化成功");
            } catch (Exception e) {
                logger.warn("[HotSwapManager] HotswapAgent 初始化失败: " + e.getMessage());
            }
            
            logger.info("[HotSwapManager] 热重载系统初始化完成");
            this.initialized = true;
        } catch (Exception e) {
            logger.error("[HotSwapManager] 热重载系统初始化失败: " + e.getMessage());
            this.initialized = false;
        }
    }
    
    /**
     * 设置类文件目录
     * @param directory 类文件目录
     */
    public void setClassDirectory(String directory) {
        this.classDirectory = directory;
        logger.info("[HotSwapManager] 设置类文件目录为: " + directory);
    }
    
    /**
     * 尝试通过反射获取Instrumentation实例
     */
    private void initializeInstrumentation() {
        try {
            // 尝试获取系统Instrumentation实例
            Class<?> vmClass = Class.forName("sun.instrument.InstrumentationImpl");
            Field[] fields = ClassLoader.getSystemClassLoader().getClass().getDeclaredFields();
            for (Field field : fields) {
                if (field.getType().isAssignableFrom(vmClass)) {
                    field.setAccessible(true);
                    this.instrumentation = (Instrumentation) field.get(ClassLoader.getSystemClassLoader());
                    logger.info("[HotSwapManager] 通过反射获取Instrumentation实例成功");
                    return;
                }
            }
        } catch (Exception e) {
            logger.warn("[HotSwapManager] 无法通过反射获取Instrumentation实例: " + e.getMessage());
        }
        
        // 如果通过反射无法获取，检查是否通过agent方式附加
        try {
            // 检查是否已经有Instrumentation实例
            Class<?> instClass = Class.forName("java.lang.instrument.Instrumentation");
            logger.info("[HotSwapManager] Instrumentation类存在");
        } catch (Exception e) {
            logger.warn("[HotSwapManager] Instrumentation类不存在: " + e.getMessage());
        }
    }
    
    /**
     * 设置Instrumentation实例
     * @param instrumentation Instrumentation实例
     */
    public void setInstrumentation(Instrumentation instrumentation) {
        this.instrumentation = instrumentation;
        logger.info("[HotSwapManager] Instrumentation实例已设置");
    }
    
    /**
     * 动态加载类文件
     * @param className 类名
     * @param classFile 类文件路径
     * @return 加载的类
     * @throws IOException IO异常
     * @throws ClassNotFoundException 类未找到异常
     */
    public Class<?> loadClass(String className, String classFile) throws IOException, ClassNotFoundException {
        File file = new File(classFile);
        URL url = file.toURI().toURL();
        ReloadableClassLoader classLoader = new ReloadableClassLoader(new URL[]{url}, this.getClass().getClassLoader());
        Class<?> clazz = classLoader.loadClass(className);
        loadedClasses.put(className, clazz);
        classLoaders.put(className, classLoader);
        logger.info("[HotSwapManager] 成功加载类: " + className);
        return clazz;
    }
    
    /**
     * 从预设目录加载类
     * @param className 类名
     * @return 加载的类
     * @throws IOException IO异常
     * @throws ClassNotFoundException 类未找到异常
     */
    public Class<?> loadClassFromDirectory(String className) throws IOException, ClassNotFoundException {
        String classFilePath = classDirectory + "/" + className.replace('.', '/') + ".class";
        return loadClass(className, classFilePath);
    }
    
    /**
     * 重新定义已加载的类（热替换）
     * @param className 类名
     * @param classFile 新的类文件路径
     * @return 是否成功
     */
    public boolean redefineClass(String className, String classFile) {
        try {
            // 检查类文件是否存在
            File file = new File(classFile);
            if (!file.exists()) {
                logger.warn("[HotSwapManager] 类文件不存在: " + classFile);
                return false;
            }
            
            // 如果有Instrumentation实例，使用标准方式
            if (instrumentation != null && instrumentation.isRedefineClassesSupported()) {
                // 实际的热替换逻辑
                logger.info("[HotSwapManager] 使用Instrumentation重新定义类: " + className);
                return performRedefineWithInstrumentation(className, classFile);
            } else {
                // 使用自定义类加载器方式替换类定义
                logger.info("[HotSwapManager] 使用自定义类加载器重新定义类: " + className);
                return redefineClassWithCustomLoader(className, classFile);
            }
        } catch (Exception e) {
            logger.error("[HotSwapManager] 重新定义类失败: " + className + ", 错误: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 使用Instrumentation重新定义类
     */
    private boolean performRedefineWithInstrumentation(String className, String classFile) {
        try {
            Class<?> clazz = loadedClasses.get(className);
            if (clazz == null) {
                logger.warn("[HotSwapManager] 类未加载，尝试加载: " + className);
                // 尝试加载类
                clazz = loadClassFromDirectory(className);
                if (clazz == null) {
                    logger.warn("[HotSwapManager] 无法加载类: " + className);
                    return false;
                }
            }
            
            byte[] classBytes = Files.readAllBytes(Paths.get(classFile));
            ClassDefinition classDefinition = new ClassDefinition(clazz, classBytes);
            instrumentation.redefineClasses(classDefinition);
            logger.info("[HotSwapManager] 成功使用Instrumentation重新定义类: " + className);
            return true;
        } catch (Exception e) {
            logger.error("[HotSwapManager] 使用Instrumentation重新定义类失败: " + className + ", 错误: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 使用自定义类加载器重新定义类
     */
    private boolean redefineClassWithCustomLoader(String className, String classFile) {
        try {
            ReloadableClassLoader oldLoader = classLoaders.get(className);
            if (oldLoader == null) {
                logger.warn("[HotSwapManager] 未找到类对应的类加载器: " + className + "，尝试创建新的");
                // 如果没有旧的加载器，创建一个新的
            }
            
            // 创建新的类加载器
            File file = new File(classFile);
            URL url = file.toURI().toURL();
            ReloadableClassLoader newLoader = new ReloadableClassLoader(new URL[]{url}, this.getClass().getClassLoader());
            
            // 加载新类
            Class<?> newClass = newLoader.loadClass(className);
            
            // 更新记录
            loadedClasses.put(className, newClass);
            classLoaders.put(className, newLoader);
            
            logger.info("[HotSwapManager] 成功使用自定义类加载器重新定义类: " + className);
            return true;
        } catch (Exception e) {
            logger.error("[HotSwapManager] 使用自定义类加载器重新定义类失败: " + className + ", 错误: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 通过反射方式重新定义类
     * @param className 类名
     * @param classFile 类文件路径
     * @return 是否成功
     */
    private boolean redefineClassViaReflection(String className, String classFile) {
        try {
            // 这里可以实现更复杂的反射逻辑来替换类定义
            // 例如修改类的字段、方法等
            Class<?> targetClass = Class.forName(className);
            logger.info("[HotSwapManager] 通过反射方式处理类: " + className);
            return true;
        } catch (Exception e) {
            logger.error("[HotSwapManager] 反射方式重新定义类失败: " + className + ", 错误: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 获取已加载的类
     * @param className 类名
     * @return 类对象，如果未找到返回null
     */
    public Class<?> getLoadedClass(String className) {
        return loadedClasses.get(className);
    }
    
    /**
     * 获取所有已加载的类名
     * @return 类名集合
     */
    public Set<String> getClassNames() {
        return Collections.unmodifiableSet(loadedClasses.keySet());
    }
    
    /**
     * 卸载类
     * @param className 类名
     * @return 是否成功卸载
     */
    public boolean unloadClass(String className) {
        boolean removed = loadedClasses.remove(className) != null;
        classLoaders.remove(className);
        logger.info("[HotSwapManager] 卸载类: " + className + ", 结果: " + removed);
        return removed;
    }
    
    /**
     * 重新加载所有已加载的类
     */
    public void reloadAllClasses() {
        logger.info("[HotSwapManager] 尝试重新加载所有类，共 " + loadedClasses.size() + " 个类");
        
        // 首先检查类目录
        File classDir = new File(classDirectory);
        if (!classDir.exists()) {
            logger.warn("[HotSwapManager] 类目录不存在: " + classDirectory);
            return;
        }
        
        if (!classDir.isDirectory()) {
            logger.warn("[HotSwapManager] 类路径不是目录: " + classDirectory);
            return;
        }
        
        // 使用数组来包装计数器，使其可以在lambda表达式中修改
        final int[] successCount = {0};
        final int[] totalCount = {0};
        
        // 遍历类目录中的所有类文件
        try {
            Path rootPath = Paths.get(classDirectory);
            Files.walk(rootPath)
                .filter(path -> path.toString().endsWith(".class"))
                .forEach(path -> {
                    try {
                        String relativePath = rootPath.relativize(path).toString();
                        String className = relativePath.replace('/', '.').replace('\\', '.')
                            .substring(0, relativePath.length() - 6); // 移除 .class 后缀
                        
                        String classFilePath = path.toString();
                        synchronized (totalCount) {
                            totalCount[0]++;
                        }
                        if (redefineClass(className, classFilePath)) {
                            synchronized (successCount) {
                                successCount[0]++;
                            }
                        }
                    } catch (Exception e) {
                        logger.error("[HotSwapManager] 处理类文件失败: " + path + ", 错误: " + e.getMessage());
                    }
                });
        } catch (Exception e) {
            logger.error("[HotSwapManager] 遍历类目录失败: " + e.getMessage());
        }
        
        logger.info("[HotSwapManager] 重新加载完成，成功: " + successCount[0] + "/" + totalCount[0] + " 个类");
    }
    
    /**
     * 检查热重载功能是否可用
     * @return 是否可用
     */
    public boolean isHotSwapAvailable() {
        // 检查HotswapAgent类是否存在
        try {
            Class.forName("org.hotswap.agent.HotswapAgent");
            logger.info("[HotSwapManager] HotswapAgent 类存在");
        } catch (ClassNotFoundException e) {
            logger.warn("[HotSwapManager] HotswapAgent 类未找到");
            // 即使没有HotswapAgent，我们也可能通过其他方式实现部分热重载功能
        }
        
        // 检查Instrumentation是否可用
        if (instrumentation != null) {
            logger.info("[HotSwapManager] Instrumentation 可用，支持类重定义: " + instrumentation.isRedefineClassesSupported());
            return instrumentation.isRedefineClassesSupported();
        }
        
        // 检查是否已初始化
        if (!initialized) {
            logger.warn("[HotSwapManager] 热重载系统未初始化");
            return false;
        }
        
        // 即使没有Instrumentation，我们也可能通过反射实现部分功能
        logger.info("[HotSwapManager] 部分热重载功能可能可用");
        return true;
    }
    
    /**
     * 动态添加URL到系统类加载器
     * @param url 要添加的URL
     */
    public void addURLToSystemClassLoader(URL url) {
        try {
            URLClassLoader classLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
            Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
            method.setAccessible(true);
            method.invoke(classLoader, url);
            logger.info("[HotSwapManager] 成功添加URL到系统类加载器: " + url.toString());
        } catch (Exception e) {
            logger.error("[HotSwapManager] 添加URL到系统类加载器失败: " + e.getMessage());
        }
    }
    
    /**
     * 可重载的类加载器
     */
    private static class ReloadableClassLoader extends URLClassLoader {
        public ReloadableClassLoader(URL[] urls, ClassLoader parent) {
            super(urls, parent);
        }
        
        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            // 先尝试从父类加载器加载
            try {
                return getParent().loadClass(name);
            } catch (ClassNotFoundException e) {
                // 父类加载器无法加载，从当前URL加载
                return findClass(name);
            }
        }
    }
}