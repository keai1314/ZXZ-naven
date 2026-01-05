import java.io.*;
import java.util.jar.*;

public class CreateJarLoaderJar {
    public static void main(String[] args) throws IOException {
        String jarFileName = "jar-naven-loader.jar";
        String manifestFile = "jar-loader-manifest.mf";
        String classFile = "JarNavenLoader.class";

        // 创建Manifest
        Manifest manifest = new Manifest();
        try (FileInputStream fis = new FileInputStream(manifestFile)) {
            manifest.read(fis);
        }

        // 验证manifest中的属性
        Attributes attrs = manifest.getMainAttributes();
        System.out.println("Manifest attributes:");
        for (Object key : attrs.keySet()) {
            System.out.println(key + ": " + attrs.get(key));
        }

        // 确保必要的属性存在
        if (!attrs.containsKey(new Attributes.Name("Agent-Class"))) {
            attrs.put(new Attributes.Name("Agent-Class"), "JarNavenLoader");
            System.out.println("Added missing Agent-Class attribute");
        }

        // 创建JAR文件
        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(jarFileName), manifest)) {
            // 添加class文件到JAR
            File classFileObj = new File(classFile);
            try (FileInputStream fis = new FileInputStream(classFileObj)) {
                JarEntry entry = new JarEntry(classFileObj.getName());
                jos.putNextEntry(entry);

                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    jos.write(buffer, 0, bytesRead);
                }
                jos.closeEntry();
            }
        }

        System.out.println("Created JAR file: " + jarFileName);
        
        // 验证创建的JAR文件
        verifyManifest(jarFileName);
    }
    
    private static void verifyManifest(String jarFileName) throws IOException {
        try (JarInputStream jis = new JarInputStream(new FileInputStream(jarFileName))) {
            Manifest manifest = jis.getManifest();
            if (manifest != null) {
                Attributes attrs = manifest.getMainAttributes();
                System.out.println("Verified manifest attributes in " + jarFileName + ":");
                for (Object key : attrs.keySet()) {
                    System.out.println(key + ": " + attrs.get(key));
                }
                
                if (attrs.containsKey(new Attributes.Name("Agent-Class"))) {
                    System.out.println("Agent-Class attribute found: " + attrs.get(new Attributes.Name("Agent-Class")));
                } else {
                    System.out.println("ERROR: Agent-Class attribute NOT found!");
                }
            } else {
                System.out.println("ERROR: No manifest found in " + jarFileName);
            }
        }
    }
}