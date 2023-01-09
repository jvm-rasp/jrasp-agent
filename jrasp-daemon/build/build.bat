@echo off
setlocal

rem 设置阿里云代理
set "GOPROXY=https://mirrors.aliyun.com/goproxy"
go env -w GOPROXY=$GOPROXY
echo GOPROXY:%GOPROXY%

rem 编译 jrasp-daemon
cd /d %~dp0
cd ..

go build -o bin\jrasp-daemon.exe