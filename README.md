# Infra

Project structure:

```
mpi/
├── backend-0.1-all-optimized.jar
├── frontend.tar
└── nginx/
    ├── access.log
    ├── error.log
    ├── html/
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

## Frontend

Start Nginx after installing frontend application in html directory:

```sh
nginx -c $(pwd)/nginx.conf -e $(pwd)/nginx.log
```

## Backend

Start application:

```sh
java -jar backend-0.1-all-optimized.jar
```

Native builds currently are not supported.
