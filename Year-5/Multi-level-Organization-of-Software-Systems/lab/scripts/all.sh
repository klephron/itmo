#!/bin/bash

script_dir=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

recreate() {
  destroy
  create
}

create() {
  bash $script_dir/network.sh create

  bash $script_dir/logstash.sh start
  bash $script_dir/network.sh connect $(bash $script_dir/logstash.sh name)
  bash $script_dir/elasticsearch.sh start
  bash $script_dir/network.sh connect $(bash $script_dir/elasticsearch.sh name)
  bash $script_dir/kibana.sh start
  bash $script_dir/network.sh connect $(bash $script_dir/kibana.sh name)

  bash $script_dir/prometheus.sh start
  bash $script_dir/network.sh connect $(bash $script_dir/prometheus.sh name)

  bash $script_dir/mongo.sh start
  bash $script_dir/network.sh connect $(bash $script_dir/mongo.sh name)
  bash $script_dir/mongo-exporter.sh start
  bash $script_dir/network.sh connect $(bash $script_dir/mongo-exporter.sh name)

  bash $script_dir/grafana.sh start
  bash $script_dir/network.sh connect $(bash $script_dir/grafana.sh name)

  bash $script_dir/rabbitmq.sh start
  bash $script_dir/network.sh connect $(bash $script_dir/rabbitmq.sh name)

  bash $script_dir/rule-engine.sh start
  bash $script_dir/network.sh connect $(bash $script_dir/rule-engine.sh name)

  bash $script_dir/controller.sh start
  bash $script_dir/network.sh connect $(bash $script_dir/controller.sh name)

  bash $script_dir/data-simulator.sh start
  bash $script_dir/network.sh connect $(bash $script_dir/data-simulator.sh name)
}

destroy() {
  bash $script_dir/data-simulator.sh stop
  bash $script_dir/data-simulator.sh rm

  bash $script_dir/controller.sh stop
  bash $script_dir/controller.sh rm

  bash $script_dir/rule-engine.sh stop
  bash $script_dir/rule-engine.sh rm

  bash $script_dir/rabbitmq.sh stop
  bash $script_dir/rabbitmq.sh rm

  bash $script_dir/grafana.sh stop
  bash $script_dir/grafana.sh rm

  bash $script_dir/mongo-exporter.sh stop
  bash $script_dir/mongo-exporter.sh rm
  bash $script_dir/mongo.sh stop
  bash $script_dir/mongo.sh rm

  bash $script_dir/prometheus.sh stop
  bash $script_dir/prometheus.sh rm

  bash $script_dir/kibana.sh stop
  bash $script_dir/kibana.sh rm
  bash $script_dir/elasticsearch.sh stop
  bash $script_dir/elasticsearch.sh rm
  bash $script_dir/logstash.sh stop
  bash $script_dir/logstash.sh rm

  bash $script_dir/network.sh rm
}

case "$1" in
  create)
    create
    ;;
  destroy)
    destroy
    ;;
  recreate)
    recreate
    ;;
  *)
    echo "Usage $0: {create|destroy|recreate}"
    exit 1
    ;;
esac
