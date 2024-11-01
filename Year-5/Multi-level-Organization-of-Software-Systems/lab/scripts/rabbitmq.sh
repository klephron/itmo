#!/bin/bash

container_name="mops-rabbitmq"
container="rabbitmq:4.0.3-management"

rabbitmq_default_user="root"
rabbitmq_default_pass="root"

start() {
  docker run --name $container_name \
    -p 9031:15672 \
    -e RABBITMQ_DEFAULT_USER="$rabbitmq_default_user" \
    -e RABBITMQ_DEFAULT_PASS="$rabbitmq_default_pass" \
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
