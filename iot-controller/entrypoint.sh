#!/bin/sh

INSTANCE_NUMBER=$(hostname | awk -F'_' '{print $NF}')
echo "Replica instance number: $INSTANCE_NUMBER"

echo "Arguments passed from docker-compose: $@"

exec java -jar /app/iot-controller.jar "$@" "$INSTANCE_NUMBER"
