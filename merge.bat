@echo off
setlocal enabledelayedexpansion

rem Find the JAR file that matches the pattern
set "JAR_FILE="
for %%f in (demo-*.jar) do set "JAR_FILE=%%f"

rem Check if the JAR file was found
if defined JAR_FILE (
    echo Running !JAR_FILE!...
    start "" cmd /k "title Merge Process & java -Xms1g -Xmx1g -jar !JAR_FILE!"

    rem Wait for a specified amount of time (e.g., 5 seconds)
    echo Waiting for 5 seconds before opening Chrome...
    timeout /t 5 /nobreak >nul

    rem Find the Chrome executable
    set "CHROME_PATH="
    for %%d in (
        "C:\Program Files\Google\Chrome\Application\chrome.exe"
        "C:\Program Files (x86)\Google\Chrome\Application\chrome.exe"
        "%LOCALAPPDATA%\Google\Chrome\Application\chrome.exe"
    ) do (
        if exist "%%d" (
            rem Remove the first and last character
            set "x=%%d"
            set "CHROME_PATH=!x:~1,-1!"
            echo Debug: Found Chrome Path !CHROME_PATH!
            goto :foundChrome
        )
        echo Debug: Path not found: %%d
    )

    rem If no Chrome path is found
    echo Chrome executable not found.
    goto :end
) else (
    echo No JAR file found matching the pattern.
    goto :end
)

:foundChrome
echo Debug: Final CHROME_PATH=!CHROME_PATH!
if defined CHROME_PATH (
    echo Chrome executable found at: !CHROME_PATH!
    echo Opening Chrome and navigating to the URL...
    start "" "!CHROME_PATH!" "http://localhost:8080/swagger-ui.html"
) else (
    echo Chrome executable not found.
)

:end
pause