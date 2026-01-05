package com.heypixel.heypixelmod.obsoverlay.utils;

import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;

import java.io.File;
import java.util.List;

/**
 * 外部注入器，用于在不修改游戏文件的情况下注入代码
 */
public class ExternalInjector {
    
    /**
     * 查找并附加到Minecraft进程
     * @param jarPath 要注入的jar文件路径
     * @return 是否成功注入
     */
    public static boolean injectIntoMinecraft(String jarPath) {
        try {
            // 检查jar文件是否存在
            File jarFile = new File(jarPath);
            if (!jarFile.exists()) {
                System.err.println("Jar文件不存在: " + jarPath);
                return false;
            }
            
            // 查找Minecraft进程
            List<VirtualMachineDescriptor> vms = VirtualMachine.list();
            VirtualMachineDescriptor targetVm = null;
            
            for (VirtualMachineDescriptor vm : vms) {
                // 匹配Minecraft进程
                if (isMinecraftProcess(vm)) {
                    targetVm = vm;
                    break;
                }
            }
            
            if (targetVm == null) {
                System.err.println("未找到Minecraft进程");
                return false;
            }
            
            // 附加到目标进程
            VirtualMachine vm = null;
            try {
                vm = VirtualMachine.attach(targetVm);
                // 加载agent
                vm.loadAgent(jarPath);
                System.out.println("成功将 " + jarPath + " 注入到进程 " + targetVm.displayName());
                return true;
            } finally {
                // 分离
                if (vm != null) {
                    vm.detach();
                }
            }
        } catch (Exception e) {
            System.err.println("注入失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 判断是否为Minecraft进程
     * @param vm 虚拟机描述符
     * @return 是否为Minecraft进程
     */
    private static boolean isMinecraftProcess(VirtualMachineDescriptor vm) {
        String displayName = vm.displayName();
        // 检查常见的Minecraft进程标识
        return displayName.contains("minecraft") || 
               displayName.contains("Minecraft") || 
               displayName.contains("net.minecraft") ||
               displayName.contains("launchwrapper") ||
               displayName.contains("fabric") ||
               displayName.contains("forge");
    }
    
    /**
     * 根据进程ID注入
     * @param pid 进程ID
     * @param jarPath jar文件路径
     * @return 是否成功注入
     */
    public static boolean injectByPid(String pid, String jarPath) {
        try {
            File jarFile = new File(jarPath);
            if (!jarFile.exists()) {
                System.err.println("Jar文件不存在: " + jarPath);
                return false;
            }
            
            VirtualMachine vm = null;
            try {
                vm = VirtualMachine.attach(pid);
                vm.loadAgent(jarPath);
                System.out.println("成功将 " + jarPath + " 注入到进程 " + pid);
                return true;
            } finally {
                if (vm != null) {
                    vm.detach();
                }
            }
        } catch (Exception e) {
            System.err.println("注入失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 列出所有Java进程
     */
    public static void listJavaProcesses() {
        try {
            List<VirtualMachineDescriptor> vms = VirtualMachine.list();
            System.out.println("找到以下Java进程:");
            for (VirtualMachineDescriptor vm : vms) {
                System.out.println("PID: " + vm.id() + ", Name: " + vm.displayName());
            }
        } catch (Exception e) {
            System.err.println("列出进程失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}