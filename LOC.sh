

cd $(dirname $0)



function statsLinesTotalForType() {

  ( find . -name "*.${1}" ) | grep -v node | grep -v jpeg |  grep -v target | grep -v jpg | grep -v json | grep -v mvn | grep -v preset | grep -v swagger | grep -v build  | grep -v eclipse  |  grep -v dbSRC | grep -v LOC.sh | xargs wc -l | tail -n 1

}

# ./LOC.sh | tail -n 1

function stats() {

  ( find . -name "*.j*" ; find . -name "*.sh" ; find . -name "*.css" ) | grep -v node | grep -v jpeg |  grep -v target | grep -v jpg | grep -v json | grep -v mvn | grep -v preset | grep -v swagger | grep -v build  | grep -v eclipse  |  grep -v dbSRC | grep -v LOC.sh | xargs wc -l

}
function statsByGroup() {
    echo "File Type         # lines "
    echo "----------------  ---------------------------------"
    echo "Shell      files: $(statsLinesTotalForType sh)"
    echo "Java       files: $(statsLinesTotalForType java)"
    echo "javascript files: $(statsLinesTotalForType 'js*')"
    echo "html       files: $(statsLinesTotalForType 'htm*')"
    echo "css        files: $(statsLinesTotalForType 'css')"
    echo "xml        files: $(statsLinesTotalForType 'xml')"
    echo "properties files: $(statsLinesTotalForType 'properties')"
}


statsByGroup 

echo "  ----  "
echo " # Lines overall "
echo " "
stats | tail -n 1
