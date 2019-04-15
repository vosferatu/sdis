#!/bin/bash

function usage {
        echo "sh client.sh <peer> <operation> [<operand1> [<operand2>]]"
        echo "Operations: BACKUP, RESTORE, DELETE, RECLAIM and STATE"
}


if [ "$#" -eq 0 ]; then
        echo "Usage: "
        usage
        exit 1;
fi

cd bin

java client.TestApp $1 $2 $3 $4
