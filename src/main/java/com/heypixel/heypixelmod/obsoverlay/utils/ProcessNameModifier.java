package com.heypixel.heypixelmod.obsoverlay.utils;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import java.util.Random;

public class ProcessNameModifier {
    
    // 主进程名称
    private static final String BASE_PROCESS_NAME = "Kards Client ";
    
    // 随机标语列表
    private static final String[] RANDOM_MESSAGES = {
        "领导我们事业的核心力量是中国共产党",
        "指导我们思想的理论基础是马克思列宁主义",
        "阶级斗争，一些阶级胜利了，一些阶级消灭了",
        "民族斗争，说到底，是一个阶级斗争问题",
        "中国的反动分子，靠我们组织起人民去打倒他",
        "革命是暴动，是一个阶级推翻一个阶级的暴烈的行动",
        "革命党是群众的向导",
        "工业无产阶级是我们革命的领导力量",
        "什么人站在革命人民方面，他就是革命派",
        "什么人站在帝国主义，封建主义，官僚资本主义方面，他就是反革命派",
        "什么人只是口头上站在革命人民方面，而在行动上则另是一样，他就是一个口头革命派",
        "如果不但在口头上而且在行动上也站在革命人民方面，他就是一个完全的革命派",
        "如若不被敌人反对，那就不好了，那一定是同敌人同流合污了",
        "如若被敌人反对，那就好了，那就证明我们同敌人划清界线了",
        "如若敌人起劲地反对我们，把我们说的一塌糊涂，一无是处那就更好了，那就整命我们不但同敌人划清了界限，而且证明我们的工作是很有成绩的了",
        "凡是敌人反对的，我们就要拥护",
        "凡是敌人拥护的，我们就要反对",
    };
    
    /**
     * 修改当前进程的名称
     */
    public static void modifyProcessName() {
        String finalProcessName = generateProcessName();
        System.out.println("尝试设置进程名称为: " + finalProcessName);
        
        try {
            // 简化平台检测，直接使用系统属性
            String osName = System.getProperty("os.name").toLowerCase();
            if (osName.contains("win")) {
                // Windows平台处理
                modifyProcessNameWindows(finalProcessName);
            } else if (osName.contains("linux")) {
                // Linux平台处理
                modifyProcessNameLinux(finalProcessName);
            } else if (osName.contains("mac")) {
                // macOS平台处理
                modifyProcessNameMac(finalProcessName);
            }
        } catch (Exception e) {
            System.err.println("修改进程名称时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 生成完整的进程名称，包括基础名称和随机标语
     * @return 完整的进程名称
     */
    private static String generateProcessName() {
        Random random = new Random();
        String randomMessage = RANDOM_MESSAGES[random.nextInt(RANDOM_MESSAGES.length)];
        return BASE_PROCESS_NAME + "/// " + randomMessage;
    }
    
    /**
     * Windows平台修改进程名称
     * @param newName 新的进程名称
     */
    private static void modifyProcessNameWindows(String newName) {
        try {
            // 使用更安全的方式加载user32库
            User32 user32 = null;
            try {
                user32 = Native.load("user32", User32.class);
            } catch (UnsatisfiedLinkError e) {
                System.err.println("无法加载user32库: " + e.getMessage());
                return;
            }
            
            // 安全地调用GetConsoleWindow
            Pointer consoleWindow = null;
            try {
                consoleWindow = user32.GetConsoleWindow();
            } catch (UnsatisfiedLinkError e) {
                System.err.println("无法找到GetConsoleWindow函数: " + e.getMessage());
            }
            
            if (consoleWindow != null) {
                boolean result = user32.SetWindowTextW(consoleWindow, newName);
                System.out.println("设置窗口标题结果: " + result);
            }
            
            // 通过系统属性设置Java进程名
            System.setProperty("sun.java.command", newName);
            System.out.println("已通过系统属性设置进程名");
        } catch (Exception e) {
            System.err.println("在Windows上修改进程名称失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Linux平台修改进程名称
     * @param newName 新的进程名称
     */
    private static void modifyProcessNameLinux(String newName) {
        // 在Linux上，我们可以尝试通过JNA调用prctl系统调用来修改进程名
        try {
            LibC libC = Native.load("c", LibC.class);
            libC.prctl(15, newName, 0, 0, 0); // PR_SET_NAME = 15
            System.out.println("已在Linux上设置进程名");
        } catch (Exception e) {
            System.err.println("在Linux上修改进程名称失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * macOS平台修改进程名称
     * @param newName 新的进程名称
     */
    private static void modifyProcessNameMac(String newName) {
        // macOS与Linux类似，也可以尝试使用prctl
        modifyProcessNameLinux(newName);
    }
    
    /**
     * JNA接口定义libc库函数
     */
    interface LibC extends Library {
        int prctl(int option, String arg2, long arg3, long arg4, long arg5);
    }
    
    /**
     * JNA接口定义Windows User32库函数
     */
    interface User32 extends Library {
        Pointer GetConsoleWindow();
        boolean SetWindowTextW(Pointer hWnd, String lpString);
    }
    
    // 静态初始化块，在类加载时自动执行
    static {
        modifyProcessName();
    }
}