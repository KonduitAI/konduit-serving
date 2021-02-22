FROM ubuntu:20.04
MAINTAINER Shams Ul Azeem <shams@konduit.ai>

ENV PATH "/root/miniconda/bin:/root/konduit/bin:$PATH"
ENV KONDUIT_WORK_DIR "/root/konduit/work"
ENV CONDA_ENVIRONMENT_FILE "${KONDUIT_WORK_DIR}/environment.yml"
ENV PIP_REQUIREMENTS_FILE "${KONDUIT_WORK_DIR}/requirements.txt"
ENV KONDUIT_CONFIG_FILE "${KONDUIT_WORK_DIR}/config.yml"
ENV KONDUIT_RUN_SCRIPT "${KONDUIT_WORK_DIR}/../run.sh"

RUN mkdir /root/konduit && mkdir /root/konduit/work

ADD konduit /root/konduit
ADD run.sh ${KONDUIT_RUN_SCRIPT}

RUN apt clean && \
    apt update --fix-missing && \
    DEBIAN_FRONTEND=noninteractive && \
    apt install -y --no-install-recommends libglib2.0-0 && \
    apt install -y build-essential htop procps curl tree wget less libgl1-mesa-glx vim nano && \
    wget -q https://repo.anaconda.com/miniconda/Miniconda3-latest-Linux-x86_64.sh -O ~/miniconda.sh && \
    bash ~/miniconda.sh -b -p $HOME/miniconda && \
    conda clean --all -y && \
    rm -rf /root/miniconda/pkgs

CMD ["bash", "${KONDUIT_RUN_SCRIPT}"]
