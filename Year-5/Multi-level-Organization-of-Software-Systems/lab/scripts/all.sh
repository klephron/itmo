#!/bin/bash

script_dir=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

controller_cnt=3
controller2rule_engine_queues=16
controller2rule_engine_consumers=4

recreate() {
  destroy
  create
}

create() {
  bash $script_dir/network.sh create

  bash $script_dir/redis.sh start
  bash $script_dir/network.sh connect $(bash $script_dir/redis.sh name)

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

  for i in $(seq 1 $controller2rule_engine_consumers);
  do
    bash $script_dir/rule-engine.sh start $controller2rule_engine_queues $controller2rule_engine_consumers $i
    bash $script_dir/network.sh connect $(bash $script_dir/rule-engine.sh name $controller2rule_engine_queues $controller2rule_engine_consumers $i)
  done

  for i in $(seq 1 $controller_cnt);
  do
    bash $script_dir/controller.sh start $controller2rule_engine_queues $i
    bash $script_dir/network.sh connect $(bash $script_dir/controller.sh name $controller2rule_engine_queues $i)

    bash $script_dir/data-simulator.sh start $i
    bash $script_dir/network.sh connect $(bash $script_dir/data-simulator.sh name $i)
  done
}

destroy() {
  for i in $(seq 1 $controller_cnt);
  do
    bash $script_dir/data-simulator.sh stop $i
    bash $script_dir/data-simulator.sh rm $i

    bash $script_dir/controller.sh stop $controller2rule_engine_queues $i
    bash $script_dir/controller.sh rm $controller2rule_engine_queues $i
  done

  for i in $(seq 1 $controller2rule_engine_consumers);
  do
    bash $script_dir/rule-engine.sh stop $controller2rule_engine_queues $controller2rule_engine_consumers $i
    bash $script_dir/rule-engine.sh rm $controller2rule_engine_queues $controller2rule_engine_consumers $i
  done

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

  bash $script_dir/redis.sh stop
  bash $script_dir/redis.sh rm

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
