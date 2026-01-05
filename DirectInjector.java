import com.heypixel.heypixelmod.obsoverlay.utils.ExternalInjector;

public class DirectInjector {
    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("用法: java DirectInjector <PID> <AGENT_JAR_PATH>");
            return;
        }
        
        String pid = args[0];
        String agentJarPath = args[1];
        
        System.out.println("开始注入...");
        System.out.println("PID: " + pid);
        System.out.println("Agent JAR路径: " + agentJarPath);
        
        boolean success = ExternalInjector.injectByPid(pid, agentJarPath);
        
        if (success) {
            System.out.println("注入成功!");
        } else {
            System.out.println("注入失败!");
        }
    }
}