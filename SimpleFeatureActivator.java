import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class SimpleFeatureActivator {
    public static void agentmain(String agentArgs, Instrumentation inst) {
        System.out.println("SimpleFeatureActivator loaded!");
        System.out.println("Agent args: " + agentArgs);
        
        try {
            activateFeatures();
        } catch (Exception e) {
            System.err.println("Error activating features: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void activateFeatures() throws Exception {
        System.out.println("Attempting to activate features...");
        
        // 尝试查找并激活可能已经加载的模组功能
        try {
            // 查找Minecraft主类
            Class<?> minecraftClass = Class.forName("net.minecraft.client.Minecraft");
            System.out.println("Found Minecraft class: " + minecraftClass.getName());
            
            // 获取Minecraft实例
            Method minecraftGetInstance = minecraftClass.getMethod("getInstance");
            Object minecraftInstance = minecraftGetInstance.invoke(null);
            System.out.println("Minecraft instance: " + minecraftInstance);
            
            // 尝试查找模组管理器或类似结构
            activateNavenIfPresent();
            
        } catch (ClassNotFoundException e) {
            System.out.println("Minecraft class not found, trying alternative approach...");
            // 尝试直接激活Naven功能
            activateNavenIfPresent();
        }
    }
    
    private static void activateNavenIfPresent() throws Exception {
        try {
            // 尝试查找Naven类
            Class<?> navenClass = Class.forName("com.heypixel.heypixelmod.obsoverlay.Naven");
            System.out.println("Found Naven class: " + navenClass.getName());
            
            // 获取实例
            Method getInstanceMethod = navenClass.getMethod("getInstance");
            Object navenInstance = getInstanceMethod.invoke(null);
            
            if (navenInstance != null) {
                System.out.println("Naven instance found: " + navenInstance);
                
                // 尝试激活UI或其他功能
                try {
                    // 获取模块管理器
                    Method getModuleManager = navenClass.getMethod("getModuleManager");
                    Object moduleManager = getModuleManager.invoke(navenInstance);
                    
                    if (moduleManager != null) {
                        System.out.println("Module manager found, trying to activate modules...");
                        
                        // 尝试激活特定模块，例如ClickGUI
                        try {
                            Class<?> moduleManagerClass = moduleManager.getClass();
                            Method getModule = moduleManagerClass.getMethod("getModule", Class.class);
                            
                            // 尝试获取ClickGUI模块
                            Class<?> clickGUIModuleClass = Class.forName("com.heypixel.heypixelmod.obsoverlay.modules.impl.render.ClickGUIModule");
                            Object clickGUIModule = getModule.invoke(moduleManager, clickGUIModuleClass);
                            
                            if (clickGUIModule != null) {
                                System.out.println("ClickGUI module found, trying to enable...");
                                
                                // 尝试启用模块
                                Method setEnabled = clickGUIModule.getClass().getMethod("setEnabled", boolean.class);
                                setEnabled.invoke(clickGUIModule, true);
                                System.out.println("ClickGUI module enabled successfully!");
                            }
                        } catch (Exception e) {
                            System.out.println("Could not activate ClickGUI module: " + e.getMessage());
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Could not access module manager: " + e.getMessage());
                }
            } else {
                System.out.println("No Naven instance found");
            }
        } catch (ClassNotFoundException e) {
            System.out.println("Naven class not found in current classpath");
        }
    }
}