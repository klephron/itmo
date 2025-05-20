source /etc/mongo-common/trap.sh
source /etc/mongo-common/keyfile_install.sh

mongos $@ &

wait

exit $?
