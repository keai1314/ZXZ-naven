import java.lang.instrument.Instrumentation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class NavenCreatorAgent {
    public static void agentmain(String agentArgs, Instrumentation inst) {
        System.out.println("NavenCreatorAgent loaded!");
        System.out.println("Agent args: " + agentArgs);
        
        try {
            // 尝试直接创建Naven实例
            createNavenInstance();
        } catch (Exception e) {
            System.err.println("Error creating Naven instance: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void createNavenInstance() throws Exception {
        System.out.println("Attempting to create Naven instance...");
        
        // 使用反射获取Naven类
        Class<?> navenClass = Class.forName("com.heypixel.heypixelmod.obsoverlay.Naven");
        System.out.println("Found Naven class: " + navenClass.getName());
        
        // 检查是否已存在实例
        Method getInstanceMethod = navenClass.getMethod("getInstance");
        Object existingInstance = getInstanceMethod.invoke(null);
        
        if (existingInstance != null) {
            System.out.println("Naven instance already exists: " + existingInstance);
            try {
                // 尝试调用init方法
                Method initMethod = navenClass.getMethod("init");
                initMethod.invoke(existingInstance);
                System.out.println("Naven initialized successfully!");
            } catch (NoSuchMethodException e) {
                System.out.println("No init method found");
            }
        } else {
            System.out.println("No existing Naven instance found, creating new one...");
            
            // 尝试调用modRegister方法创建实例
            try {
                Method modRegisterMethod = navenClass.getMethod("modRegister");
                modRegisterMethod.invoke(null);
                System.out.println("Naven registered successfully!");
                
                // 再次检查实例
                Object newInstance = getInstanceMethod.invoke(null);
                System.out.println("New Naven instance created: " + newInstance);
            } catch (Exception e) {
                System.err.println("Failed to register Naven via modRegister: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}