@echo off
REM Build Schellmonitor and deploy to Algoma cluster server plugins folder
mvn package -Ddeploy.dir=M:\AlgomaClusterServer\plugins
if %ERRORLEVEL% NEQ 0 exit /b %ERRORLEVEL%
echo.
echo Done. JAR deployed to M:\AlgomaClusterServer\plugins\Schellmonitor-1.0.0.jar
