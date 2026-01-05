import java.lang.instrument.Instrumentation;

public class DirectNavenLoader {
    public static void agentmain(String agentArgs, Instrumentation inst) {
        System.out.println("DirectNavenLoader agent loaded!");
        System.out.println("Agent args: " + agentArgs);
        
        try {
            // 尝试直接加载已存在于游戏中的Naven类
            Class<?> navenClass = Class.forName("com.heypixel.heypixelmod.obsoverlay.Naven");
            System.out.println("Found Naven class: " + navenClass.getName());
            
            // 尝试获取实例
            try {
                java.lang.reflect.Method getInstanceMethod = navenClass.getMethod("getInstance");
                Object instance = getInstanceMethod.invoke(null);
                System.out.println("Naven instance: " + instance);
                
                if (instance != null) {
                    // 如果实例存在，尝试调用初始化方法
                    try {
                        java.lang.reflect.Method initMethod = navenClass.getMethod("init");
                        initMethod.invoke(instance);
                        System.out.println("Naven initialized successfully!");
                    } catch (NoSuchMethodException e) {
                        System.out.println("No init method found");
                    }
                } else {
                    // 如果实例不存在，尝试调用modRegister方法
                    try {
                        java.lang.reflect.Method modRegisterMethod = navenClass.getMethod("modRegister");
                        modRegisterMethod.invoke(null);
                        System.out.println("Naven registered successfully!");
                    } catch (NoSuchMethodException e) {
                        System.out.println("No modRegister method found");
                    }
                }
            } catch (Exception e) {
                System.err.println("Error working with Naven instance: " + e.getMessage());
                e.printStackTrace();
            }
            
        } catch (ClassNotFoundException e) {
            System.err.println("Naven class not found in the current classpath");
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Error loading Naven: " + e.getMessage());
            e.printStackTrace();
        }
    }
}