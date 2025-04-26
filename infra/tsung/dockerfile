FROM erlang:25-slim
ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get update && \
    apt-get install -y --no-install-recommends \
        ca-certificates wget && \
    update-ca-certificates && \
    \
    apt-get install -y --no-install-recommends \
        make gcc autoconf automake libtool \
        libxml2-dev libexpat1-dev libssl-dev && \
    \
    wget https://github.com/processone/tsung/archive/refs/tags/v1.8.0.tar.gz && \
    tar -xzf v1.8.0.tar.gz && \
    cd tsung-1.8.0 && \
    ./configure && make && make install && \
    cd / && rm -rf tsung-1.8.0 v1.8.0.tar.gz && \
    \
    apt-get purge -y gcc autoconf automake libtool && \
    apt-get autoremove -y && \
    apt-get clean && rm -rf /var/lib/apt/lists/*

RUN mkdir -p /root/.tsung/log
WORKDIR /tsung
VOLUME ["/tsung"]
ENTRYPOINT ["tsung"]
