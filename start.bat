@echo off
setlocal
set "SCRIPT_DIR=%~dp0"
set "LOCAL_MAVEN=%TEMP%\apache-maven-3.9.5\bin\mvn.cmd"
set "MAVEN_ZIP=%TEMP%\maven.zip"
set "LAUNCH_LOG=%LOCALAPPDATA%\LabFlow\data\logs\launcher.log"

echo ================================
echo  LabFlow - Equipment Management
echo ================================
echo.

if not defined LABFLOW_AI_API_KEY if defined GEMINI_API_KEY set "LABFLOW_AI_API_KEY=%GEMINI_API_KEY%"
if exist "ai.env" (
    for /f "usebackq tokens=1,* delims==" %%A in ("ai.env") do (
        if /I "%%~A"=="LABFLOW_AI_API_KEY" set "LABFLOW_AI_API_KEY=%%~B"
        if /I "%%~A"=="LABFLOW_AI_BASE_URL" set "LABFLOW_AI_BASE_URL=%%~B"
        if /I "%%~A"=="LABFLOW_AI_MODEL" set "LABFLOW_AI_MODEL=%%~B"
    )
)
if not defined LABFLOW_AI_BASE_URL set "LABFLOW_AI_BASE_URL=https://generativelanguage.googleapis.com/v1beta/openai/"
if not defined LABFLOW_AI_MODEL set "LABFLOW_AI_MODEL=gemini-2.5-flash"

pushd "%SCRIPT_DIR%" >nul 2>nul
if errorlevel 1 (
    echo [ERROR] Could not open project folder:
    echo %SCRIPT_DIR%
    echo.
    pause
    exit /b 1
)

if not exist "pom.xml" (
    echo [ERROR] pom.xml was not found in:
    cd
    echo.
    pause
    popd
    exit /b 1
)

where java >nul 2>nul
if errorlevel 1 (
    echo [ERROR] Java was not found on PATH.
    echo Install JDK 21 or add Java to PATH, then run this file again.
    echo.
    pause
    popd
    exit /b 1
)

if exist "%LOCAL_MAVEN%" (
    set "MVN_CMD=%LOCAL_MAVEN%"
) else (
    for /f "delims=" %%M in ('where mvn.cmd 2^>nul') do if not defined MVN_CMD set "MVN_CMD=%%M"
)

if not defined MVN_CMD (
    echo [INFO] Maven not found. Downloading Maven 3.9.5...
    powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; [Net.ServicePointManager]::SecurityProtocol = [Net.ServicePointManager]::SecurityProtocol -bor [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri 'https://archive.apache.org/dist/maven/maven-3/3.9.5/binaries/apache-maven-3.9.5-bin.zip' -OutFile '%MAVEN_ZIP%' -UseBasicParsing; Expand-Archive -Force '%MAVEN_ZIP%' -DestinationPath '%TEMP%'; Remove-Item '%MAVEN_ZIP%' -Force"
    if errorlevel 1 (
        echo.
        echo [ERROR] Maven download failed.
        echo Check your internet connection or install Maven manually.
        echo.
        pause
        popd
        exit /b 1
    )
    set "MVN_CMD=%LOCAL_MAVEN%"
)

if not exist "%MVN_CMD%" (
    echo [ERROR] Maven executable was not found:
    echo %MVN_CMD%
    echo.
    pause
    popd
    exit /b 1
)

if not defined LABFLOW_AI_API_KEY (
    echo [WARN] LABFLOW_AI_API_KEY is not configured.
    echo        AI Helper will stay offline until you set it in ai.env or as an environment variable.
    echo.
)

if not exist "%LOCALAPPDATA%\LabFlow\data\logs" mkdir "%LOCALAPPDATA%\LabFlow\data\logs" >nul 2>nul

echo [INFO] Launching LabFlow...
echo [INFO] The launcher window will close automatically.
echo [INFO] If startup fails, check the app log in:
echo        %LOCALAPPDATA%\LabFlow\data\logs\labflow.log

powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$mvn = '%MVN_CMD%';" ^
  "$workdir = '%SCRIPT_DIR%';" ^
  "Start-Process -FilePath $mvn -ArgumentList '-q','-DskipTests','javafx:run' -WorkingDirectory $workdir -WindowStyle Hidden | Out-Null"
if errorlevel 1 (
    echo.
    echo [ERROR] LabFlow could not be launched.
    echo.
    pause
    popd
    exit /b 1
)

popd
exit /b 0
