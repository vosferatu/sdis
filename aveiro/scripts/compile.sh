rm -r bin
mkdir -p bin

javac $(find src | grep .java) -d bin

echo "Compiling finished"

cd bin

kill -9 $(pidof rmiregistry)

rmiregistry &

echo "RMI registry started"