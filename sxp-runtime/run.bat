@echo off
VER 
echo SXP-CORE [2014,%PROCESSOR_ARCHITECTURE%]
echo.                                           

rem ---------- LIBRARIES ----------

set LIB=lib

set LOG_PATH=%LIB%\slf4j-api-1.7.7.jar;%LIB%\slf4j-simple-1.7.7.jar

set NETTY_PATH=%LIB%\netty-all-4.0.21.Final.jar
        
set SWT_PATH=%LIB%\swt.jar

set TCPMD5PATH=%LIB%\tcpmd5-api-1.0.0-20140910.150508-19.jar;%LIB%\tcpmd5-jni-1.0.0-20140910.150512-19.jar;%LIB%\tcpmd5-netty-1.0.0-20140910.150515-19.jar

rem ---------- APPLICATION ---------

set TARGET=target

set CLASS_PATH=%TARGET%\classes

set JAR_PATH=%TARGET%\org.opendaylight.sxp.sxp-runtime-1.0-SNAPSHOT-executable.jar

rem ---------- LISTING -------------

set LIB_PATH=%LOG_PATH%;%NETTY_PATH%;%SWT_PATH%;%TCPMD5PATH%

set APP_PATH=%JAR_PATH%

rem ---------- RUNTIME -------------
set CLASSPATHS=%LIB_PATH%;%APP_PATH%
set JVM_PARAMS=-Dio.netty.leakDetectionLevel=PARANOID -Xms512M -Xmx2048M -Xss68k
rem -Xms16m -Xmx1024m -Xss68k 
rem -XX:MaxHeapSize=1024m
set JVM_CLASS=org.opendaylight.sxp.Runtime %*

"%JAVA_HOME%\bin\java.exe" -cp %CLASSPATHS% %JVM_PARAMS% %JVM_CLASS% 