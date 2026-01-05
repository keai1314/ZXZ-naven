@echo off
cls
echo =====================
echo    HOTSWAP FIXED
echo =====================
echo.

echo 1. 检查agent jar文件...
if not exist "Naven-Modern-1337-agent.jar" (
    echo 错误: 未找到agent jar文件
    pause
    exit /b 1
)

echo agent jar: OK
echo.

echo 2. 正在列出Java进程...
jps -l
echo.

set /p PID="请输入游戏进程PID: "
if "%PID%" == "" (
    echo 错误: PID不能为空!
    pause
    exit /b 1
)

echo.
echo 3. 检查jattach...
where jattach >nul 2>nul
if errorlevel 1 (
    echo 错误: jattach.exe not found!
    echo 请从 https://github.com/apangin/jattach/releases 下载
    echo 并将jattach.exe放在当前目录
    pause
    exit /b 1
)

echo jattach: OK
echo.

echo 4. 正在附加热注入agent...
echo 命令: jattach %PID% load instrument false "Naven-Modern-1337-agent.jar"
echo.

rem 执行热注入
jattach %PID% load instrument false "Naven-Modern-1337-agent.jar"

echo.
echo 5. 操作完成!
echo.
echo 请检查游戏控制台日志
echo 热注入成功后会显示:
echo "✅ 成功启用ClickGUIModule"
echo.
echo 按任意键退出...
pause
