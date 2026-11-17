@echo off
chcp 65001 >nul
cd /d "%~dp0"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0executar.ps1" -Acao compilar
if errorlevel 1 pause
