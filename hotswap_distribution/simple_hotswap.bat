@echo off

rem �������ע��ű�
echo =====================
echo �������ע�빤��
echo =====================
echo.

rem ���Java�Ƿ����
java -version >nul 2>nul
if errorlevel 1 (
    echo ����: Java�����ã���ȷ���Ѱ�װJDK 17+
    pause
    exit /b 1
)

echo 1. Java��������
echo.

rem ���agent jar�Ƿ����
if not exist "Naven-Modern-1337-agent.jar" (
    echo ����: δ�ҵ�agent jar�ļ�
    pause
    exit /b 1
)

echo 2. agent jar�ļ�����
echo.

rem �г�����Java����
echo 3. �����г�Java����...
jps -l
echo.

rem �ֶ�����PID
set /p PID="��������Ϸ����PID: "

rem ���PID�Ƿ�Ϊ��
if "%PID%" == "" (
    echo ����: PID����Ϊ��
    pause
    exit /b 1
)

echo.
echo 4. ���ڸ�����ע��agent...

rem ���jattach�Ƿ����
jattach -h >nul 2>nul
if errorlevel 1 (
    echo ����: jattach�������
    echo ��� https://github.com/apangin/jattach/releases ����
    echo ����jattach.exe���ڵ�ǰĿ¼
    pause
    exit /b 1
)

echo ִ������: jattach %PID% load instrument false "Naven-Modern-1337-agent.jar"
echo.

rem ִ�и�������
jattach %PID% load instrument false "Naven-Modern-1337-agent.jar"

if errorlevel 1 (
    echo.
    echo ����: ����agentʧ��
    echo ����:
    echo 1. PID�Ƿ���ȷ
    echo 2. �Ƿ��й���ԱȨ��
    echo 3. ��Ϸ�Ƿ���������
    pause
    exit /b 1
)

echo.
echo =====================
echo ? �����ɹ���
echo =====================
echo.
echo ��ע�빦��������
echo ��Ŀ¼: ./mods/classes
echo ÿ5���Զ�����ļ��仯
echo.
echo ��������˳�...
pause
