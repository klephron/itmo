#!/bin/bash

container_name="mops-mongo"
container="mongo:8.0"

mongo_root_username="root"
mongo_root_password="root"

start() {
  docker run --name $container_name \
    -e MONGO_INITDB_ROOT_USERNAME="$mongo_root_username" \
    -e MONGO_INITDB_ROOT_PASSWORD="$mongo_root_password" \
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
  *)
    echo "unknown command"
    exit 1
    ;;
esac
