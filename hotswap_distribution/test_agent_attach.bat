@echo off
setlocal enabledelayedexpansion

echo =================================
echo      ��ע��Agent���Ӳ���
set "JAVA_HOME=%JAVA_HOME:~0,-1%"
echo =================================
echo.

:: ���agent jar�Ƿ����
if not exist "Naven-Modern-1337-agent.jar" (
    echo ����: δ�ҵ�agent jar�ļ�
    pause
    exit /b 1
)

echo 1. ���Ҳ��Խ���...

:: �г�Java����
jps -l

echo.
echo 2. ����agent����...
echo ���ڲ���agent���ӹ���...
echo.

:: ֱ�Ӳ���agent���ӣ�ʹ��һ�������ڵ�PID�������������ʽ��
echo ���������ʽ:
jattach 12345 load instrument false "Naven-Modern-1337-agent.jar"
echo.

echo 3. ���ű�����...
echo �ű�����ȷ���ã�����ʹ�������������ע��agent:
echo jattach ^<PID^> load instrument false "Naven-Modern-1337-agent.jar"
echo.
echo 4. �������!
echo =================================
echo ��ע�빤�߰���׼������!
echo =================================
echo.
echo �������ļ�:
echo - Naven-Modern-1337-agent.jar  (��ע��agent)
echo - start_hotswap.bat           (�����ű�)
echo - ��ע��ʹ��˵��.md           (ʹ��˵��)
echo.
echo ʹ�÷���:
echo 1. ������Ϸ
2. ����start_hotswap.bat
3. ������ʾ����
4. ��������class�ļ�����./mods/classesĿ¼
echo 5. �ȴ�Լ5�룬agent���Զ���������
echo.
echo ��������˳�...
pause
