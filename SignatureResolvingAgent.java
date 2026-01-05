import java.io.File;
import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.jar.JarFile;

public class SignatureResolvingAgent {
    public static void agentmain(String agentArgs, Instrumentation inst) {
        System.out.println("SignatureResolvingAgent loaded!");
        System.out.println("Agent args: " + agentArgs);
        System.out.println("Instrumentation: " + inst);
        
        try {
            loadModWithSignatureResolution(inst);
        } catch (Exception e) {
            System.err.println("Error loading mod with signature resolution: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void loadModWithSignatureResolution(Instrumentation inst) throws Exception {
        System.out.println("Attempting to load mod with signature resolution...");
        
        // 直接加载Naven模组jar
        String modJarPath = "G:/yuanma/new naven/naven3/build/libs/Naven-Modern-1337.jar";
        File modJar = new File(modJarPath);
        
        System.out.println("Looking for mod jar at: " + modJar.getAbsolutePath());
        
        if (modJar.exists()) {
            System.out.println("Found mod jar: " + modJar.getAbsolutePath());
            System.out.println("Mod jar size: " + modJar.length() + " bytes");
            
            // 创建一个半隔离的类加载器来避免签名冲突但仍能访问系统类
            URL modUrl = modJar.toURI().toURL();
            SemiIsolatedClassLoader semiIsolatedClassLoader = new SemiIsolatedClassLoader(new URL[]{modUrl});
            
            System.out.println("Created semi-isolated classloader with mod jar");
            
            // 尝试加载Naven类
            try {
                Class<?> navenClass = semiIsolatedClassLoader.loadClass("com.heypixel.heypixelmod.obsoverlay.Naven");
                System.out.println("Loaded Naven class with semi-isolated classloader: " + navenClass.getName());
                System.out.println("Naven class loader: " + navenClass.getClassLoader());
                
                // 检查是否已存在实例
                try {
                    java.lang.reflect.Method getInstanceMethod = navenClass.getMethod("getInstance");
                    Object existingInstance = getInstanceMethod.invoke(null);
                    System.out.println("Existing Naven instance: " + existingInstance);
                    
                    if (existingInstance != null) {
                        System.out.println("Naven already initialized, trying to activate features...");
                        activateFeatures(existingInstance);
                    } else {
                        // 调用modRegister方法创建新实例
                        try {
                            System.out.println("Calling modRegister method to create new instance...");
                            java.lang.reflect.Method modRegisterMethod = navenClass.getMethod("modRegister");
                            modRegisterMethod.invoke(null);
                            System.out.println("Naven mod registered successfully!");
                            
                            // 再次检查实例
                            Object newInstance = getInstanceMethod.invoke(null);
                            System.out.println("New Naven instance created: " + newInstance);
                            
                            if (newInstance != null) {
                                activateFeatures(newInstance);
                            }
                        } catch (Exception registerException) {
                            System.err.println("Error during modRegister: " + registerException.getMessage());
                            registerException.printStackTrace();
                            
                            // 尝试直接创建实例
                            try {
                                System.out.println("Trying to create Naven instance directly...");
                                java.lang.reflect.Constructor<?> constructor = navenClass.getDeclaredConstructor();
                                constructor.setAccessible(true);
                                Object directInstance = constructor.newInstance();
                                System.out.println("Direct Naven instance created: " + directInstance);
                                
                                if (directInstance != null) {
                                    activateFeatures(directInstance);
                                }
                            } catch (Exception directException) {
                                System.err.println("Error creating direct instance: " + directException.getMessage());
                                directException.printStackTrace();
                            }
                        }
                    }
                } catch (Exception instanceException) {
                    System.err.println("Error checking Naven instance: " + instanceException.getMessage());
                    instanceException.printStackTrace();
                }
            } catch (Exception loadException) {
                System.err.println("Error loading Naven class: " + loadException.getMessage());
                loadException.printStackTrace();
                
                // 尝试列出可以从JAR加载的类
                try {
                    System.out.println("Listing available classes in JAR...");
                    JarFile jarFile = new JarFile(modJar);
                    jarFile.stream().forEach(entry -> {
                        if (entry.getName().endsWith(".class")) {
                            System.out.println("  Found class: " + entry.getName());
                        }
                    });
                    jarFile.close();
                } catch (Exception jarException) {
                    System.err.println("Error listing JAR contents: " + jarException.getMessage());
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
    
    /**
     * 半隔离类加载器，用于避免签名冲突但仍能访问系统类和Minecraft类
     */
    static class SemiIsolatedClassLoader extends URLClassLoader {
        public SemiIsolatedClassLoader(URL[] urls) {
            // 使用系统类加载器作为父类加载器，这样可以访问Log4j等系统类
            super(urls, ClassLoader.getSystemClassLoader());
        }
        
        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            // 首先检查是否已加载
            Class<?> clazz = findLoadedClass(name);
            if (clazz != null) {
                return clazz;
            }
            
            try {
                // 对于模组类，尝试从本地URL加载以避免签名冲突
                if (name.startsWith("com.heypixel.heypixelmod.obsoverlay")) {
                    clazz = findClass(name);
                    if (resolve) {
                        resolveClass(clazz);
                    }
                    System.out.println("Loaded from local JAR: " + name);
                    return clazz;
                }
                
                // 其他类委托给父类加载器
                System.out.println("Delegating to parent classloader: " + name);
                return super.loadClass(name, resolve);
            } catch (ClassNotFoundException e) {
                // 如果找不到，再尝试父类加载器
                System.out.println("Class not found in local JAR, trying parent: " + name);
                return super.loadClass(name, resolve);
            }
        }
    }
}