# Многоуровневая организация программных систем

Ссылка на задание: [vk.com/@-227271114-laboratornoe-zadanie](https://vk.com/@-227271114-laboratornoe-zadanie)

## Deployment

Ensure `mops-rule-engine`, `mops-controller`, `mops-data-simulator` are built before execution:

```sh
cd controller/
docker build . -t mops-controller
cd ..

cd rule-engine/
docker build . -t mops-rule-engine
cd ..

cd data-simulator/
docker build . -t mops-data-simulator
cd ..
```

Deploy services:

```sh
bash scripts/all.sh create
```

Script will create `mops-network` network and start these services:

- `redis`
- `logstash`
- `elasticsearch`
- `kibana`
- `prometheus`
- `mongo`
- `mongo-exporter`
- `grafana`
- `rabbitmq:management`
- 4 instances of `mops-rule-engine`
- 3 instances of `mops-controller`
- 3 instances of `mops-data-simulator`

And named volumes:

- `mops-elasticsearch-data`
- `mops-grafana-data`
- `mops-kibana-data` (UNUSED)
- `mops-prometheus-data`

Create logstash user after system startup:

```sh
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
```

To destroy everything:

```sh
bash scripts/all.sh destroy
```

Script won't destroy named volumes.

## Useful commands

Execute to get other useful info. For example for `all.sh`:

```sh
bash scripts/all.sh help
```

## Connection to dashboards

RabbitMQ:
- `localhost:9031`
- login/password `root`/`root`

Prometheus:
- `localhost:9041`
- login/password `root`/`root`

Grafana:
- `localhost:9051`
- login/password `root`/`root`

Kibana:
- `localhost:9071`
- Default ELK authentication, see:

```sh
bash scripts/elasticsearch.sh commands
```
