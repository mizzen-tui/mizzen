#!/bin/bash
# clear

cd $(dirname $0)

CLASS_PKG=mizzen.examples
JAVA_OPTIONS="-cp ./target/classes"

if [[ "$1" == "NOCLEAR" ]] ; then
   shift
else
   clear
fi


if [[ "$1" == "DEBUG" ]] ; then
   JAVA_OPTIONS="-Xdebug -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=localhost:8000 $JAVA_OPTIONS"
   shift
fi


export PERFORMANCE_MONITORING_FLAGS=""

if [[ "$1" == "PERF" ]] ; then
   export PERFORMANCE_MONITORING_FLAGS="-Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=9010 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false"
   shift
fi


# -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:8000

java  -XX:+UnlockDiagnosticVMOptions $JAVA_OPTIONS $PERFORMANCE_MONITORING_FLAGS $@


#   java java -Xdebug -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=y -cp ./bin/ mizzen.BootStrapTests
