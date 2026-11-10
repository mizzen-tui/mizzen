#!/bin/bash

cd "$(dirname "$0")"

CLASS_PKG=mizzen.examples
JAVA_OPTIONS="-cp ./target/classes"

# Track if we need to clear the screen
if [[ "$1" == "NOCLEAR" ]] ; then
   shift
else
   clear
fi

# Debug mode (Local)
if [[ "$1" == "DEBUG" ]] ; then
   JAVA_OPTIONS="-Xdebug -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=localhost:8000 $JAVA_OPTIONS"
   shift
fi

# Debug mode (Remote)
if [[ "$1" == "DEBUGREMOTE" ]] ; then
   JAVA_OPTIONS="-Xdebug -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=localhost:8080 $JAVA_OPTIONS"
   shift
fi

export PERFORMANCE_MONITORING_FLAGS=""


java -XX:+UnlockDiagnosticVMOptions \
     $JAVA_OPTIONS \
     $PERFORMANCE_MONITORING_FLAGS \
     "$@" 
