@echo off
setlocal

REM Forge 1.8.9 + ForgeGradle 2.1 requires Java 8.
if not defined JAVA8_HOME (
  echo ERROR: JAVA8_HOME is not set.
  echo.
  echo Set JAVA8_HOME to your JDK 8 folder, for example:
  echo   setx JAVA8_HOME "C:\Program Files\Java\jdk1.8.0_202"
  echo.
  echo Then run this script again.
  exit /b 1
)

if not exist "%JAVA8_HOME%\bin\java.exe" (
  echo ERROR: JAVA8_HOME is invalid: %JAVA8_HOME%
  echo Expected to find: %JAVA8_HOME%\bin\java.exe
  exit /b 1
)

set "JAVA_HOME=%JAVA8_HOME%"
echo Using JAVA_HOME=%JAVA_HOME%
call "%~dp0gradlew.bat" %*

