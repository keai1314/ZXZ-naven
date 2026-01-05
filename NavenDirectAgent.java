import java.lang.instrument.Instrumentation;

public class NavenDirectAgent {
    public static void agentmain(String agentArgs, Instrumentation inst) {
        System.out.println("NavenDirectAgent loaded successfully!");
        System.out.println("Agent args: " + agentArgs);
        System.out.println("Instrumentation: " + inst);
        
        try {
            // 直接调用Naven类中的agentmain方法来初始化模组
            Class<?> navenClass = Class.forName("com.heypixel.heypixelmod.obsoverlay.Naven");
            System.out.println("Loaded Naven class: " + navenClass.getName());
            
            // 首先检查实例是否存在，如果不存在则创建
            java.lang.reflect.Method getInstanceMethod = navenClass.getMethod("getInstance");
            Object instance = getInstanceMethod.invoke(null);
            
            if (instance == null) {
                System.out.println("Naven instance is null, creating new instance...");
                // 调用modRegister方法创建实例
                java.lang.reflect.Method modRegisterMethod = navenClass.getMethod("modRegister");
                modRegisterMethod.invoke(null);
                
                // 再次检查实例
                instance = getInstanceMethod.invoke(null);
                System.out.println("New Naven instance created: " + instance);
            }
            
            // 使用反射调用Naven的agentmain方法
            java.lang.reflect.Method agentMainMethod = navenClass.getMethod("agentmain", String.class, java.lang.instrument.Instrumentation.class);
            agentMainMethod.invoke(null, agentArgs, inst);
            
            System.out.println("Naven mod initialized through direct agent call!");
        } catch (Exception e) {
            System.err.println("Error initializing Naven mod: " + e.getMessage());
            e.printStackTrace();
        }
    }
}