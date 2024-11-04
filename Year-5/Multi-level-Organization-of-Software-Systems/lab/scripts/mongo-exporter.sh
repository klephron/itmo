#!/bin/bash

script_dir=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

container_name="mops-mongo-exporter"
container="bitnami/mongodb-exporter:0.42.0"

mongo_root_username="root"
mongo_root_password="root"

start() {
  docker run --name $container_name \
    -p 9091:9216 \
    -e MONGODB_USER=$mongo_root_username \
    -e MONGODB_PASSWORD=$mongo_root_password \
    -d $container \
    --mongodb.uri=mongodb://mops-mongo:27017 \
    --collector.dbstats \
    --collector.dbstatsfreestorage \
    --collector.indexstats \
    --collector.collstats \
    --collector.topmetrics
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
