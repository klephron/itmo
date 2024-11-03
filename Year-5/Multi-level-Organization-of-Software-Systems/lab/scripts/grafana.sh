#!/bin/bash

script_dir=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

container_name="mops-grafana"
container="grafana/grafana:11.3.0"
container_volume_data="mops-grafana-data"

start() {
  docker volume create $container_volume_data
  docker run --name $container_name \
    -p 9051:3000 \
    -e GF_INSTALL_PLUGINS="" \
    -e GF_PATHS_CONFIG="/etc/grafana/grafana.ini" \
    -v $script_dir/../grafana/grafana.ini:/etc/grafana/grafana.ini \
    -v $container_volume_data:/var/lib/grafana \
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

rm-volumes() {
  docker volume rm $container_volume_data
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
  rm-volumes)
    rm-volumes
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
