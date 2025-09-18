@echo off
setlocal

:: ============================================================================
:: [Orpheus TTS - Dynamic Compile & Run Script]
:: This script automatically finds a Java Development Kit (JDK) on your system
:: by searching the PATH for 'javac.exe'. It then uses that same JDK to
:: compile and run the application, ensuring version consistency.
:: ============================================================================

:: Configuration
set "SRC_DIR=src\Main"
set "BUILD_DIR=build"
set "MAIN_CLASS=ProjectOrpheusTTS"

echo.
:: FIXED: Escaped the '&' character with a '^' to prevent a command error.
echo [Orpheus TTS - Compile ^& Run Script]
echo ----------------------------------------

:: 1. Find the Java compiler (javac.exe)
echo Searching for a suitable JDK...
set "JAVAC_PATH="
for /f "delims=" %%a in ('where javac 2^>nul') do (
    set "JAVAC_PATH=%%a"
    goto :found_javac
)

:not_found_javac
echo ERROR: Could not find 'javac.exe' in your system's PATH.
echo Please ensure you have a Java Development Kit (JDK) installed and
echo that its 'bin' directory is included in your PATH environment variable.
goto :error

:found_javac
echo Found javac at: %JAVAC_PATH%

:: 2. Determine the JAVA_HOME directory from the found javac path
pushd "%JAVAC_PATH%\.."
pushd "..\."
set "JAVA_HOME=%CD%"
popd
popd

echo Using JDK located at: %JAVA_HOME%
echo.

:: 3. Validate that java.exe also exists in the same location
if not exist "%JAVA_HOME%\bin\java.exe" (
    echo ERROR: Found javac.exe, but java.exe is missing from the same JDK folder.
    echo Your JDK installation at "%JAVA_HOME%" might be corrupt.
    goto :error
)

:: 4. Clean and create build directory
echo Cleaning up previous build...
if exist "%BUILD_DIR%" (
    rmdir /s /q "%BUILD_DIR%"
)
mkdir "%BUILD_DIR%"

:: 5. Compile source files
echo Compiling source files...
:: FIXED: Changed directory into src\Main to compile more reliably.
pushd "%SRC_DIR%"
"%JAVA_HOME%\bin\javac.exe" -d "..\..\%BUILD_DIR%" *.java
popd
if %errorlevel% neq 0 (
    echo ERROR: Compilation failed.
    goto :error
)
echo Compilation successful.
echo.

:: 6. Run the application
echo Running application: %MAIN_CLASS%
echo ----------------------------------------
"%JAVA_HOME%\bin\java.exe" -cp "%BUILD_DIR%" %MAIN_CLASS%

echo.
echo ----------------------------------------
echo Application finished.

goto :end

:error
echo.
echo Script terminated due to an error.
pause

:end
endlocal