mvn clean install

if [[ $? -ne 0 ]] ; then
   echo "build failed!"
   exit 1
fi
mvn source:jar

