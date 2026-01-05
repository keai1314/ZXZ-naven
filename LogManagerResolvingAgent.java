import java.io.File;
import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.net.URLClassLoader;
import java.lang.reflect.Field;

public class LogManagerResolvingAgent {
    public static void agentmain(String agentArgs, Instrumentation inst) {
        System.out.println("LogManagerResolvingAgent loaded!");
        System.out.println("Agent args: " + agentArgs);
        System.out.println("Instrumentation: " + inst);
        
        try {
            loadModWithLogManagerResolution(inst);
        } catch (Exception e) {
            System.err.println("Error loading mod with LogManager resolution: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void loadModWithLogManagerResolution(Instrumentation inst) throws Exception {
        System.out.println("Attempting to load mod with LogManager resolution...");
        
        // 直接加载Naven模组jar
        String modJarPath = "G:/yuanma/new naven/naven3/build/libs/Naven-Modern-1337.jar";
        File modJar = new File(modJarPath);
        
        System.out.println("Looking for mod jar at: " + modJar.getAbsolutePath());
        
        if (modJar.exists()) {
            System.out.println("Found mod jar: " + modJar.getAbsolutePath());
            System.out.println("Mod jar size: " + modJar.length() + " bytes");
            
            // 获取系统类加载器
            ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
            System.out.println("System classloader: " + systemClassLoader);
            
            // 尝试直接在系统类加载器中查找Naven类
            try {
                Class<?> navenClass = systemClassLoader.loadClass("com.heypixel.heypixelmod.obsoverlay.Naven");
                System.out.println("Found Naven class in system classloader: " + navenClass);
                
                // 检查实例
                java.lang.reflect.Method getInstanceMethod = navenClass.getMethod("getInstance");
                Object existingInstance = getInstanceMethod.invoke(null);
                System.out.println("Existing Naven instance: " + existingInstance);
                
                if (existingInstance != null) {
                    System.out.println("Naven already initialized, trying to activate features...");
                    activateFeatures(existingInstance);
                } else {
                    // 调用modRegister方法
                    System.out.println("Calling modRegister method...");
                    java.lang.reflect.Method modRegisterMethod = navenClass.getMethod("modRegister");
                    modRegisterMethod.invoke(null);
                    System.out.println("Naven mod registered successfully!");
                    
                    // 再次检查实例
                    Object newInstance = getInstanceMethod.invoke(null);
                    System.out.println("New Naven instance created: " + newInstance);
                    
                    if (newInstance != null) {
                        activateFeatures(newInstance);
                    }
                }
            } catch (ClassNotFoundException e) {
                System.out.println("Naven class not found in system classloader, trying to inject JAR into classpath...");
                
                // 如果找不到，尝试使用Instrumentation将JAR添加到系统类路径
                try {
                    inst.appendToSystemClassLoaderSearch(new java.util.jar.JarFile(modJar));
                    System.out.println("Added mod JAR to system classpath");
                    
                    // 再次尝试加载Naven类
                    Class<?> navenClass = systemClassLoader.loadClass("com.heypixel.heypixelmod.obsoverlay.Naven");
                    System.out.println("Loaded Naven class after adding to system classpath: " + navenClass);
                    
                    // 调用modRegister方法
                    System.out.println("Calling modRegister method...");
                    java.lang.reflect.Method modRegisterMethod = navenClass.getMethod("modRegister");
                    modRegisterMethod.invoke(null);
                    System.out.println("Naven mod registered successfully!");
                    
                    // 检查实例
                    java.lang.reflect.Method getInstanceMethod = navenClass.getMethod("getInstance");
                    Object instance = getInstanceMethod.invoke(null);
                    System.out.println("Naven instance created: " + instance);
                    
                    if (instance != null) {
                        activateFeatures(instance);
                    }
                } catch (Exception injectException) {
                    System.err.println("Error injecting JAR into system classpath: " + injectException.getMessage());
                    injectException.printStackTrace();
                }
            }
        } else {
            System.err.println("Mod jar not found at: " + modJar.getAbsolutePath());
        }
    }
    
    private static void activateFeatures(Object navenInstance) throws Exception {
        System.out.println("Activating features for Naven instance: " + navenInstance);
        
        Class<?> navenClass = navenInstance.getClass();
        
        // 尝试获取模块管理器并激活ClickGUI
        try {
            java.lang.reflect.Method getModuleManager = navenClass.getMethod("getModuleManager");
            Object moduleManager = getModuleManager.invoke(navenInstance);
            
            if (moduleManager != null) {
                System.out.println("Module manager found, trying to activate ClickGUI...");
                
                try {
                    Class<?> moduleManagerClass = moduleManager.getClass();
                    java.lang.reflect.Method getModule = moduleManagerClass.getMethod("getModule", Class.class);
                    
                    // 尝试获取ClickGUI模块
                    Class<?> clickGUIModuleClass = Class.forName("com.heypixel.heypixelmod.obsoverlay.modules.impl.render.ClickGUIModule");
                    Object clickGUIModule = getModule.invoke(moduleManager, clickGUIModuleClass);
                    
                    if (clickGUIModule != null) {
                        System.out.println("ClickGUI module found, trying to enable...");
                        
                        // 尝试启用模块
                        java.lang.reflect.Method setEnabled = clickGUIModule.getClass().getMethod("setEnabled", boolean.class);
                        setEnabled.invoke(clickGUIModule, true);
                        System.out.println("ClickGUI module enabled successfully!");
                        
                        // 尝试显示GUI
                        java.lang.reflect.Method setDisplay = clickGUIModule.getClass().getMethod("setDisplay", boolean.class);
                        setDisplay.invoke(clickGUIModule, true);
                        System.out.println("ClickGUI module display set to true!");
                    } else {
                        System.out.println("ClickGUI module not found");
                    }
                } catch (Exception moduleException) {
                    System.err.println("Error activating ClickGUI module: " + moduleException.getMessage());
                    moduleException.printStackTrace();
                }
            } else {
                System.out.println("Module manager is null");
            }
        } catch (Exception managerException) {
            System.err.println("Error accessing module manager: " + managerException.getMessage());
            managerException.printStackTrace();
        }
    }
}