@echo off
setlocal enabledelayedexpansion enableextensions

set TARGET=target
rd %TARGET% /S /Q

set DEPENDENCIES=
for %%x in (lib\*) do set DEPENDENCIES=!DEPENDENCIES!;%%x

cd lib
set LIBS=
for %%x in (*) do set LIBS=!LIBS! %%x
cd..

set LIST_FILE=%TARGET%\list
set MANIFEST=%TARGET%\manifest.mf
set CLASSES=%TARGET%\classes
set LIB=%TARGET%\lib
set ARTIFACT=%LIB%\reviewanalyzer.jar
mkdir %TARGET%
mkdir %CLASSES%
mkdir %LIB%

dir src /s /b /a-d > %LIST_FILE%
dir test\com\roundforest\MainTest.java /s /B >> %LIST_FILE%

echo Manifest-Version: 1.0 > %MANIFEST%
echo Class-Path: %LIBS% >> %MANIFEST%
echo Main-Class: com.roundforest.MainTest >> %MANIFEST%

javac -cp %DEPENDENCIES%;%CLASSES% -d %CLASSES% @%LIST_FILE%

jar cfm %ARTIFACT% %MANIFEST% -C %CLASSES% .

copy lib\*.jar %LIB%

echo Build finished
choice /c yn /m "Do you want to run application?"
if errorlevel 2 goto ex
java -Xmx512M -jar %ARTIFACT%

:ex
pause