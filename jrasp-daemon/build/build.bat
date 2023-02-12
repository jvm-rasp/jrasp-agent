@echo off
setlocal

rem 设置阿里云代理
set "GOPROXY=https://mirrors.aliyun.com/goproxy"
go env -w GOPROXY=$GOPROXY
echo GOPROXY:%GOPROXY%

set now=%date:~0,4%-%date:~5,2%-%date:~8,2% %time:~0,2%:%time:~3,2%:%time:~6,2%
echo %now%

cd /d %~dp0
set "CURRENT_DIR=%cd%"
echo [JRASP INFO] CURRENT DIR: %CURRENT_DIR%

set KEY_VERSION=""

cd ..\..\bin\

for /f  %%a in (DECRYPT_KEY.txt) do (
    set KEY_VERSION=%%a
    goto SHOW_VERSION
)
:SHOW_VERSION
echo [JRASP INFO] JRASP KEY_VERSION: "%KEY_VERSION%"

cd %CURRENT_DIR%
cd ..\

rem 编译信息

for /f  %%a in ('go list -m') do (
    set moduleName=%%a
)

echo %moduleName%

@REM set commit= %git rev-parse --short HEAD%
for /f  %%a in ('git rev-parse --short HEAD') do (
    set commit=%%a
)
echo %commit%


@REM set branch= %git rev-parse --abbrev-ref HEAD%
for /f  %%a in ('git rev-parse --abbrev-ref HEAD') do (
    set branch=%%a
)
echo %branch%

set buildTime=%date:~0,4%%date:~5,2%%date:~8,2%%time:~0,2%%time:~3,2%%time:~6,2%
echo %buildTime%


rem TODO attach 工程 & windows编译脚本 同步完善
set buildDecryptKey=%KEY_VERSION%


rem environ.go 包名
set environPkgName=environ


@REM -X 'jrasp-daemon/environ.BuildGitBranch=master' -X 'jrasp-daemon/environ.BuildGitCommit=88146ea' -X 'jrasp-daemon/environ.BuildDateTime=202302080620' -X 'jrasp-daemon/environ.BuildDecryptKey=1234567890123456'
set flags=-"X '%moduleName%/%environPkgName%.BuildGitBranch=%branch%' -X '%moduleName%/%environPkgName%.BuildGitCommit=%commit%' -X '%moduleName%/%environPkgName%.BuildDateTime=%buildTime%' -X '%moduleName%/%environPkgName%.BuildDecryptKey=%buildDecryptKey%'"
echo %flags%

rem 编译 jrasp-daemon
cd %CURRENT_DIR%
cd ..\

@REM go build -o bin\jrasp-daemon.exe
go build -v -ldflags=%flags% -o bin\jrasp-daemon.exe


echo build go project end: %now%