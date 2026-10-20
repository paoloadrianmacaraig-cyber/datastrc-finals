@echo off
title Kingpin MNL DSA Verification Test Runner
echo ===================================================
echo   Compiling Kingpin MNL Test Suite...
echo ===================================================
if not exist "bin" mkdir bin
javac -d bin src\com\kingpin\model\*.java src\com\kingpin\dsa\*.java src\com\kingpin\util\*.java src\com\kingpin\service\*.java src\com\kingpin\*.java

if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Compilation failed.
    pause
    exit /b %ERRORLEVEL%
)

echo ===================================================
echo   Running Tests...
echo ===================================================
java -cp bin com.kingpin.TestRunner
pause
