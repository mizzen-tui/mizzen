
MAIN_CLASS=$1
targetName=$2

JAVA_HOME=/usr/lib/jvm/graalvm
GRAAL_HOME=$JAVA_HOME

PATH=$JAVA_HOME/bin:$PATH

INIT_AT_BUILD_TIME=--initialize-at-build-time=dtb.terminalIO,sun.java2d.Disposer,com.sun.imageio.plugins.jpeg.JPEGImageReader,com.sun.imageio.plugins.png.PNGImageReader
INIT_AT_BUILD_TIME=--initialize-at-build-time=dtb.terminalIO


NATIVE_IMG=$JAVA_HOME/bin/native-image

$NATIVE_IMG    -H:+AllowIncompleteClasspath  \
               -H:+PrintClassInitialization   \
               --no-fallback  \
               --static \
               $INIT_AT_BUILD_TIME  \
               -Djava.awt.headless=true  \
               -cp "$(ls ./target/dtbTerminalIO-*.jar | grep -v sources | head -1)":.  \
                $MAIN_CLASS  
               
if [[ $? -ne 0 ]] ; then
    exit 1
fi               

generated=$(echo $MAIN_CLASS |  tr '[:upper:]' '[:lower:]')

# all native outputs live under target/native (gitignored, wiped by mvn clean)
OUT_DIR=./target/native
mkdir -p $OUT_DIR/reports
mv $generated $OUT_DIR/$targetName
[[ -d reports ]] && mv reports/* $OUT_DIR/reports/ && rmdir reports
echo "built $OUT_DIR/$targetName"
