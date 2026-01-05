import zipfile
import sys

def check_jar_contents(jar_path):
    try:
        with zipfile.ZipFile(jar_path, 'r') as jar:
            print(f"Contents of {jar_path}:")
            for name in jar.namelist():
                print(f"  {name}")
            
            # Check for manifest
            if 'META-INF/MANIFEST.MF' in jar.namelist():
                print("\nManifest content:")
                manifest_content = jar.read('META-INF/MANIFEST.MF')
                print(manifest_content.decode('utf-8'))
            else:
                print("\nNo manifest found!")
                
    except Exception as e:
        print(f"Error reading jar file: {e}")

if __name__ == "__main__":
    if len(sys.argv) > 1:
        jar_path = sys.argv[1]
        check_jar_contents(jar_path)
    else:
        print("Please provide a jar file path as argument")