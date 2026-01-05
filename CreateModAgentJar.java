import java.io.*;
import java.util.jar.*;

public class CreateModAgentJar {
    public static void main(String[] args) throws Exception {
        // 创建manifest
        Manifest manifest = new Manifest();
        Attributes attributes = manifest.getMainAttributes();
        attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        attributes.put(new Attributes.Name("Can-Redefine-Classes"), "true");
        attributes.put(new Attributes.Name("Can-Retransform-Classes"), "true");
        attributes.put(new Attributes.Name("Agent-Class"), "com.heypixel.heypixelmod.obsoverlay.utils.ModInjectAgent");
        
        // 创建jar文件
        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream("build/libs/mod-agent-final.jar"), manifest)) {
            // 添加ModInjectAgent类
            addClassToJar(jos, "build/temp_classes/com/heypixel/heypixelmod/obsoverlay/utils/ModInjectAgent.class", 
                         "com/heypixel/heypixelmod/obsoverlay/utils/ModInjectAgent.class");
            
            System.out.println("JAR file created successfully: build/libs/mod-agent-final.jar");
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