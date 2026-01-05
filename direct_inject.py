import subprocess
import sys
import os

def inject_agent(pid, agent_jar):
    try:
        # 使用jattach工具进行注入（如果可用）
        cmd = ["jattach", str(pid), "load", "agent", agent_jar]
        result = subprocess.run(cmd, capture_output=True, text=True)
        
        if result.returncode == 0:
            print("Agent注入成功!")
            print(result.stdout)
        else:
            print("Agent注入失败:")
            print(result.stderr)
            # 如果jattach不可用，尝试使用Java方式
            try_java_injection(pid, agent_jar)
    except FileNotFoundError:
        print("jattach工具未找到，尝试使用Java方式注入...")
        try_java_injection(pid, agent_jar)
    except Exception as e:
        print(f"注入过程中发生错误: {e}")
        try_java_injection(pid, agent_jar)

def try_java_injection(pid, agent_jar):
    try:
        # 使用Java Attach API
        cmd = ["java", "-cp", ".", "AttachAgent", str(pid), agent_jar]
        result = subprocess.run(cmd, capture_output=True, text=True, cwd=os.getcwd())
        
        print("Java注入结果:")
        print("STDOUT:", result.stdout)
        print("STDERR:", result.stderr)
        print("Return code:", result.returncode)
    except Exception as e:
        print(f"Java注入也失败了: {e}")

if __name__ == "__main__":
    if len(sys.argv) != 3:
        print("用法: python direct_inject.py <PID> <AGENT_JAR_PATH>")
        sys.exit(1)
    
    pid = sys.argv[1]
    agent_jar = sys.argv[2]
    
    print(f"开始注入Agent到进程: {pid}")
    print(f"Agent JAR路径: {agent_jar}")
    
    inject_agent(pid, agent_jar)