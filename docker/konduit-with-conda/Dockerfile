FROM jupyter/base-notebook
USER root
ARG CHIP=cpu
RUN apt-get update && apt-get -y dist-upgrade \
 && apt-get  install -y --no-install-recommends \
   git \
   openjdk-8-jdk \
 && rm -rf /var/lib/apt/lists/*

ARG NB_USER="jovyan"
USER $NB_USER

RUN git clone https://github.com/KonduitAI/konduit-serving  && \
     cd konduit && \
     python build_jar.py


WORKDIR /home/$NB_USER/konduit
RUN cd python && \
     python setup.py install

USER root

RUN rm -rf /home/$NB_USER/.m2

USER $NB_USER
WORKDIR /home/$NB_USER/





