import java.io.File;
import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.net.URLClassLoader;

public class JarNavenLoader {
    public static void agentmain(String agentArgs, Instrumentation inst) {
        System.out.println("JarNavenLoader agent loaded!");
        System.out.println("Agent args: " + agentArgs);
        System.out.println("Instrumentation: " + inst);
        
        try {
            loadNavenFromJar();
        } catch (Exception e) {
            System.err.println("Error loading Naven from jar: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void loadNavenFromJar() throws Exception {
        System.out.println("Attempting to load Naven from jar file...");
        
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
                JarNavenLoader.class.getClassLoader());
            
            System.out.println("Created classloader with mod jar");
            
            // 加载Naven类
            Class<?> navenClass = classLoader.loadClass("com.heypixel.heypixelmod.obsoverlay.Naven");
            System.out.println("Loaded Naven class: " + navenClass.getName());
            
            // 尝试调用modRegister方法注册模组
            try {
                System.out.println("Calling modRegister method...");
                java.lang.reflect.Method modRegisterMethod = navenClass.getMethod("modRegister");
                modRegisterMethod.invoke(null);
                System.out.println("Naven mod registered successfully!");
            } catch (Exception registerException) {
                System.err.println("Error during modRegister: " + registerException.getMessage());
                registerException.printStackTrace();
            }
        } else {
            System.err.println("Mod jar not found at: " + modJar.getAbsolutePath());
        }
    }
}