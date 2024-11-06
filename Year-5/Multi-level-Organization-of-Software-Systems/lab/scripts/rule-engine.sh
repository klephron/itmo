#!/bin/bash

script_dir=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
queues="${2:-1}"
consumers="${3:-1}"
index="${4:-1}"

container_name="mops-rule-engine-$consumers-$index"
container="mops-rule-engine:latest"

start() {
    # --log-driver=gelf \
    # --log-opt gelf-address=udp://127.0.0.1:9081 \
    # --log-opt tag="$container_name" \
  docker run --name $container_name \
    --log-driver=gelf \
    --log-opt gelf-address=udp://127.0.0.1:9081 \
    --log-opt tag="$container_name" \
    -e RULE_ENGINE_MONGO_URL="mongodb://mops-mongo:27017/" \
    -e RULE_ENGINE_MQ_HOST="mops-rabbitmq" \
    -e RULE_ENGINE_REDIS_HOST="mops-redis" \
    -e RULE_ENGINE_MQ_QUEUES="$queues" \
    -e RULE_ENGINE_MQ_CONSUMERS="$consumers" \
    -e RULE_ENGINE_MQ_INDEX="$index" \
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
