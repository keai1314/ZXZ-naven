@echo off
echo Rebuilding simple-agent.jar...

REM 删除旧的jar文件
if exist "build\libs\simple-agent.jar" del "build\libs\simple-agent.jar"

REM 创建temp目录
if not exist "build\temp_classes" mkdir "build\temp_classes"

REM 编译Java类
echo Compiling Java classes...
javac -encoding UTF-8 src\main\java\com\heypixel\heypixelmod\obsoverlay\utils\SimpleAgentMain.java src\main\java\com\heypixel\heypixelmod\obsoverlay\utils\SimpleHotSwapManager.java -d build\temp_classes

REM 创建manifest文件
echo Creating manifest file...
echo Manifest-Version: 1.0 > new-manifest.mf
echo Can-Redefine-Classes: true >> new-manifest.mf
echo Can-Retransform-Classes: true >> new-manifest.mf
echo Agent-Class: com.heypixel.heypixelmod.obsoverlay.utils.SimpleAgentMain >> new-manifest.mf

REM 创建jar文件
echo Creating jar file...
jar cfm build\libs\simple-agent.jar new-manifest.mf -C build\temp_classes .

echo Done! Checking manifest...
jar -xf build\libs\simple-agent.jar META-INF/MANIFEST.MF
type META-INF\MANIFEST.MF

pause