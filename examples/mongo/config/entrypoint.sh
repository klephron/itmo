source /etc/mongo-common/trap.sh
source /etc/mongo-common/keyfile_install.sh

mkdir -p /var/lib/mongo/db

mongod $@ &

MAIN_PID=$!
wait $MAIN_PID
