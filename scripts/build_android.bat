@echo off
setlocal
set ROOT=%~dp0..
set ANDROID_DIR=%ROOT%\android
where gradle >nul 2>nul
if errorlevel 1 (
  echo Gradle not found. Install Android Studio or Gradle 8.9+ and retry.
  exit /b 1
)
if "%ANDROID_HOME%"=="" if "%ANDROID_SDK_ROOT%"=="" (
  echo Set ANDROID_HOME or ANDROID_SDK_ROOT to your Android SDK directory.
  exit /b 1
)
cd /d "%ANDROID_DIR%"
gradle :app:assembleDebug
if errorlevel 1 exit /b 1
echo APK: %ANDROID_DIR%\app\build\outputs\apk\debug\app-debug.apk
