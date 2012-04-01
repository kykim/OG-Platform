@ECHO OFF

REM PLAT-1527
PUSHD %~dp0\..

set BASEDIR=%cd%
set SCRIPTDIR=%BASEDIR%\scripts
set PROJECT=og-bloombergexample
set PROJECTJAR=%PROJECT%.jar
set LOGBACK_CONFIG=jetty-logback.xml
set CONFIG=config\fullstack\bloombergexample-bin.properties

IF NOT EXIST %BASEDIR%\temp\hsqldb goto :nodb 

IF "%JAVA_HOME%" == "" ECHO Warning: JAVA_HOME is not set
SET JAVACMD=%JAVA_HOME%\bin\java.exe

IF "%1"=="start" goto :start
IF "%1"=="debug" goto :start
IF "%1"=="restart" goto :start
IF "%1"=="reload" goto :start
IF "%1"=="stop" ECHO Stop not supported
IF "%1"=="status" ECHO Status not supported
IF "%1"=="" ECHO Usage: %0 start^|stop^|restart^|status^|reload^|debug
GOTO :exit

:nodb
ECHO ERROR: The %PROJECT% database could not be found.
ECHO Please run %SCRIPTDIR%\init-%PROJECT%-db.bat to create and populate the database.
ECHO Exiting immediately...
GOTO :exit

:start
"%JAVACMD%" ^
  -Xms1024m -Xmx3072m -XX:MaxPermSize=256M -XX:+UseConcMarkSweepGC ^
  -XX:+CMSIncrementalMode -XX:+CMSIncrementalPacing ^
  -Dlogback.configurationFile=%LOGBACK_CONFIG% ^
        -Dcommandmonitor.secret=OpenGamma ^
  -cp config\*;%PROJECTJAR%;lib\* ^
  com.opengamma.component.OpenGammaComponentServer ^
  %CONFIG%
GOTO :exit

:exit
REM PLAT-1527
POPD
EXIT /B