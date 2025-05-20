#!/bin/bash

cp /etc/mongo-common/keyfile.key /etc/mongo.key
chmod 400 /etc/mongo.key
chown 999:999 /etc/mongo.key
