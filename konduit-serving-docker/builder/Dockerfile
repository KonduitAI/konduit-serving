FROM ubuntu:20.04
MAINTAINER Shams Ul Azeem <shams@konduit.ai>

ENV MINICONDA_ROOT_DIR "${HOME}/miniconda"
ENV PATH "${MINICONDA_ROOT_DIR}/bin:${HOME}:$PATH"
ENV SCRIPTS_DIR "${HOME}/scripts"
ENV CONDA_ENVIRONMENT_FILE "${SCRIPTS_DIR}/environment.yml"
ENV PIP_REQUIREMENTS_FILE "${SCRIPTS_DIR}/requirements.txt"
ENV PRE_SETUP_FILE "${SCRIPTS_DIR}/pre.sh"
ENV POST_SETUP_FILE "${SCRIPTS_DIR}/post.sh"
ENV INIT_SCRIPT "${SCRIPTS_DIR}/../init.sh"

RUN mkdir "${SCRIPTS_DIR}"

ADD init.sh ${INIT_SCRIPT}

RUN apt clean && \
    apt update --fix-missing && \
    DEBIAN_FRONTEND=noninteractive && \
    apt install -y --no-install-recommends libglib2.0-0 && \
    apt install -y build-essential htop procps curl tree wget less libgl1-mesa-glx vim nano && \
    wget -q https://repo.anaconda.com/miniconda/Miniconda3-latest-Linux-x86_64.sh -O ~/miniconda.sh && \
    bash ~/miniconda.sh -b -p "${MINICONDA_ROOT_DIR}" && \
    conda install -y -c conda-forge python=3.7 openjdk=8 maven && \
    conda clean --all -y && \
    rm -rf "${MINICONDA_ROOT_DIR}"/pkgs && \
    chmod u+x ${INIT_SCRIPT}

CMD ["echo", "This is a builder image for konduit-serving..."]
