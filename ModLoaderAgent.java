import java.io.File;
import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.net.URLClassLoader;

public class ModLoaderAgent {
    public static void agentmain(String agentArgs, Instrumentation inst) {
        System.out.println("ModLoaderAgent loaded successfully!");
        System.out.println("Agent args: " + agentArgs);
        System.out.println("Instrumentation: " + inst);
        
        try {
            // 直接加载Naven模组jar
            String modJarPath = "G:/yuanma/new naven/naven3/build/libs/Naven-Modern-1337.jar";
            File modJar = new File(modJarPath);
            
            System.out.println("Looking for mod jar at: " + modJar.getAbsolutePath());
            
            if (modJar.exists()) {
                System.out.println("Found mod jar: " + modJar.getAbsolutePath());
                System.out.println("Mod jar size: " + modJar.length() + " bytes");
                
                // 使用URLClassLoader加载模组
                URL modUrl = modJar.toURI().toURL();
                URLClassLoader classLoader = new URLClassLoader(new URL[]{modUrl}, 
                    ModLoaderAgent.class.getClassLoader());
                
                System.out.println("Created classloader with mod jar");
                
                // 加载并注册模组
                Class<?> navenClass = classLoader.loadClass("com.heypixel.heypixelmod.obsoverlay.Naven");
                System.out.println("Loaded Naven class: " + navenClass.getName());
                
                // 获取实例
                java.lang.reflect.Method getInstanceMethod = navenClass.getMethod("getInstance");
                Object instance = getInstanceMethod.invoke(null);
                System.out.println("Naven instance: " + instance);
                
                // 尝试调用modRegister方法注册模组
                try {
                    System.out.println("Calling modRegister method...");
                    java.lang.reflect.Method modRegisterMethod = navenClass.getMethod("modRegister");
                    modRegisterMethod.invoke(null);
                    System.out.println("Naven mod registered successfully!");
                } catch (Exception registerException) {
                    System.err.println("Error during modRegister: " + registerException.getMessage());
                    registerException.printStackTrace();
                    
                    // 如果modRegister失败，尝试其他初始化方法
                    try {
                        System.out.println("Trying alternative initialization methods...");
                        
                        // 尝试调用init方法
                        try {
                            java.lang.reflect.Method initMethod = navenClass.getMethod("init");
                            initMethod.invoke(instance);
                            System.out.println("Naven mod initialized via init method!");
                        } catch (NoSuchMethodException e) {
                            System.out.println("No init method found");
                        }
                        
                        // 尝试调用agentmain方法
                        try {
                            java.lang.reflect.Method agentMainMethod = navenClass.getMethod("agentmain", String.class, java.lang.instrument.Instrumentation.class);
                            agentMainMethod.invoke(null, agentArgs, inst);
                            System.out.println("Naven mod initialized via agentmain method!");
                        } catch (NoSuchMethodException e) {
                            System.out.println("No agentmain method found");
                        }
                        
                    } catch (Exception altException) {
                        System.err.println("Error during alternative initialization: " + altException.getMessage());
                        altException.printStackTrace();
                    }
                }
                
            } else {
                System.err.println("Mod jar not found at: " + modJar.getAbsolutePath());
                // 列出libs目录下的文件
                File libsDir = new File("G:/yuanma/new naven/naven3/build/libs");
                if (libsDir.exists() && libsDir.isDirectory()) {
                    System.err.println("Files in libs directory:");
                    for (File file : libsDir.listFiles()) {
                        System.err.println("  " + file.getName() + " (" + file.length() + " bytes)");
                    }
                } else {
                    System.err.println("Libs directory not found or not a directory: " + libsDir.getAbsolutePath());
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error loading mod: " + e.getMessage());
            e.printStackTrace();
        }
    }
}