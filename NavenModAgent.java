import java.io.File;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

public class NavenModAgent {
    public static void agentmain(String agentArgs, Instrumentation inst) {
        System.out.println("Naven Mod Agent loaded successfully!");
        System.out.println("Agent args: " + agentArgs);
        
        try {
            // 查找模组JAR文件
            File modJar = new File("G:/yuanma/new naven/naven3/build/libs/Naven-Modern-1337.jar");
            
            if (modJar.exists()) {
                System.out.println("Found mod jar: " + modJar.getAbsolutePath());
                
                // 使用URLClassLoader加载模组
                URL modUrl = modJar.toURI().toURL();
                URLClassLoader classLoader = new URLClassLoader(new URL[]{modUrl}, 
                    NavenModAgent.class.getClassLoader());
                
                // 加载并初始化模组
                Class<?> navenClass = classLoader.loadClass("com.heypixel.heypixelmod.obsoverlay.Naven");
                System.out.println("Loaded Naven class: " + navenClass.getName());
                
                // 获取实例
                Method getInstanceMethod = navenClass.getMethod("getInstance");
                Object navenInstance = getInstanceMethod.invoke(null);
                System.out.println("Got Naven instance: " + navenInstance);
                
                // 初始化模组
                Method initMethod = navenClass.getMethod("init");
                initMethod.invoke(navenInstance);
                System.out.println("Naven mod initialized successfully!");
                
            } else {
                System.err.println("Mod jar not found at: " + modJar.getAbsolutePath());
            }
            
        } catch (Exception e) {
            System.err.println("Error loading mod: " + e.getMessage());
            e.printStackTrace();
        }
    }
}