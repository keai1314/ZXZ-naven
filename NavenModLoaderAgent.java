import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;

public class NavenModLoaderAgent {
    public static void agentmain(String agentArgs, Instrumentation inst) {
        System.out.println("NavenModLoaderAgent loaded!");
        System.out.println("Agent args: " + agentArgs);
        
        try {
            // 检查Naven类是否已经加载
            Class<?> navenClass = null;
            try {
                navenClass = Class.forName("com.heypixel.heypixelmod.obsoverlay.Naven");
                System.out.println("Naven class already loaded: " + navenClass);
            } catch (ClassNotFoundException e) {
                System.out.println("Naven class not found, will try to load it");
            }
            
            // 如果类未加载，则尝试加载
            if (navenClass == null) {
                // 获取系统类加载器
                ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
                
                // 尝试通过反射获取系统类加载器的URLs并添加我们的jar
                try {
                    Method addURLMethod = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
                    addURLMethod.setAccessible(true);
                    
                    // 添加模组jar到类路径
                    String modJarPath = "G:/yuanma/new naven/naven3/build/libs/Naven-Modern-1337.jar";
                    URL modUrl = Paths.get(modJarPath).toUri().toURL();
                    addURLMethod.invoke(systemClassLoader, modUrl);
                    System.out.println("Added mod jar to classpath: " + modUrl);
                    
                    // 现在尝试加载Naven类
                    navenClass = Class.forName("com.heypixel.heypixelmod.obsoverlay.Naven", true, systemClassLoader);
                    System.out.println("Loaded Naven class: " + navenClass);
                } catch (Exception e) {
                    System.err.println("Failed to add jar to classpath: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            
            if (navenClass != null) {
                // 尝试获取实例
                Method getInstanceMethod = navenClass.getMethod("getInstance");
                Object navenInstance = getInstanceMethod.invoke(null);
                
                if (navenInstance == null) {
                    System.out.println("Naven instance is null, trying to create it...");
                    // 尝试调用modRegister方法创建实例
                    Method modRegisterMethod = navenClass.getMethod("modRegister");
                    modRegisterMethod.invoke(null);
                    
                    // 再次检查实例
                    navenInstance = getInstanceMethod.invoke(null);
                    System.out.println("Naven instance after modRegister: " + navenInstance);
                } else {
                    System.out.println("Naven instance found: " + navenInstance);
                }
                
                if (navenInstance != null) {
                    // 尝试初始化模组
                    try {
                        Method initMethod = navenClass.getMethod("init");
                        initMethod.invoke(navenInstance);
                        System.out.println("Naven mod initialized successfully!");
                    } catch (NoSuchMethodException e) {
                        System.out.println("No init method found, mod may be initialized already");
                    }
                    
                    // 尝试激活ClickGUI模块
                    activateClickGUIModule(navenInstance);
                }
            } else {
                System.err.println("Failed to load Naven class");
            }
            
        } catch (Exception e) {
            System.err.println("Error in NavenModLoaderAgent: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void activateClickGUIModule(Object navenInstance) {
        try {
            // 获取Naven类
            Class<?> navenClass = navenInstance.getClass();
            
            // 获取moduleManager字段
            Field moduleManagerField = navenClass.getDeclaredField("moduleManager");
            moduleManagerField.setAccessible(true);
            Object moduleManager = moduleManagerField.get(navenInstance);
            
            if (moduleManager != null) {
                System.out.println("ModuleManager found: " + moduleManager);
                
                // 获取ClickGUI模块
                Class<?> moduleManagerClass = moduleManager.getClass();
                Method getModuleMethod = moduleManagerClass.getMethod("getModule", Class.class);
                
                Class<?> clickGUIClass = Class.forName("com.heypixel.heypixelmod.obsoverlay.modules.impl.render.ClickGUIModule");
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