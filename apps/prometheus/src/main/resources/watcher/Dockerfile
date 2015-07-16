FROM centos:7
MAINTAINER Jimmi Dyson <jimmidyson@gmail.com>

ENTRYPOINT ["/prometheus-k8s-watcher"]

RUN curl -L https://github.com/fabric8io/prometheus-k8s-watcher/releases/download/v0.5/prometheus-k8s-watcher-0.5-linux-amd64.tar.gz | tar xvz prometheus-k8s-watcher
