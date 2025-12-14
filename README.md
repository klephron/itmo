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

Start Nginx after installing frontend application in html directory:

```sh
$(cd works/mpi/nginx && nginx -c $(pwd)/nginx.conf -e $(pwd)/nginx.log)
```

> IMPORTANT!
>
> 1. Update `nginx.conf` paths if needed. Current project prefix is `/home/studs/s312563/works/mpi`.
> 2. Ensure `nginx/temp` directory exists.

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

Use earlier received URL to configure `VITE_SERVER_URL`:

```sh
sed -i 's|^VITE_SERVER_URL=.*$|https://xgzti-188-243-217-139.a.free.pinggy.link|' .env.production
pnpm run build
tar cvf frontend.tar -C build/client .
scp -P 2222 frontend.tar s312563@helios:/home/studs/s312563/works/mpi
```

Or if localhost:

```sh
sed -i 's|^VITE_SERVER_URL=.*$|VITE_SERVER_URL=http://localhost:7621|' .env.production
```

And on helios:

```sh
pushd works/mpi

tar xvf frontend.tar -C nginx/html/
```
