import java.io.*;
import java.util.jar.*;

public class CreateModActivatorAgentJar {
    public static void main(String[] args) throws IOException {
        String jarFileName = "mod-activator-agent.jar";
        String manifestFile = "mod-activator-manifest.mf";
        String agentClassFile = "ModActivatorAgent.class";
        
        // 创建JAR文件
        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(jarFileName))) {
            // 添加META-INF目录
            JarEntry metaInfDir = new JarEntry("META-INF/");
            jos.putNextEntry(metaInfDir);
            jos.closeEntry();
            
            // 添加MANIFEST.MF文件
            JarEntry manifestEntry = new JarEntry("META-INF/MANIFEST.MF");
            jos.putNextEntry(manifestEntry);
            
            // 读取manifest文件内容并写入
            try (FileInputStream fis = new FileInputStream(manifestFile);
                 BufferedInputStream bis = new BufferedInputStream(fis)) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = bis.read(buffer)) != -1) {
                    jos.write(buffer, 0, bytesRead);
                }
            }
            jos.closeEntry();
            
            // 添加ModActivatorAgent.class文件
            JarEntry classEntry = new JarEntry("ModActivatorAgent.class");
            jos.putNextEntry(classEntry);
            
            // 读取class文件内容并写入
            try (FileInputStream fis = new FileInputStream(agentClassFile);
                 BufferedInputStream bis = new BufferedInputStream(fis)) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = bis.read(buffer)) != -1) {
                    jos.write(buffer, 0, bytesRead);
                }
            }
            jos.closeEntry();
        }
        
        System.out.println("Successfully created " + jarFileName);
    }
}