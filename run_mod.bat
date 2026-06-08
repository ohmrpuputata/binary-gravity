@echo off
title Alien Apocalypse Launcher
echo Starting Launcher...

set "JAVA21=C:\Program Files\Microsoft\jdk-21.0.10.7-hotspot\bin\java.exe"
if exist "%JAVA21%" (
    set "JAVA_EXE=%JAVA21%"
) else (
    set "JAVA_EXE=java"
)

REM Check Java
echo Checking for Java...
"%JAVA_EXE%" -version >nul 2>nul
if %errorlevel% neq 0 (
    echo [ERROR] Java is not installed or not in PATH!
    echo Please install JDK 21.
    pause
    exit /b
)

REM Create dir
if not exist "gradle\wrapper" mkdir "gradle\wrapper"

REM Download wrapper if missing
if not exist "gradle\wrapper\gradle-wrapper.jar" (
    echo [INFO] Downloading Gradle Wrapper...
    
    REM Try curl first (Windows 10+)
    curl -L -o "gradle\wrapper\gradle-wrapper.jar" "https://raw.githubusercontent.com/spring-projects/spring-boot/main/gradle/wrapper/gradle-wrapper.jar"
    
    if not exist "gradle\wrapper\gradle-wrapper.jar" (
        echo [WARNING] Curl failed, trying PowerShell...
        powershell -Command "Invoke-WebRequest -Uri 'https://raw.githubusercontent.com/spring-projects/spring-boot/main/gradle/wrapper/gradle-wrapper.jar' -OutFile 'gradle\wrapper\gradle-wrapper.jar'"
    )

    if not exist "gradle\wrapper\gradle-wrapper.jar" (
        echo [ERROR] Failed to download gradle-wrapper.jar!
        pause
        exit /b
    )
)

echo [INFO] Launching Minecraft...
echo [INFO] This might take a few minutes...

"%JAVA_EXE%" -cp gradle\wrapper\gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain runClient

if %errorlevel% neq 0 (
    echo.
    echo [ERROR] The game crashed or failed to build.
    echo [TIP] If it says "Compilation failed", you might need to run this again.
)

echo.
echo Application finished.
pause
