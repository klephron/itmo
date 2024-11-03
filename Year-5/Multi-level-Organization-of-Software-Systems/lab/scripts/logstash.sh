#!/bin/bash

script_dir=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

container_name="mops-logstash"
container="logstash:8.15.3"

start() {
  docker run --name $container_name \
    -p 9081:9081/udp \
    -e LS_JAVA_OPTS="-Xms512m -Xmx512m" \
    -v $script_dir/../logstash/logstash.yml:/usr/share/logstash/config/logstash.yml \
    -v $script_dir/../logstash/pipeline/:/usr/share/logstash/pipeline/ \
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
