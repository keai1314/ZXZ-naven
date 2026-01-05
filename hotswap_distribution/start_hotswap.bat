@echo off
setlocal enabledelayedexpansion

echo =================================
echo      Naven热注入工具 v1.0
echo =================================
echo.
echo 正在检测Java环境...

:: 检查Java是否安装
where java >nul 2>nul
if errorlevel 1 (
    echo 错误: 未检测到Java环境，请确保已安装JDK 17+
    echo 并正确配置JAVA_HOME环境变量
    echo 例如: set JAVA_HOME=C:\Program Files\Java\jdk-17
    pause
    exit /b 1
)

:: 获取Java版本
for /f tokens^=2-5^ delims^=.-_^ " %%j in ('java -version 2^>^&1') do (
    if "%%j" == "version" (
        set "JAVA_VERSION=%%k"
        goto :java_version_found
    )
)

:java_version_found
if %JAVA_VERSION% LSS 17 (
    echo 错误: Java版本过低，需要JDK 17+
    echo 当前版本: %JAVA_VERSION%
    pause
    exit /b 1
)

echo 检测到Java环境: %JAVA_VERSION%
echo.

:: 检查agent jar是否存在
if not exist "Naven-Modern-1337-agent.jar" (
    echo 错误: 未找到agent jar文件
    echo 请确保Naven-Modern-1337-agent.jar在当前目录
    pause
    exit /b 1
)

echo 1. 查找游戏进程...
echo 正在列出Java进程，请稍候...
echo.

:: 获取游戏进程ID
set "GAME_PID="
set "GAME_PROCESS="

for /f "tokens=1,2*" %%i in ('jps -l ^| findstr /i "minecraft forge"') do (
    if not defined GAME_PID (
        set "GAME_PID=%%i"
        set "GAME_PROCESS=%%j"
    )
)

if not defined GAME_PID (
    echo 错误: 未找到Minecraft游戏进程
    echo 请先启动游戏，然后再运行此脚本
    pause
    exit /b 1
)

echo 找到游戏进程:
echo PID: %GAME_PID%
echo 进程: %GAME_PROCESS%
echo.

:: 询问是否继续
echo 2. 附加热注入Agent...
echo 即将向游戏进程附加热注入agent
set /p "CONFIRM=是否继续? (y/n): "
if /i not "%CONFIRM%" == "y" (
    echo 操作已取消
    pause
    exit /b 0
)

echo.
echo 正在附加agent...

:: 附加agent到游戏进程
jattach %GAME_PID% load instrument false "%~dp0Naven-Modern-1337-agent.jar"

if errorlevel 1 (
    echo 错误: 附加agent失败
    echo 请检查是否有管理员权限，或尝试手动附加
    echo 手动命令: jattach %GAME_PID% load instrument false %~dp0Naven-Modern-1337-agent.jar
    pause
    exit /b 1
)

echo.
echo =================================
echo ✅ 热注入agent附加成功!
echo =================================
echo.
echo 🔍 热注入功能已启用，每5秒自动检测文件变化
echo 📁 类目录: ./mods/classes
echo 📝 日志将输出到游戏控制台
echo.
echo 热注入成功后，游戏控制台将显示:
echo "✅ 成功启用ClickGUIModule"
echo.
echo 使用说明:
echo 1. 修改代码后编译生成class文件
echo 2. 将class文件放入 ./mods/classes 目录
echo 3. 等待约5秒，agent将自动加载新类
echo 4. 使用快捷键打开ClickGUI
echo.
echo 按任意键退出...
pause
