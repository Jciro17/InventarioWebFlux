@REM Maven Wrapper CMD Script
@IF EXIST "%~dp0.mvn\wrapper\maven-wrapper.jar" GOTO OkJarPresent

@ECHO Descargando Maven Wrapper JAR...
@SET MVNW_JAR_URL=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar
powershell -Command "(New-Object Net.WebClient).DownloadFile('!MVNW_JAR_URL!', '%~dp0.mvn\wrapper\maven-wrapper.jar')"

:OkJarPresent
@SET JAVA_EXE=%JAVA_HOME%\bin\java.exe
@IF NOT EXIST "%JAVA_EXE%" SET "JAVA_EXE=java"
@"%JAVA_EXE%" -classpath "%~dp0.mvn\wrapper\maven-wrapper.jar" org.apache.maven.wrapper.MavenWrapperMain %*
