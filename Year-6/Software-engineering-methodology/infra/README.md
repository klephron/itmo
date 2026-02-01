# Infra

Project structure:

```
mpi/
├── backend-0.1-all-optimized.jar
├── frontend.tar
└── nginx/
    ├── access.log
    ├── error.log
    ├── html/ <- here goes static application
    ├── mime.types
    ├── nginx.conf
    ├── nginx.log
    └── temp/
        ├── client_body_temp/
        ├── fastcgi_temp/
        ├── proxy_temp/
        ├── scgi_temp/
        └── uwsgi_temp/
```

## Nginx

Config:

```
worker_processes  1;
pid       /home/studs/s312563/works/mpi/nginx/nginx.pid;
error_log /home/studs/s312563/works/mpi/nginx/error.log;

events {
  worker_connections  1024;
}

http {
  include /home/studs/s312563/works/mpi/nginx/mime.types;

  access_log            /home/studs/s312563/works/mpi/nginx/access.log;
  client_body_temp_path /home/studs/s312563/works/mpi/nginx/temp/client_body_temp;
  fastcgi_temp_path     /home/studs/s312563/works/mpi/nginx/temp/fastcgi_temp;
  proxy_temp_path       /home/studs/s312563/works/mpi/nginx/temp/proxy_temp;
  scgi_temp_path        /home/studs/s312563/works/mpi/nginx/temp/scgi_temp;
  uwsgi_temp_path       /home/studs/s312563/works/mpi/nginx/temp/uwsgi_temp;

  server {
    listen 7621;
    server_name _;
    root /home/studs/s312563/works/mpi/nginx/html;

    index index.html;

    location /api/ {
      proxy_pass http://localhost:7622/api/;
      proxy_set_header Host $http_host;
      proxy_set_header X-Real-IP $remote_addr;
      proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
      proxy_set_header X-Forwarded-Proto $scheme;
    }
    location /ws/ {
      proxy_pass http://localhost:7622/ws/;
      proxy_set_header Host $http_host;
      proxy_set_header X-Real-IP $remote_addr;
      proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
      proxy_set_header X-Forwarded-Proto $scheme;
      proxy_set_header Upgrade $http_upgrade;
      proxy_set_header Connection "upgrade";
    }
    location / {
      try_files $uri $uri/ /index.html;
    }
  }
}
```

1. Update `nginx.conf` paths if needed. Current project prefix is `/home/studs/s312563/works/mpi`.
2. Ensure `nginx/temp` directory exists.

Start Nginx after installing frontend application in html directory:

```sh
nginx -c $(pwd)/nginx.conf -e $(pwd)/nginx.log
```

## Tunneling

```sh
ssh -p 443 -R0:localhost:7621 free.pinggy.io
```

> None of these work now, as external network is down:
>
> - `ngrok` ip is in blacklist
> - `vk-tunnel` doesn't work
> - `localtunnel` is not supported by FreeBSD

No installation required. Use server url to build frontend application. For example:

> ```
> https://rnvlk-77-234-196-15.a.free.pinggy.link
> ```

If deploying on `helios.se.ifmo.ru` does not work then port forward and deploy proxy from localhost.

> See Backend and Frontend for additional details.

## Backend

Build application:

```sh
./gradlew build -x test -x spotlessJavaCheck -x spotlessYamlCheck
```

Install application:

```sh
scp -P 2222 build/libs/backend-0.1-all-optimized.jar s312563@helios:/home/studs/s312563/works/mpi
```

Start application on helios:

```sh
pushd works/mpi

DATASOURCES_DEFAULT_URL="$DATASOURCES_DEFAULT_URL" \
DATASOURCES_DEFAULT_USERNAME="$DATASOURCES_DEFAULT_USERNAME" \
DATASOURCES_DEFAULT_PASSWORD="$DATASOURCES_DEFAULT_PASSWORD" \
DATASOURCES_CBD_URL="$DATASOURCES_CBD_URL" \
DATASOURCES_CBD_USERNAME="$DATASOURCES_CBD_USERNAME" \
DATASOURCES_CBD_PASSWORD="$DATASOURCES_CBD_PASSWORD" \
MICRONAUT_SERVER_PORT=7622 \
java -jar backend-0.1-all-optimized.jar
```

Native builds are currently not supported.

## Frontend

Use earlier received URL to configure `VITE_SERVER_URL`, `VITE_WS_URL`:

```sh
sed -i '
  s|^VITE_SERVER_URL=.*$|VITE_SERVER_URL=https://zmsct-188-243-217-139.a.free.pinggy.link|
  s|^VITE_WS_URL=.*$|VITE_WS_URL=wss://zmsct-188-243-217-139.a.free.pinggy.link|
' .env.production

pnpm run build
tar cvf frontend.tar -C build/client .
scp -P 2222 frontend.tar s312563@helios:/home/studs/s312563/works/mpi
```

And on helios:

```sh
pushd works/mpi

tar xvf frontend.tar -C nginx/html/
```
