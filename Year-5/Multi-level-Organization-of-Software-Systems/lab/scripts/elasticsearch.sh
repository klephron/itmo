#!/bin/bash

script_dir=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

container_name="mops-elasticsearch"
container="elasticsearch:8.15.3"
container_volume_data="mops-elasticsearch-data"

start() {
  docker volume create $container_volume_data
  docker run --name $container_name \
    -p 9061:9200 \
    --ulimit memlock=-1:-1 \
    -e node.name="$container_name" \
    -e discovery.type="single-node" \
    -e cluster.name="mops-elasticsearch-cluster" \
    -e bootstrap.memory_lock="true" \
    -e ES_JAVA_OPTS="-Xms512m -Xmx512m" \
    -v $container_volume_data:/usr/share/elasticsearch/data/ \
    -d $container

  commands;
}

commands() {
  echo docker exec -it $container_name \
   /usr/share/elasticsearch/bin/elasticsearch-reset-password -u elastic
  echo docker exec -it $container_name \
    /usr/share/elasticsearch/bin/elasticsearch-create-enrollment-token -s kibana
  echo "Or configure manually:"
  echo docker exec -it $container_name \
   /usr/share/elasticsearch/bin/elasticsearch-reset-password -u kibana_system
  # create user/role for logstash
  echo curl -u elastic:password \
    -X PUT "http://localhost:9061/_security/role/logstash_writer_role" -H \
    'Content-Type: application/json' -d '{
      "cluster": ["manage_index_templates", "monitor", "manage", "all"],
      "indices": [
        {
          "names": ["mops-*"],
          "privileges": ["write", "create", "create_index", "manage"]
        }
      ]
    }'
  echo curl -u elastic:password \
    -X POST "http://localhost:9061/_security/user/logstash_writer" -H \
    'Content-Type: application/json' -d '{
      "password" : "password",
      "roles" : [ "logstash_writer_role" ]
    }'
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
  commands)
    commands
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
    echo "Usage $0: {start|commands|stop|inspect|restart|rm|rm-volumes|name|recreate}"
    exit 1
    ;;
esac
