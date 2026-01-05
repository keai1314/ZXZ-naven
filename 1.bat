@echo off
echo 正在查找Java进程...
java -jar build/libs/simple-injector.jar list
echo.
set /p pid=请输入要注入的进程PID: 
java -jar build/libs/simple-injector.jar %pid% build/libs/simple-agent.jar
pause