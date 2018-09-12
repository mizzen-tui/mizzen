
clear

cd $(dirname $0)

CLASS_PKG=dtb.terminalIO.examples
CLASS_NAME=${CLASS_PKG}.TabPanelTest
JAVA_OPTIONS="-cp ./bin/"
if [[ "$1" == "DEBUG" ]] ; then
   JAVA_OPTIONS="-Xdebug -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=localhost:8000 $JAVA_OPTIONS"
   shift
fi

if [[ "$1" == bar* ]] ; then
   export CLASS_NAME=${CLASS_PKG}.BarChartTest
   shift
else 
	if [[ "$1" == border* ]] ; then
	   export CLASS_NAME=${CLASS_PKG}.BorderedComponentTest
	   shift
    else
        if [[ "$1" == compo* ]] ; then
	       export CLASS_NAME=${CLASS_PKG}.CompositeTest
	       shift
	    else
	    	if [[ "$1" == scroll* ]] ; then
               export CLASS_NAME=${CLASS_PKG}.CompositeScrollTest
	           shift
	        else
	        	if [[ -n "${1}" ]] ; then
	        	     export CLASS_NAME=$1
	            fi 
	        fi
        fi    
	fi	
fi

# -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:8000

java  $JAVA_OPTIONS $CLASS_NAME $@


#   java java -Xdebug -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=y -cp ./bin/ dtb.terminalIO.BootStrapTests
