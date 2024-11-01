#!/bin/bash

script_dir=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

recreate() {
  destroy
  create
}

create() {
  bash $script_dir/network.sh create

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
    echo "unknown command"
    exit 1
    ;;
esac
