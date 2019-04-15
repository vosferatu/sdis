#!/bin/bash

function usage {
	echo "sh peer.sh <version> <peerID> <access_point>"
}


if [ "$#" -eq 0 ]; then
	echo "Usage: "
	usage
	exit 1;
fi

cd bin

java server.Peer $1 $2 $3 224.0.0.0 8888 225.0.0.0 8888 226.0.0.0 8888