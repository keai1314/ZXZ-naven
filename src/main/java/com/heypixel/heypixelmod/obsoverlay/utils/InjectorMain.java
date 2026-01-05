package com.heypixel.heypixelmod.obsoverlay.utils;

/**
 * 注入器主类，用于从外部运行注入器
 */
public class InjectorMain {
    
    public static void main(String[] args) {
        System.out.println("Naven External Injector");
        System.out.println("用法:");
        System.out.println("  1. java -cp <this-jar> com.heypixel.heypixelmod.obsoverlay.utils.InjectorMain list");
        System.out.println("     - 列出所有Java进程");
        System.out.println("  2. java -cp <this-jar> com.heypixel.heypixelmod.obsoverlay.utils.InjectorMain inject <agent-jar-path>");
        System.out.println("     - 注入agent到Minecraft进程");
        System.out.println("  3. java -cp <this-jar> com.heypixel.heypixelmod.obsoverlay.utils.InjectorMain inject <pid> <agent-jar-path>");
        System.out.println("     - 注入agent到指定PID的进程");
        
        if (args.length == 0) {
            System.out.println("请提供参数");
            return;
        }
        
        String command = args[0].toLowerCase();
        
        switch (command) {
            case "list":
                ExternalInjector.listJavaProcesses();
                break;
                
            case "inject":
                if (args.length == 2) {
                    // 自动查找Minecraft进程
                    String agentJarPath = args[1];
                    boolean success = ExternalInjector.injectIntoMinecraft(agentJarPath);
                    if (success) {
                        System.out.println("注入成功");
                    } else {
                        System.out.println("注入失败");
                    }
                } else if (args.length == 3) {
                    // 指定PID
                    String pid = args[1];
                    String agentJarPath = args[2];
                    boolean success = ExternalInjector.injectByPid(pid, agentJarPath);
                    if (success) {
                        System.out.println("注入成功");
                    } else {
                        System.out.println("注入失败");
                    }
                } else {
                    System.out.println("无效的参数数量");
                }
                break;
                
            default:
                System.out.println("未知命令: " + command);
        }
    }
}