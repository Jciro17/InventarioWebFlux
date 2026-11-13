@echo off
REM Inventario UCO en http://localhost:8081
REM Lee la configuracion del archivo .env (copia .env.example como .env y completa tus valores).
REM Requiere Java 25 y Maven (mvn) en el PATH.

IF NOT EXIST ".env" (
    ECHO [ERROR] Falta el archivo .env. Copia .env.example como .env y completa tus valores.
    EXIT /B 1
)

REM Carga cada linea CLAVE=VALOR del .env; ignora comentarios (#) y valores vacios.
FOR /F "usebackq eol=# tokens=1,* delims==" %%A IN (".env") DO (
    IF NOT "%%B"=="" SET "%%A=%%B"
)

IF "%MESSAGEUCOLAB_CLIENT_ID%"=="" (
    ECHO [ERROR] Falta MESSAGEUCOLAB_CLIENT_ID en el archivo .env.
    EXIT /B 1
)
IF "%MESSAGEUCOLAB_CLIENT_SECRET%"=="" (
    ECHO [ERROR] Falta MESSAGEUCOLAB_CLIENT_SECRET en el archivo .env.
    EXIT /B 1
)

mvn spring-boot:run
