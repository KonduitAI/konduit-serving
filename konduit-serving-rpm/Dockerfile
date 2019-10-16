FROM centos:7
RUN yum install -y java-1.8.0-openjdk-devel which rpm-build redhat-rpm-config
ADD pipelines /usr/share/pipelines
WORKDIR  /usr/share/pipelines
RUN ./mvnw -Djavacpp.platform=linux-x86_64 -Dchip=cpu -Prpm -Ppython clean install -Dmaven.test.skip=true