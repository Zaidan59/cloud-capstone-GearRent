ARG RUNNER_VERSION=5.1.1
FROM gitea/runner:${RUNNER_VERSION}

USER root
RUN apk add --no-cache \
    bash \
    ca-certificates \
    curl \
    git \
    git-lfs \
    jq \
    openssh-client \
    docker-cli \
    openjdk25-jdk \
    maven \
    nodejs \
    npm

WORKDIR /data
