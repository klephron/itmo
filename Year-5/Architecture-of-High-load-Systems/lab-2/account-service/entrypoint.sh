#!/bin/sh

INSTANCE_ID=$(hostname | awk -F'_' '{print $NF}')
ADDRESS=$(hostname)

echo "Replica instance ID: $INSTANCE_ID"

echo "Arguments passed from docker-compose: $@"

exec java org.springframework.boot.loader.launch.JarLauncher "$@" "$ADDRESS" "$INSTANCE_ID"
