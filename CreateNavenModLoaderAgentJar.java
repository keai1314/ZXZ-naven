import java.io.*;
import java.util.jar.*;

public class CreateNavenModLoaderAgentJar {
    public static void main(String[] args) throws IOException {
        String jarFileName = "naven-mod-loader-agent.jar";
        String manifestFile = "naven-mod-loader-manifest.mf";
        String agentClassFile = "NavenModLoaderAgent.class";
        
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
            
            // 添加NavenModLoaderAgent.class文件
            JarEntry classEntry = new JarEntry("NavenModLoaderAgent.class");
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
        
        // 验证创建的JAR文件
        verifyJarManifest(jarFileName);
    }
    
    private static void verifyJarManifest(String jarFileName) {
        try (JarInputStream jis = new JarInputStream(new FileInputStream(jarFileName))) {
            Manifest manifest = jis.getManifest();
            if (manifest != null) {
                System.out.println("Manifest attributes:");
                manifest.getMainAttributes().forEach((key, value) -> 
                    System.out.println(key + ": " + value));
            } else {
                System.out.println("No manifest found in " + jarFileName);
            }
        } catch (IOException e) {
            System.err.println("Error reading manifest: " + e.getMessage());
        }
    }
}