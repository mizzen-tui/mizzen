#!/bin/bash

cd $(dirname $0)

echo "PARM 1 = $1"

echo "slow runner : $(pwd)"


$(pwd)/runDBG.sh NOCLEAR dtb.terminalIO.examples.StringOutputTest

echo "sleeping for 5 seconds .. "

sleep 5
echo "done sleep .."


echo " "
echo " "
echo " "
echo " "
# $(pwd)/runDBG.sh  NOCLEAR dtb.terminalIO.examples.SmallCanvasTest


ls -l

echo "sleeping for 10 seconds .. "
sleep 10
echo "done sleep .."

echo " "
echo " "
echo " "
echo " "

ls -l

echo ""
echo ""
echo ""
echo "Done! ........................................."
