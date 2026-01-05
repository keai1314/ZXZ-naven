import java.lang.instrument.Instrumentation;
import java.net.URLClassLoader;

public class ClassPathAgent {
    public static void agentmain(String agentArgs, Instrumentation inst) {
        System.out.println("ClassPathAgent loaded!");
        System.out.println("Agent args: " + agentArgs);
        System.out.println("Instrumentation: " + inst);
        
        try {
            inspectClassPath();
        } catch (Exception e) {
            System.err.println("Error inspecting classpath: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void inspectClassPath() throws Exception {
        System.out.println("Inspecting classpath...");
        
        // 获取系统类加载器
        ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
        System.out.println("System classloader: " + systemClassLoader);
        
        // 检查是否是URLClassLoader
        if (systemClassLoader instanceof URLClassLoader) {
            URLClassLoader urlClassLoader = (URLClassLoader) systemClassLoader;
            System.out.println("URLs in classpath:");
            for (java.net.URL url : urlClassLoader.getURLs()) {
                System.out.println("  " + url);
            }
        }
        
        // 尝试查找模组相关类
        String[] classNames = {
            "com.heypixel.heypixelmod.obsoverlay.Naven",
            "tech.naven.NavenModLoader",
            "net.minecraftforge.fml.common.Mod",
            "net.minecraft.client.Minecraft"
        };
        
        for (String className : classNames) {
            try {
                Class<?> clazz = Class.forName(className);
                System.out.println("Found class: " + className + " -> " + clazz.getClassLoader());
            } catch (ClassNotFoundException e) {
                System.out.println("Class not found: " + className);
            }
        }
    }
}