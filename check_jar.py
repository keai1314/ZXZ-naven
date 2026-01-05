import zipfile
import sys

def check_jar_manifest(jar_path):
    try:
        with zipfile.ZipFile(jar_path, 'r') as jar:
            # 读取MANIFEST.MF文件
            manifest_content = jar.read('META-INF/MANIFEST.MF')
            print("Manifest content:")
            print(manifest_content.decode('utf-8'))
            
            # 检查是否包含Agent-Class
            if b'Agent-Class:' in manifest_content:
                print("\n✓ Agent-Class attribute found")
            else:
                print("\n✗ Agent-Class attribute NOT found")
                
    except Exception as e:
        print(f"Error reading jar file: {e}")

if __name__ == "__main__":
    if len(sys.argv) > 1:
        jar_path = sys.argv[1]
        print(f"Checking {jar_path}...")
        check_jar_manifest(jar_path)
    else:
        print("Please provide a jar file path as argument")