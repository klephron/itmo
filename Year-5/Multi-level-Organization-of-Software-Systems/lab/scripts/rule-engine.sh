#!/bin/bash

container_name="mops-rule-engine"
container="mops-rule-engine:latest"

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

name() {
  echo $container_name
}

recreate() {
  stop
  rm
  start
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
  name)
    name
    ;;
  recreate)
    recreate
    ;;
  *)
    echo "unknown command"
    exit 1
    ;;
esac
