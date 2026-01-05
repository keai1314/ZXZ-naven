import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.lang.reflect.Constructor;

public class ModActivatorAgent {
    public static void agentmain(String agentArgs, Instrumentation inst) {
        System.out.println("ModActivatorAgent loaded!");
        System.out.println("Agent args: " + agentArgs);
        System.out.println("Instrumentation: " + inst);
        
        try {
            activateExistingMod(inst);
        } catch (Exception e) {
            System.err.println("Error activating existing mod: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void activateExistingMod(Instrumentation inst) throws Exception {
        System.out.println("Attempting to activate existing mod...");
        
        // 尝试查找已经在JVM中的Naven类
        try {
            Class<?> navenClass = Class.forName("com.heypixel.heypixelmod.obsoverlay.Naven");
            System.out.println("Found Naven class: " + navenClass);
            
            // 获取实例
            java.lang.reflect.Method getInstanceMethod = navenClass.getMethod("getInstance");
            Object navenInstance = getInstanceMethod.invoke(null);
            System.out.println("Naven instance: " + navenInstance);
            
            if (navenInstance != null) {
                System.out.println("Naven is already initialized, activating features...");
                activateFeatures(navenInstance);
            } else {
                System.out.println("Naven is not initialized, trying to initialize...");
                // 尝试调用初始化方法
                try {
                    java.lang.reflect.Method modRegisterMethod = navenClass.getMethod("modRegister");
                    modRegisterMethod.invoke(null);
                    System.out.println("Naven registered successfully!");
                    
                    // 再次检查实例
                    navenInstance = getInstanceMethod.invoke(null);
                    if (navenInstance != null) {
                        activateFeatures(navenInstance);
                    }
                } catch (Exception e) {
                    System.err.println("Failed to register Naven: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        } catch (ClassNotFoundException e) {
            System.out.println("Naven class not found in JVM, mod may not be loaded");
            
            // 列出一些可能相关的类
            System.out.println("Searching for related classes...");
            Class<?>[] loadedClasses = inst.getAllLoadedClasses();
            for (Class<?> clazz : loadedClasses) {
                String className = clazz.getName();
                if (className.contains("heypixel") || className.contains("naven") || 
                    className.contains("obsoverlay")) {
                    System.out.println("  Found related class: " + className);
                }
            }
        }
    }
    
    private static void activateFeatures(Object navenInstance) throws Exception {
        System.out.println("Activating features for Naven instance: " + navenInstance);
        
        Class<?> navenClass = navenInstance.getClass();
        
        // 检查moduleManager字段
        try {
            Field moduleManagerField = navenClass.getDeclaredField("moduleManager");
            moduleManagerField.setAccessible(true);
            Object moduleManager = moduleManagerField.get(navenInstance);
            
            if (moduleManager != null) {
                System.out.println("Module manager found via reflection: " + moduleManager);
                activateClickGUIModule(moduleManager);
            } else {
                System.out.println("Module manager is null, trying to create a new instance...");
                // 尝试创建一个新的ModuleManager实例
                try {
                    Class<?> moduleManagerClass = Class.forName("com.heypixel.heypixelmod.obsoverlay.modules.ModuleManager");
                    Constructor<?> constructor = moduleManagerClass.getDeclaredConstructor();
                    constructor.setAccessible(true);
                    Object newModuleManager = constructor.newInstance();
                    
                    // 设置到Naven实例中
                    moduleManagerField.set(navenInstance, newModuleManager);
                    System.out.println("New ModuleManager created and set: " + newModuleManager);
                    
                    activateClickGUIModule(newModuleManager);
                } catch (Exception e) {
                    System.err.println("Failed to create and set new ModuleManager: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        } catch (NoSuchFieldException e) {
            System.out.println("moduleManager field not found in Naven class");
        } catch (Exception e) {
            System.err.println("Error accessing module manager via reflection: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void activateClickGUIModule(Object moduleManager) throws Exception {
        // 尝试激活ClickGUI模块
        try {
            Class<?> moduleManagerClass = moduleManager.getClass();
            java.lang.reflect.Method getModule = moduleManagerClass.getMethod("getModule", Class.class);
            
            Class<?> clickGUIModuleClass = Class.forName("com.heypixel.heypixelmod.obsoverlay.modules.impl.render.ClickGUIModule");
            Object clickGUIModule = getModule.invoke(moduleManager, clickGUIModuleClass);
            
            if (clickGUIModule != null) {
                System.out.println("ClickGUI module found: " + clickGUIModule);
                
                // 启用模块
                java.lang.reflect.Method setEnabled = clickGUIModule.getClass().getMethod("setEnabled", boolean.class);
                setEnabled.invoke(clickGUIModule, true);
                System.out.println("ClickGUI module enabled!");
                
                // 显示GUI
                try {
                    java.lang.reflect.Method setDisplay = clickGUIModule.getClass().getMethod("setDisplay", boolean.class);
                    setDisplay.invoke(clickGUIModule, true);
                    System.out.println("ClickGUI display set to true!");
                } catch (NoSuchMethodException e) {
                    System.out.println("setDisplay method not found, trying alternative approach...");
                    
                    // 尝试调用onEnable方法
                    try {
                        java.lang.reflect.Method onEnable = clickGUIModule.getClass().getMethod("onEnable");
                        onEnable.invoke(clickGUIModule);
                        System.out.println("ClickGUI onEnable called!");
                    } catch (NoSuchMethodException e2) {
                        System.out.println("onEnable method not found either");
                    }
                }
            } else {
                System.out.println("ClickGUI module is null");
            }
        } catch (Exception e) {
            System.err.println("Error working with ClickGUI module: " + e.getMessage());
            e.printStackTrace();
        }
    }
}