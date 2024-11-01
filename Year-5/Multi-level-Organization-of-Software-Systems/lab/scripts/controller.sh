#!/bin/bash

container_name="mops-controller"
container="mops-controller:latest"

start() {
  docker run --name $container_name \
    -d $container
}

stop() {
  docker stop $container_name
}

restart() {
  docker restart $container_name
}

inspect() {
  docker inspect $container_name
}

rm() {
  docker rm $container_name
}

case "$1" in
  start)
    start
    ;;
  stop)
    stop
    ;;
  inspect)
    inspect
    ;;
  restart)
    restart
    ;;
  rm)
    rm
    ;;
  *)
    echo "unknown command"
    exit 1
    ;;
esac
