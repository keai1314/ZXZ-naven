import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ModBytecodeAgent {
    public static void agentmain(String agentArgs, Instrumentation inst) {
        System.out.println("ModBytecodeAgent loaded!");
        System.out.println("Agent args: " + agentArgs);
        System.out.println("Instrumentation: " + inst);
        
        try {
            loadModThroughInstrumentation(inst);
        } catch (Exception e) {
            System.err.println("Error loading mod through instrumentation: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void loadModThroughInstrumentation(Instrumentation inst) throws Exception {
        System.out.println("Attempting to load mod through instrumentation...");
        
        // 获取当前类加载器
        ClassLoader classLoader = ModBytecodeAgent.class.getClassLoader();
        System.out.println("Current classloader: " + classLoader);
        
        // 尝试通过Instrumentation获取已加载的类
        Class<?>[] loadedClasses = inst.getAllLoadedClasses();
        System.out.println("Number of loaded classes: " + loadedClasses.length);
        
        // 查找关键类
        Class<?> minecraftClass = null;
        Class<?> forgeClass = null;
        
        for (Class<?> clazz : loadedClasses) {
            String className = clazz.getName();
            if (className.startsWith("net.minecraft.client.Minecraft")) {
                minecraftClass = clazz;
                System.out.println("Found Minecraft class: " + className);
            } else if (className.startsWith("net.minecraftforge")) {
                forgeClass = clazz;
                System.out.println("Found Forge class: " + className);
            }
        }
        
        if (minecraftClass != null && forgeClass != null) {
            System.out.println("Minecraft and Forge detected, trying to load mod...");
            // 这里可以添加更多的模组加载逻辑
        } else {
            System.out.println("Minecraft or Forge not detected in loaded classes");
        }
    }
}