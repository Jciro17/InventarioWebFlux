@echo off
REM Inventario UCO en http://localhost:8081
IF EXIST "mvnw.cmd" (
    CALL mvnw.cmd spring-boot:run
) ELSE (
    mvn spring-boot:run
)
