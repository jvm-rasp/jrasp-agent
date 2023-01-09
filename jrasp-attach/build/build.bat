@echo off
setlocal
rem 编译 jrasp-attach
cd /d %~dp0
cd ..
go build -o bin\attach.exe
