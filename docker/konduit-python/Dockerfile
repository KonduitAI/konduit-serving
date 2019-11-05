FROM python:3.7-alpine

USER root

RUN apt-get update && apt-get -y dist-upgrade \
 && apt-get install -y --no-install-recommends \
   git \
   openjdk-8-jdk \
 && rm -rf /var/lib/apt/lists/*

ARG USER="konduit"
USER ${USER}

WORKDIR /home/${USER}

RUN pip install --no-cache-dir -r requirements.txt && cd mkdir {data, source}

COPY konduit.jar requirements.txt create-config.py run.sh ./
COPY config.json script.py ./source/
COPY data/* ./data/

EXPOSE 9090

CMD [ "run.sh" ]