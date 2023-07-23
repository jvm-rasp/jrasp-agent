@echo off
setlocal
rem Брвы jrasp-attach
cd /d %~dp0
cd ..
go build -o bin\attach.exe
