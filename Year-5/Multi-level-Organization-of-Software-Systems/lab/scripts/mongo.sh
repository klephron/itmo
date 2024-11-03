#!/bin/bash

script_dir=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

container_name="mops-mongo"
container="mongo:8.0"

mongo_root_username="root"
mongo_root_password="root"

start() {
  logstash_host=$(bash $script_dir/logstash.sh name)

  docker run --name $container_name \
    --log-driver=gelf \
    --log-opt gelf-address=udp://127.0.0.1:9081 \
    --log-opt tag="$container_name" \
    -p 9011:27017 \
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
    echo "Usage $0: {start|stop|inspect|restart|rm|name|recreate}"
    exit 1
    ;;
esac
