@echo off
setlocal

rem Find the JAR file that matches the pattern
for %%f in (demo-*.jar) do set JAR_FILE=%%f

rem Check if the JAR file was found
if defined JAR_FILE (
    echo Running %JAR_FILE%...
    java -Xms1g -Xmx1g -jar "%JAR_FILE%"
) else (
    echo No JAR file found matching the pattern.
)

pause