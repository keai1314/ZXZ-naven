@echo off
chcp 65001 >nul
echo ================================
echo Minecraft热重载注入工具
echo ================================
echo.

echo 当前目录: %cd%
echo.

echo 正在查找Java进程...
echo.
java -jar build/libs/simple-injector.jar list
echo.

set /p pid=请输入要注入的进程PID: 
echo.
echo 你输入的PID是: %pid%
echo.

echo 正在执行注入...
java -jar build/libs/simple-injector.jar %pid% build/libs/simple-agent.jar
echo.

echo 如果注入成功，你应该能在Minecraft中看到变化。
echo 确保你的类文件已放置在 mods/classes 目录中。
echo.
pause