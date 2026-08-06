@echo off
setlocal enabledelayedexpansion
set "BASE_DIR=%~dp0"
for /f "tokens=1,* delims==" %%A in (%BASE_DIR%.mvn\wrapper\maven-wrapper.properties) do if "%%A"=="distributionUrl" set "DIST_URL=%%B"
if not defined DIST_URL (
  echo Missing distributionUrl 1>&2
  exit /b 1
)
for %%F in (%DIST_URL%) do set "ARCHIVE_NAME=%%~nxF"
set "VERSION=%ARCHIVE_NAME:apache-maven-=%"
set "VERSION=%VERSION:-bin.zip=%"
set "CACHE_ROOT=%USERPROFILE%\.m2\wrapper\dists"
set "MAVEN_HOME=%CACHE_ROOT%\apache-maven-%VERSION%"
if not exist "%MAVEN_HOME%\bin\mvn.cmd" (
  if not exist "%CACHE_ROOT%" mkdir "%CACHE_ROOT%"
  powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; $zip=Join-Path $env:TEMP 'zip-github-maven.zip'; Invoke-WebRequest -Uri '%DIST_URL%' -OutFile $zip; Expand-Archive -Force $zip '%CACHE_ROOT%'; Remove-Item $zip"
  if errorlevel 1 exit /b 1
)
call "%MAVEN_HOME%\bin\mvn.cmd" -f "%BASE_DIR%pom.xml" %*
exit /b %ERRORLEVEL%
