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
nginx -c $(pwd)/nginx.conf -e $(pwd)/nginx.log
```

> IMPORTANT!
> 1. Update `nginx.conf` paths if needed. Current project prefix is `/home/studs/s312563/works/mpi`.
> 2. Ensure `nginx/temp` directory exists.

## Tunneling

```sh
ssh -p 443 -R0:localhost:762 a.pinggy.io
```

> - `ngrok` ip is in blacklist
> - `vk-tunnel` doesn't work
> - `localtunnel` is not supported by FreeBSD

No installation required. Use server url to build frontend application. For example:

```
https://rnvlk-77-234-196-15.a.free.pinggy.link
```

> See Backend and Frontend for additional details.

## Backend

Build application:

```sh
./gradlew build
```

Install application:

```sh
scp -P 2222 build/libs/backend-0.1-all-optimized.jar s312563@helios:/home/studs/s312563/works/mpi
```

Start application on helios:

```sh
DATASOURCES_DEFAULT_URL="jdbc:postgresql://localhost:5432/studs" DATASOURCES_DEFAULT_USERNAME="<username>" DATASOURCES_DEFAULT_PASSWORD="<password>" MICRONAUT_SERVER_PORT=7622 java -jar backend-0.1-all-optimized.jar
```

Native builds are currently not supported.

## Frontend

Use earlier received URL to configure `VITE_SERVER_URL`:

```sh
sed -i 's|^VITE_SERVER_URL=.*$|VITE_SERVER_URL=https://rnvlk-77-234-196-15.a.free.pinggy.link/api|' .env.production
pnpm run build
tar cvf frontend.tar -C build/client .
scp -P 2222 frontend.tar s312563@helios:/home/studs/s312563/works/mpi
```

And on helios:

```sh
pwd
/home/studs/s312563/works/mpi
[s312563@helios ~/works/mpi]$ tar xvf frontend.tar -C nginx/html/
```
