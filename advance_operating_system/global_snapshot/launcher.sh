#!/usr/bin/env bash

# Change this to your netid
netid=ccz180000

# Root directory of your project
PROJDIR=$HOME/AOSAssignment1

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
    minPerActive=$( echo ${configFile[0]} | awk '{ print $2 }' )
    maxPerActive=$( echo ${configFile[0]}| awk '{ print $3 }' )
    minSendDelay=$( echo ${configFile[0]} | awk '{ print $4 }' )
    snapshotDelay=$( echo ${configFile[0]} | awk '{ print $5 }' )
    maxNumber=$( echo ${configFile[0]} | awk '{ print $6 }' )

    i=1

    while [[ $i -le $nodes ]]
    do
        params=(${configFile[$((i))]})
        nodeId="${params[0]}"
        host="${params[1]}"
        port="${params[2]}"
        
        neighborsArray=(${configFile[$((i + nodes))]})
        counter=0

        for neighbor in "${neighborsArray[@]}"
        do 
            neighborDetails=(${configFile[$((1+neighbor))]})
            neighborsArray[counter]=$(printf ",%s" "$neighbor" "${neighborDetails[1]}" "${neighborDetails[2]}")
            counter=$((counter+1))
        done

        #I have to combine them with _ because the osascript does not allow empty spaces in strings.
        neighbors=$(printf "_%s" "${neighborsArray[@]}")
        
        echo "${neighbors}"
        
        # gnome-terminal -- ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no '$netid@$host' java -cp '$BINDIR' '$PROG' '$nodeId' '$port' '$minPerActive' '$maxPerActive' '$minSendDelay' '$snapshotDelay' '$maxNumber' '${neighbors[@]}'
        echo "$BINDIR: $PROG $nodeId $port $minPerActive $maxPerActive $minSendDelay $snapshotDelay $maxNumber ${neighbors[@]} $nodes"
        gnome-terminal -- bash -c "java -cp $BINDIR: $PROG $nodeId $port $minPerActive $maxPerActive $minSendDelay $snapshotDelay $maxNumber ${neighbors[@]} $nodes $CONFIG_FILE_NAME; exec bash" &
        i=$(( i + 1 ))
    done
)


 
