@echo off
setlocal

set "JAVA21=C:\Program Files\Microsoft\jdk-21.0.10.7-hotspot\bin\java.exe"
if defined JAVA_HOME (
    set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
) else if exist "%JAVA21%" (
    set "JAVA_EXE=%JAVA21%"
) else (
    set "JAVA_EXE=java"
)

"%JAVA_EXE%" -cp "%~dp0gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %*
exit /b %errorlevel%
