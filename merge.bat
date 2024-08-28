@echo off

set /p FILES_FOLDER="FILES_FOLDER: "

java -Xms1g -Xmx1g -jar "demo-1.0-SNAPSHOT.jar" %FILES_FOLDER%

pause