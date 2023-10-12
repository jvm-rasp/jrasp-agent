@echo off
rem windowsϵͳ�±���jrasp-agent
rem �������ű��� win10 x86-64��������������windows���������ܴ��ڲ����ݣ�����
setlocal

rem Guess JRASP_HOME
rem ��ǰ��ִ��bat�ļ���·��
cd /d %~dp0
set "CURRENT_DIR=%cd%"
echo [JRASP INFO] CURRENT DIR: %CURRENT_DIR%

cd ..
set "JRASP_HOME=%cd%"
echo [JRASP INFO] PROJECT HOME: %JRASP_HOME%
cd "%CURRENT_DIR%"

set VERSION=""
rem �жϰ汾�ļ��Ƿ����
echo [JRASP INFO] read VERSION.txt
if not exist "%CURRENT_DIR%\VERSION.txt" (
    echo [JRASP ERROR] VERSION.txt not exist!
    goto end
)

for /f  %%a in (VERSION.txt) do (
    set VERSION=%%a
    goto SHOW_VERSION
)
:SHOW_VERSION

echo [JRASP INFO] JRASP VERSION: "%VERSION%"

rem ����Java agent����
echo [JRASP INFO] mvn clean package jrasp-agent start...
call mvn clean package -Dmaven.test.skip=false -f ..\pom.xml

if not %errorlevel% == 0 (
    echo [JRASP ERROR] mvn clean package jrasp-agent error��
    goto end
)
echo [JRASP INFO] mvn clean package jrasp-agent success.

rem ����GoLang-����
rem echo [JRASP INFO] go build jrasp-attach start...
rem call %JRASP_HOME%\jrasp-attach\build\build.bat

if not %errorlevel% == 0 (
    echo [JRASP ERROR] go build jrasp-attach error!
    goto end
)

rem �ж��ļ��Ƿ�����
rem echo [JRASP INFO] go build jrasp-attach end.
rem if not exist "%JRASP_HOME%\jrasp-attach\bin\attach.exe" (
rem     echo [JRASP ERROR] go build jrasp-attach error: attach.exe not exist!
rem     goto end
rem )

rem ����GoLang����
echo [JRASP INFO] go build jrasp-daemon start...
call %JRASP_HOME%\jrasp-daemon\build\build.bat

if not %errorlevel% == 0 (
    echo [JRASP ERROR] go build jrasp-daemon error!
    goto end
)

rem �ж��ļ��Ƿ�����
echo [JRASP INFO] go build jrasp-daemon end.
if not exist "%JRASP_HOME%\jrasp-daemon\bin\jrasp-daemon.exe" (
    echo [JRASP ERROR] go build jrasp-daemon error: jrasp-daemon.exe not exist!
    goto end
)

rem �����ļ���
set "JRASP_PACKAGE_HOME=%JRASP_HOME%\target\jrasp"
md %JRASP_PACKAGE_HOME%
if not exist %JRASP_PACKAGE_HOME% (
    echo [JRASP ERROR] mkdir JRASP PACKAGE HOME error: %JRASP_PACKAGE_HOME% not exist!
    goto end
)
echo [JRASP INFO] JRASP PACKAGE HOME: %JRASP_PACKAGE_HOME%

rem ������Ŀ¼
cd %JRASP_PACKAGE_HOME%
set "BIN_DIR=%JRASP_PACKAGE_HOME%\bin"
set "LIB_DIR=%JRASP_PACKAGE_HOME%\lib"
set "MODULE_DIR=%JRASP_PACKAGE_HOME%\module"
set "CFG_DIR=%JRASP_PACKAGE_HOME%\config"
set "LOGS_DIR=%JRASP_PACKAGE_HOME%\logs"
set "RUN_DIR=%JRASP_PACKAGE_HOME%\run"
set "TMP_DIR=%JRASP_PACKAGE_HOME%\tmp"
md  %BIN_DIR% %LIB_DIR% %MODULE_DIR% %CFG_DIR% %LOGS_DIR% %RUN_DIR% %TMP_DIR%

rem �ļ�����
rem agent jar����

echo f | xcopy /s /f "%JRASP_HOME%\jrasp-bridge\target\jrasp-bridge-"%VERSION%".jar" "%LIB_DIR%\jrasp-bridge-"%VERSION%".jar"
echo f | xcopy /s /f "%JRASP_HOME%\jrasp-core\target\jrasp-core-"%VERSION%".jar" "%LIB_DIR%\jrasp-core-"%VERSION%".jar"
echo f | xcopy /s /f "%JRASP_HOME%\jrasp-launcher\target\jrasp-launcher-"%VERSION%".jar" "%LIB_DIR%\jrasp-launcher-"%VERSION%".jar"

rem module jar����
rem ��ǰ·���Լ���·���¸���jar��
for /r %JRASP_HOME%\jrasp-module %%i in (*-encrypted.jar) do copy %%i "%MODULE_DIR%"

rem  jrasp-attach����
rem copy "%JRASP_HOME%\jrasp-attach\bin\attach.exe" "%BIN_DIR%"

rem  jattach����
copy "%JRASP_HOME%\tools\jattach.exe" "%BIN_DIR%"

rem  config.json ����
copy "%JRASP_HOME%\jrasp-daemon\config\config.json" "%CFG_DIR%"

rem  jrasp-daemon����
copy "%JRASP_HOME%\jrasp-daemon\bin\jrasp-daemon.exe" "%BIN_DIR%"

rem VERSION.txt
copy "%CURRENT_DIR%\VERSION.txt"  "%JRASP_PACKAGE_HOME%"



rem  windowsϵͳ�� ��������������zip
if not exist %JRASP_HOME%\tools\7z.exe (
    echo [JRASP ERROR] 7z.exe not exist!
    goto end
)

Echo zipping...
%JRASP_HOME%\tools\7z.exe a "%JRASP_HOME%\target\jrasp-%VERSION%-windows-release.zip" %JRASP_PACKAGE_HOME%


:end

echo [JRASP INFO] JRASP PACKAGE END. %DATE%
