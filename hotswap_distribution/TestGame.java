public class TestGame {
    public static void main(String[] args) {
        System.out.println("=== 测试游戏进程 ===");
        System.out.println("进程已启动，PID: " + getProcessId());
        System.out.println("等待热注入agent附加...");
        System.out.println("按Ctrl+C退出");
        
        while (true) {
            try {
                Thread.sleep(2000);
                System.out.println("游戏运行中...");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    
    private static String getProcessId() {
        // 获取当前进程ID的简单实现
        String name = java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
        return name.split("@")[0];
    }
}