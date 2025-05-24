# Infra

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
