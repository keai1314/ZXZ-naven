import java.io.File;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

public class DirectModLoaderAgent {
    public static void agentmain(String agentArgs, Instrumentation inst) {
        System.out.println("DirectModLoaderAgent loaded!");
        System.out.println("Agent args: " + agentArgs);
        
        try {
            // 直接从文件系统加载模组jar
            String modJarPath = "G:/yuanma/new naven/naven3/build/libs/Naven-Modern-1337.jar";
            File modJarFile = new File(modJarPath);
            
            if (!modJarFile.exists()) {
                System.err.println("Mod jar file not found: " + modJarPath);
                return;
            }
            
            System.out.println("Loading mod from: " + modJarFile.getAbsolutePath());
            
            // 创建URLClassLoader来加载模组
            URL modUrl = modJarFile.toURI().toURL();
            URLClassLoader modClassLoader = new URLClassLoader(new URL[]{modUrl}, 
                DirectModLoaderAgent.class.getClassLoader());
            
            // 使用新的类加载器加载Naven类
            Class<?> navenClass = modClassLoader.loadClass("com.heypixel.heypixelmod.obsoverlay.Naven");
            System.out.println("Loaded Naven class: " + navenClass.getName());
            
            // 获取实例
            Method getInstanceMethod = navenClass.getMethod("getInstance");
            Object navenInstance = getInstanceMethod.invoke(null);
            
            if (navenInstance == null) {
                System.out.println("Naven instance is null, calling modRegister...");
                // 调用modRegister创建实例
                Method modRegisterMethod = navenClass.getMethod("modRegister");
                modRegisterMethod.invoke(null);
                
                // 再次检查实例
                navenInstance = getInstanceMethod.invoke(null);
                System.out.println("Naven instance after modRegister: " + navenInstance);
            } else {
                System.out.println("Naven instance found: " + navenInstance);
            }
            
            if (navenInstance != null) {
                // 初始化模组
                try {
                    Method initMethod = navenClass.getMethod("init");
                    initMethod.invoke(navenInstance);
                    System.out.println("Naven mod initialized successfully!");
                } catch (NoSuchMethodException e) {
                    System.out.println("No init method found, mod may be initialized already");
                }
                
                // 激活ClickGUI模块
                activateClickGUIModule(navenInstance, modClassLoader);
            } else {
                System.err.println("Failed to create Naven instance");
            }
            
        } catch (Exception e) {
            System.err.println("Error in DirectModLoaderAgent: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void activateClickGUIModule(Object navenInstance, ClassLoader modClassLoader) {
        try {
            // 获取Naven类
            Class<?> navenClass = navenInstance.getClass();
            
            // 获取moduleManager字段
            java.lang.reflect.Field moduleManagerField = navenClass.getDeclaredField("moduleManager");
            moduleManagerField.setAccessible(true);
            Object moduleManager = moduleManagerField.get(navenInstance);
            
            if (moduleManager != null) {
                System.out.println("ModuleManager found: " + moduleManager);
                
                // 获取ClickGUI模块
                Class<?> moduleManagerClass = moduleManager.getClass();
                Method getModuleMethod = moduleManagerClass.getMethod("getModule", Class.class);
                
                Class<?> clickGUIClass = modClassLoader.loadClass("com.heypixel.heypixelmod.obsoverlay.modules.impl.render.ClickGUIModule");
                Object clickGUIModule = getModuleMethod.invoke(moduleManager, clickGUIClass);
                
                if (clickGUIModule != null) {
                    System.out.println("ClickGUI module found: " + clickGUIModule);
                    
                    // 启用模块
                    Method setEnabledMethod = clickGUIModule.getClass().getMethod("setEnabled", boolean.class);
                    setEnabledMethod.invoke(clickGUIModule, true);
                    System.out.println("ClickGUI module enabled!");
                    
                    // 尝试显示GUI
                    try {
                        Method setDisplayMethod = clickGUIModule.getClass().getMethod("setDisplay", boolean.class);
                        setDisplayMethod.invoke(clickGUIModule, true);
                        System.out.println("ClickGUI display set to true!");
                    } catch (NoSuchMethodException e) {
                        System.out.println("setDisplay method not found");
                        
                        // 尝试调用onEnable方法
                        try {
                            Method onEnableMethod = clickGUIModule.getClass().getMethod("onEnable");
                            onEnableMethod.invoke(clickGUIModule);
                            System.out.println("ClickGUI onEnable called!");
                        } catch (NoSuchMethodException e2) {
                            System.out.println("onEnable method not found either");
                        }
                    }
                } else {
                    System.out.println("ClickGUI module is null");
                }
            } else {
                System.out.println("ModuleManager is null");
            }
        } catch (Exception e) {
            System.err.println("Error activating ClickGUI module: " + e.getMessage());
            e.printStackTrace();
        }
    }
}