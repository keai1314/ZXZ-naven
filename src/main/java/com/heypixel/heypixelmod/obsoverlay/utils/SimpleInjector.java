package com.heypixel.heypixelmod.obsoverlay.utils;

import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;

/**
 * Simple injector
 */
public class SimpleInjector {
    
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java -jar simple-injector.jar <pid> <agent-jar-path>");
            System.out.println("Or: java -jar simple-injector.jar list");
            return;
        }
        
        String command = args[0];
        
        if ("list".equals(command)) {
            listProcesses();
            return;
        }
        
        if (args.length < 2) {
            System.out.println("Usage: java -jar simple-injector.jar <pid> <agent-jar-path>");
            System.out.println("Or: java -jar simple-injector.jar list");
            return;
        }
        
        String pid = args[0];
        String agentJar = args[1];
        inject(pid, agentJar);
    }
    
    private static void listProcesses() {
        try {
            List<VirtualMachineDescriptor> vms = VirtualMachine.list();
            System.out.println("Found Java processes:");
            for (VirtualMachineDescriptor vm : vms) {
                System.out.println("PID: " + vm.id() + ", Name: " + vm.displayName());
            }
        } catch (Exception e) {
            System.err.println("Failed to list processes: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void inject(String pid, String agentJar) {
        try {
            // 使用Paths.get来处理路径并获取绝对路径
            File jarFile = Paths.get(agentJar).toAbsolutePath().normalize().toFile();
            System.out.println("Checking for agent JAR at: " + jarFile.getAbsolutePath());
            
            if (!jarFile.exists()) {
                // 尝试其他可能的路径解析方式
                File currentDirJar = new File(System.getProperty("user.dir"), agentJar);
                if (currentDirJar.exists()) {
                    jarFile = currentDirJar.getAbsoluteFile();
                    System.out.println("Found agent JAR at: " + jarFile.getAbsolutePath());
                } else {
                    System.err.println("Agent JAR file does not exist: " + jarFile.getAbsolutePath());
                    System.err.println("Current directory: " + System.getProperty("user.dir"));
                    System.err.println("Tried paths:");
                    System.err.println("  1. Direct path: " + new File(agentJar).getAbsolutePath());
                    System.err.println("  2. Relative to current dir: " + currentDirJar.getAbsolutePath());
                    return;
                }
            }
            
            System.out.println("Connecting to process: " + pid);
            VirtualMachine vm = VirtualMachine.attach(pid);
            System.out.println("Loading agent: " + jarFile.getAbsolutePath());
            vm.loadAgent(jarFile.getAbsolutePath());
            vm.detach();
            System.out.println("Injection completed successfully");
        } catch (Exception e) {
            System.err.println("Injection failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}