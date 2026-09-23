FROM jenkins/jenkins:lts-jdk21

USER root

RUN apt-get update \
    && apt-get install docker.io \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

USER jenkins