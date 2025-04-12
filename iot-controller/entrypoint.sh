#!/bin/sh

INSTANCE_ID=$(hostname | awk -F'_' '{print $NF}')
ADDRESS=$(hostname)

echo "Replica instance ID: $INSTANCE_ID"

echo "Arguments passed from docker-compose: $@"

exec java -jar /app/iot-controller.jar "$@" "$ADDRESS" "$INSTANCE_ID"
