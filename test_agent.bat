@echo off
set JAVA_HOME=C:\Program Files\Java\jdk-17
set AGENT_JAR=build\libs\Naven-Modern-1337-agent.jar

echo Testing Hotswap Agent...
echo Agent JAR: %AGENT_JAR%

if not exist "%AGENT_JAR%" (
    echo Error: Agent JAR not found!
    pause
    exit /b 1
)

echo 1. Building test class...
mkdir -p test_classes 2>nul

:: 创建一个简单的测试类
echo public class TestHotswap { >> test_classes\TestHotswap.java
echo     public static void main(String[] args) { >> test_classes\TestHotswap.java
echo         System.out.println("TestHotswap initialized!"); >> test_classes\TestHotswap.java
echo         while (true) { >> test_classes\TestHotswap.java
echo             try { >> test_classes\TestHotswap.java
echo                 Thread.sleep(2000); >> test_classes\TestHotswap.java
echo                 System.out.println("Running..."); >> test_classes\TestHotswap.java
echo             } catch (InterruptedException e) { >> test_classes\TestHotswap.java
echo                 e.printStackTrace(); >> test_classes\TestHotswap.java
echo             } >> test_classes\TestHotswap.java
echo         } >> test_classes\TestHotswap.java
echo     } >> test_classes\TestHotswap.java
echo } >> test_classes\TestHotswap.java

:: 编译测试类
"%JAVA_HOME%\bin\javac" test_classes\TestHotswap.java

if errorlevel 1 (
    echo Error: Failed to compile test class!
    pause
    exit /b 1
)

echo 2. Starting test application...
start "Test Application" "%JAVA_HOME%\bin\java" -cp test_classes TestHotswap

:: 等待应用启动
ping 127.0.0.1 -n 3 >nul

echo 3. Attaching agent...
for /f "tokens=2" %%i in ('jps ^| findstr "TestHotswap"') do set PID=%%i

echo Found TestHotswap process with PID: %PID%

if not defined PID (
    echo Error: TestHotswap process not found!
    pause
    exit /b 1
)

"%JAVA_HOME%\bin\jattach" %PID% load instrument false "%AGENT_JAR%"

echo 4. Agent attached successfully!
echo You can now modify the TestHotswap.java file, recompile it, and watch the changes take effect.
echo Press any key to exit...
pause >nul

:: 清理测试资源
taskkill /f /im java.exe /fi "windowtitle eq Test Application"
del /f /q test_classes\*.class
del /f /q test_classes\*.java
rmdir test_classes
