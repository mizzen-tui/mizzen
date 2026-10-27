#!/bin/bash

cd $(dirname $0)/..    # the project root

function colorMarkupON() { # turns colors on

    export RCol='\e[0m'    # Text Reset

    # Regular                  Bold                       Underline                  High Intensity             BoldHigh Intens            Background                 High Intensity Backgrounds
    export Bla='\e[0;30m';     export BBla='\e[1;30m';    export UBla='\e[4;30m';    export IBla='\e[0;90m';    export BIBla='\e[1;90m';   export On_Bla='\e[40m';    export On_IBla='\e[0;100m';
    export Red='\e[0;31m';     export BRed='\e[1;31m';    export URed='\e[4;31m';    export IRed='\e[0;91m';    export BIRed='\e[1;91m';   export On_Red='\e[41m';    export On_IRed='\e[0;101m';
    export Gre='\e[0;32m';     export BGre='\e[1;32m';    export UGre='\e[4;32m';    export IGre='\e[0;92m';    export BIGre='\e[1;92m';   export On_Gre='\e[42m';    export On_IGre='\e[0;102m';
    export Yel='\e[0;33m';     export BYel='\e[1;33m';    export UYel='\e[4;33m';    export IYel='\e[0;93m';    export BIYel='\e[1;93m';   export On_Yel='\e[43m';    export On_IYel='\e[0;103m';
    export Blu='\e[0;34m';     export BBlu='\e[1;34m';    export UBlu='\e[4;34m';    export IBlu='\e[0;94m';    export BIBlu='\e[1;94m';   export On_Blu='\e[44m';    export On_IBlu='\e[0;104m';
    export Pur='\e[0;35m';     export BPur='\e[1;35m';    export UPur='\e[4;35m';    export IPur='\e[0;95m';    export BIPur='\e[1;95m';   export On_Pur='\e[45m';    export On_IPur='\e[0;105m';
    export Cya='\e[0;36m';     export BCya='\e[1;36m';    export UCya='\e[4;36m';    export ICya='\e[0;96m';    export BICya='\e[1;96m';   export On_Cya='\e[46m';    export On_ICya='\e[0;106m';
    export Whi='\e[0;37m';     export BWhi='\e[1;37m';    export UWhi='\e[4;37m';    export IWhi='\e[0;97m';    export BIWhi='\e[1;97m';   export On_Whi='\e[47m';    export On_IWhi='\e[0;107m';
}

function doWait {  # interactively waits $1 seconds - displaying a message $2 and a progress bar
    local count=0
    local total=$1
    local msg=$2
    local sleepInterval="${3:-1}"
    local pstr="[========]"
    while [ $count -lt $total ]; do
      sleep $sleepInterval
      count=$(( $count + $sleepInterval ))
      local pd=$(( $count * 10 / $total ))
      printf "\r${RCol}%s - ${IYel}%3d ${Yel}seconds  ${Blu}%.${pd}s ${RCol}" "$msg" $(( $total - $count  ))  $pstr
    done
    printf "\n${Bla}Done waiting${RCol}\n"
}




colorMarkupON

echo "PARM 1 = $1"

echo "slow runner : $(pwd)"

$(pwd)/runDBG.sh NOCLEAR mizzen.examples.StringOutputTest


echo "sleeping for 5 seconds .. "


doWait 5 "Testing cursor handling"

echo " "
echo " .. "
$(pwd)/runDBG.sh  NOCLEAR mizzen.examples.SmallCanvasTest


doWait 10 "Let's wait for a bit"

echo ""
echo ""

ls -l

echo "sleeping for 5 seconds .. "
sleep 5
echo "done sleep .."

clear
echo "cleared screen?"

echo "A"
echo "B"
echo "C"
echo "D"

ls -l

echo "1"
echo "2"
echo "3"


echo "------------brought to you by ----------------------------------"
printf "${Cya}"
cat scripts/slowRunner.sh
printf "${RCol}"


echo -e "\n\n BANNER \n\n"

cat src/main/resources/mizzen/examples/sampledata/colour-banner.txt


cat scripts/*.sh


echo "$0 Done! ............................................."
