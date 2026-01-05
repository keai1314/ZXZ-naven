@echo off
echo Creating mod agent jar...

REM 删除旧的jar文件
if exist build\libs\mod-inject-agent.jar del build\libs\mod-inject-agent.jar

REM 创建新的jar文件
jar cfm build\libs\mod-inject-agent.jar mod-inject-agent.mf -C build\temp_classes .

echo Jar file created successfully!
pause