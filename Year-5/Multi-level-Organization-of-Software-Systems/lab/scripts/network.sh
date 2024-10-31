#!/bin/bash

network_name="mops-network"
network_driver="bridge"


create() {
  docker network create -d $network_driver $network_name
}

rm() {
  docker network rm $network_name
}

connect() {
  docker network connect $network_name $1
}

disconect() {
  docker network disconnect $network_name $1
}

inspect() {
  docker network inspect $network_name
}

case "$1" in
  create)
    create
    ;;
  rm)
    rm
    ;;
  connect)
    connect $2
    ;;
  disconnect)
    disconnect $2
    ;;
  inspect)
    inspect
    ;;
  *)
    echo "Usage: $0 {start|stop|status|restart}"
    exit 1
    ;;
esac
