@echo off
setlocal

rem Find the JAR file that matches the pattern
for %%f in (demo-*.jar) do set JAR_FILE=%%f

rem Check if the JAR file was found
if defined JAR_FILE (
    echo Running %JAR_FILE%...
    start "" cmd /c "title Merge Process & java -Xms1g -Xmx1g -jar "%JAR_FILE%""

    rem Wait for a specified amount of time (e.g., 10 seconds)
    echo Waiting for 5 seconds before opening Chrome...
    timeout /t 5 /nobreak >nul

    rem Open Chrome and navigate to the URL
    echo Opening Chrome and navigating to the URL...
    start "" "C:\Program Files\Google\Chrome\Application\chrome.exe" "http://localhost:8080/swagger-ui.html"
) else (
    echo No JAR file found matching the pattern.
)

pause