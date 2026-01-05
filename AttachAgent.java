import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.AttachNotSupportedException;
import java.io.IOException;

public class AttachAgent {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java AttachAgent <PID> <AGENT_JAR_PATH>");
            return;
        }

        String pid = args[0];
        String agentJarPath = args[1];

        System.out.println("开始附加Agent到进程: " + pid);
        System.out.println("Agent JAR路径: " + agentJarPath);

        try {
            VirtualMachine vm = VirtualMachine.attach(pid);
            vm.loadAgent(agentJarPath);
            vm.detach();
            System.out.println("Agent附加成功!");
        } catch (AttachNotSupportedException e) {
            System.err.println("附加不支持: " + e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("IO错误: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("附加Agent失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}