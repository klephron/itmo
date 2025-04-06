#!/bin/sh

# Извлекаем номер реплики из HOSTNAME
INSTANCE_NUMBER=$(hostname | awk -F'_' '{print $NF}')
echo "Replica instance number: $INSTANCE_NUMBER"

# Логируем переданные аргументы
echo "Arguments passed from docker-compose: $@"

# Запускаем приложение с номером реплики и переданными аргументами
exec java -jar /app/iot-controller.jar "$@" "$INSTANCE_NUMBER"
