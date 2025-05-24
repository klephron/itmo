# Infra

## Frontend

Project structure:

```sh
# /home/studs/s312563/works/mpi/frontend
access.log
error.log
html/
mime.types
nginx.conf
nginx.log
temp/
```

Start Nginx after installing frontend application in html directory:

```sh
nginx -c $(pwd)/nginx.conf -e $(pwd)/nginx.log
```

## Backend

Project structure:

```sh
# /home/studs/s312563/works/mpi/backend
backend-0.1-all-optimized.jar
backend-0.1-all.jar
backend-0.1-native.jar
backend-0.1-optimized-runner.jar
backend-0.1.jar
```

Start application:

```sh
java -jar backend-0.1-all-optimized.jar
```

Native builds currently are not supported.
