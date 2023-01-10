#!/usr/bin/env bash

# Change this to your netid
netid=ccz180000

# Root directory of your project
PROJDIR=$HOME/AOSAssignment2

# Directory where the config file is located on your local system
CONFIG_FILE_NAME='configlocal'
CONFIGLOCAL=$PROJDIR/$CONFIG_FILE_NAME.txt

# Directory your java classes are in
BINDIR=$PROJDIR/bin

# Your main project class
PROG=Main

javac -d ./bin -cp ./bin ./src/*.java

cat $CONFIGLOCAL | sed -e "s/#.*//" | sed -e "/^\s*$/d" |
(
    mapfile -t configFile
    echo "${configFile[0]}"
    nodes=$( echo ${configFile[0]} | awk '{ print $1 }' )

    i=1
    while [[ $i -le $nodes ]]
    do
        params=(${configFile[$((i))]})
        nodeId="${params[0]}"
        echo "Starting Node: ${nodeId}"
        gnome-terminal -- bash -c "java -cp $BINDIR: $PROG $nodeId $CONFIGLOCAL; exec bash" &
        i=$(( i + 1 ))
    done
)


 
