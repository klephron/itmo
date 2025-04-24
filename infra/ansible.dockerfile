FROM ubuntu:24.04

ENV ANSIBLE_VERSION=2.18.5
ENV PATH="/root/.local/bin:$PATH"

RUN apt-get update && \
    apt-get install -y python3 python3-pip python3-venv pipx && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

RUN pipx install ansible-core==${ANSIBLE_VERSION}

WORKDIR /ansible
CMD ["/bin/bash"]
