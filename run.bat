@echo off
REM Inventario UCO en http://localhost:8081
REM Antes define MESSAGEUCOLAB_APPLICATION_ID y MESSAGEUCOLAB_ENVIRONMENT_ID (ver README.md)
IF "%MESSAGEUCOLAB_APPLICATION_ID%"=="" ECHO [AVISO] Falta MESSAGEUCOLAB_APPLICATION_ID: los mensajes usaran el texto de respaldo
IF "%MESSAGEUCOLAB_ENVIRONMENT_ID%"=="" ECHO [AVISO] Falta MESSAGEUCOLAB_ENVIRONMENT_ID: los mensajes usaran el texto de respaldo
IF EXIST "mvnw.cmd" (
    CALL mvnw.cmd spring-boot:run
) ELSE (
    mvn spring-boot:run
)
