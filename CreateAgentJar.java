import java.io.*;
import java.util.jar.*;

public class CreateAgentJar {
    public static void main(String[] args) throws Exception {
        // 创建manifest
        Manifest manifest = new Manifest();
        Attributes attributes = manifest.getMainAttributes();
        attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        attributes.put(new Attributes.Name("Can-Redefine-Classes"), "true");
        attributes.put(new Attributes.Name("Can-Retransform-Classes"), "true");
        attributes.put(new Attributes.Name("Agent-Class"), "ModLoaderAgent");
        
        // 创建jar文件
        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream("mod-loader-final.jar"), manifest)) {
            // 添加ModLoaderAgent类
            addClassToJar(jos, "ModLoaderAgent.class", "ModLoaderAgent.class");
            System.out.println("JAR file created successfully: mod-loader-final.jar");
        }
    }
    
    private static void addClassToJar(JarOutputStream jos, String filePath, String entryName) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            System.err.println("File not found: " + filePath);
            return;
        }
        
        JarEntry entry = new JarEntry(entryName);
        jos.putNextEntry(entry);
        
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                jos.write(buffer, 0, bytesRead);
            }
        }
        
        jos.closeEntry();
    }
}