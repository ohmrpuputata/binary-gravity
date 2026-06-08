@echo off
title Alien Apocalypse Builder
echo Starting Build Process...

set "JAVA21=C:\Program Files\Microsoft\jdk-21.0.10.7-hotspot\bin\java.exe"
if exist "%JAVA21%" (
    set "JAVA_EXE=%JAVA21%"
) else (
    set "JAVA_EXE=java"
)

REM Check Java
"%JAVA_EXE%" -version >nul 2>nul
if %errorlevel% neq 0 (
    echo [ERROR] Java is not installed!
    pause
    exit /b
)

REM Download wrapper if missing (reusing logic)
if not exist "gradle\wrapper\gradle-wrapper.jar" (
    echo [INFO] Downloading Gradle Wrapper...
    curl -L -o "gradle\wrapper\gradle-wrapper.jar" "https://raw.githubusercontent.com/spring-projects/spring-boot/main/gradle/wrapper/gradle-wrapper.jar"
)

echo [INFO] Building Mod (Fabric)...
echo [INFO] This will generate a JAR file in build/libs/
echo.

"%JAVA_EXE%" -cp gradle\wrapper\gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain build

if %errorlevel% neq 0 (
    echo.
    echo [ERROR] Build failed! Check the errors above.
) else (
    echo.
    echo [SUCCESS] Build complete!
    echo You can find your mod file in: build\libs\
    dir build\libs\*.jar
)

pause
