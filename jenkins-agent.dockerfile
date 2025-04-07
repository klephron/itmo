FROM jenkins/ssh-agent:jdk21

USER root

RUN apt update -y && apt install -y --no-install-recommends \
    ca-certificates curl && \
    rm -rf /var/lib/apt/lists/*

RUN install -m 0755 -d /etc/apt/keyrings && \
  curl -fsSL https://download.docker.com/linux/debian/gpg -o /etc/apt/keyrings/docker.asc && \
  chmod a+r /etc/apt/keyrings/docker.asc

RUN echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/debian \
  $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | \
  tee /etc/apt/sources.list.d/docker.list > /dev/null && \
  apt update -y && \
  apt install --no-install-recommends -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin && \
  rm -rf /var/lib/apt/lists/*

RUN sed -i 's/ulimit -Hn/# ulimit -Hn/g' /etc/init.d/docker

RUN usermod -aG docker jenkins

RUN mkdir -p /var/lib/docker
VOLUME /var/lib/docker

RUN echo '#!/bin/bash' > /custom-entrypoint.sh && \
    echo 'service docker start' >> /custom-entrypoint.sh && \
    echo 'exec /usr/local/bin/setup-sshd "$@"' >> /custom-entrypoint.sh && \
    chmod +x /custom-entrypoint.sh

ENTRYPOINT ["/custom-entrypoint.sh"]
