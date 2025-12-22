FROM gitlab/gitlab-runner:ubuntu-v18.7.0

RUN apt-get update && apt-get install -y \
      apt-transport-https \
      ca-certificates \
      curl \
      gnupg \
      lsb-release && \
    install -m 0755 -d /etc/apt/keyrings && \
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg && \
    chmod a+r /etc/apt/keyrings/docker.gpg && \
    echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" > /etc/apt/sources.list.d/docker.list && \
    apt-get update && apt-get install -y \
      docker-ce \
      docker-ce-cli \
      containerd.io=1.7.28-1~ubuntu.24.04~noble \
      docker-buildx-plugin \
      docker-compose-plugin && \
    rm -rf /var/lib/apt/lists/*

RUN echo '#!/bin/sh' > /entrypoint-wrapper.sh && \
    echo 'dockerd &' >> /entrypoint-wrapper.sh && \
    echo 'sleep 3' >> /entrypoint-wrapper.sh && \
    echo 'exec /usr/bin/dumb-init /entrypoint "$@"' >> /entrypoint-wrapper.sh && \
    chmod +x /entrypoint-wrapper.sh

ENTRYPOINT ["/entrypoint-wrapper.sh"]
CMD ["run"]
