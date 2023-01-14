@echo off
rem windows系统下编译jrasp-agent
rem 编译打包脚本在 win10 x86-64环境开发，其他windows环境，可能存在不兼容！！！
setlocal

rem Guess JRASP_HOME
rem 当前所执行bat文件的路径
cd /d %~dp0
set "CURRENT_DIR=%cd%"
echo [JRASP INFO] CURRENT DIR: %CURRENT_DIR%

cd ..
set "JRASP_HOME=%cd%"
echo [JRASP INFO] PROJECT HOME: %JRASP_HOME%
cd "%CURRENT_DIR%"

set VERSION=""
rem 判断版本文件是否存在
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

rem 编译Java agent工程
echo [JRASP INFO] mvn clean package jrasp-agent start...
call mvn clean package -Dmaven.test.skip=false -f ..\pom.xml

if not %errorlevel% == 0 (
    echo [JRASP ERROR] mvn clean package jrasp-agent error！
    goto end
)
echo [JRASP INFO] mvn clean package jrasp-agent success.

rem 编译GoLang-工程
echo [JRASP INFO] go build jrasp-attach start...
call %JRASP_HOME%\jrasp-attach\build\build.bat

if not %errorlevel% == 0 (
    echo [JRASP ERROR] go build jrasp-attach error!
    goto end
)

rem 判断文件是否生成
echo [JRASP INFO] go build jrasp-attach end.
if not exist "%JRASP_HOME%\jrasp-attach\bin\attach.exe" (
    echo [JRASP ERROR] go build jrasp-attach error: attach.exe not exist!
    goto end
)

rem 编译GoLang工程
echo [JRASP INFO] go build jrasp-daemon start...
call %JRASP_HOME%\jrasp-daemon\build\build.bat

if not %errorlevel% == 0 (
    echo [JRASP ERROR] go build jrasp-daemon error!
    goto end
)

rem 判断文件是否生成
echo [JRASP INFO] go build jrasp-daemon end.
if not exist "%JRASP_HOME%\jrasp-daemon\bin\jrasp-daemon.exe" (
    echo [JRASP ERROR] go build jrasp-daemon error: jrasp-daemon.exe not exist!
    goto end
)

rem 创建文件夹
set "JRASP_PACKAGE_HOME=%JRASP_HOME%\target\jrasp"
md %JRASP_PACKAGE_HOME%
if not exist %JRASP_PACKAGE_HOME% (
    echo [JRASP ERROR] mkdir JRASP PACKAGE HOME error: %JRASP_PACKAGE_HOME% not exist!
    goto end
)
echo [JRASP INFO] JRASP PACKAGE HOME: %JRASP_PACKAGE_HOME%

rem 创建子目录
cd %JRASP_PACKAGE_HOME%
set "BIN_DIR=%JRASP_PACKAGE_HOME%\bin"
set "LIB_DIR=%JRASP_PACKAGE_HOME%\lib"
set "MODULE_DIR=%JRASP_PACKAGE_HOME%\module"
set "CFG_DIR=%JRASP_PACKAGE_HOME%\config"
set "LOGS_DIR=%JRASP_PACKAGE_HOME%\logs"
set "RUN_DIR=%JRASP_PACKAGE_HOME%\run"
set "TMP_DIR=%JRASP_PACKAGE_HOME%\tmp"
md  %BIN_DIR% %LIB_DIR% %MODULE_DIR% %CFG_DIR% %LOGS_DIR% %RUN_DIR% %TMP_DIR%

rem 文件复制
rem agent jar复制

echo f | xcopy /s /f "%JRASP_HOME%\jrasp-bridge\target\jrasp-bridge-"%VERSION%".jar" "%LIB_DIR%\jrasp-bridge-"%VERSION%".jar"
echo f | xcopy /s /f "%JRASP_HOME%\jrasp-core\target\jrasp-core-"%VERSION%".jar" "%LIB_DIR%\jrasp-core-"%VERSION%".jar"
echo f | xcopy /s /f "%JRASP_HOME%\jrasp-launcher\target\jrasp-launcher-"%VERSION%".jar" "%LIB_DIR%\jrasp-launcher-"%VERSION%".jar"

rem module jar复制
rem 当前路径以及子路径下复制jar包
for /r %JRASP_HOME%\jrasp-module %%i in (*.jar) do copy %%i "%MODULE_DIR%"

rem  jrasp-attach复制
copy "%JRASP_HOME%\jrasp-attach\bin\attach.exe" "%BIN_DIR%"

rem  jattach复制
copy "%JRASP_HOME%\tools\jattach.exe" "%BIN_DIR%"

rem  config.json 复制
copy "%JRASP_HOME%\jrasp-daemon\config\config.json" "%CFG_DIR%"

rem  jrasp-daemon复制
copy "%JRASP_HOME%\jrasp-daemon\bin\jrasp-daemon.exe" "%BIN_DIR%"

rem VERSION.txt
copy "%CURRENT_DIR%\VERSION.txt"  "%JRASP_PACKAGE_HOME%"



rem  windows系统下 打包整体编译结果至zip
if not exist %JRASP_HOME%\tools\7z.exe (
    echo [JRASP ERROR] 7z.exe not exist!
    goto end
)

Echo zipping...
%JRASP_HOME%\tools\7z.exe a "%JRASP_HOME%\target\jrasp-windows-release.zip" %JRASP_PACKAGE_HOME%


:end

echo [JRASP INFO] JRASP PACKAGE END. %DATE%
