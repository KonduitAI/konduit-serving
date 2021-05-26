FROM ubuntu:20.04
MAINTAINER Shams Ul Azeem <shams@konduit.ai>

ENV MINICONDA_ROOT_DIR "${HOME}/miniconda"
ENV KONDUIT_ROOT_DIR "${HOME}/konduit"
ENV PATH "${MINICONDA_ROOT_DIR}/bin:${KONDUIT_ROOT_DIR}:${KONDUIT_ROOT_DIR}/bin:$PATH"
ENV KONDUIT_SCRIPTS_DIR "${KONDUIT_ROOT_DIR}/scripts"
ENV KONDUIT_WORK_DIR "${KONDUIT_ROOT_DIR}/work"
ENV CONDA_ENVIRONMENT_FILE "${KONDUIT_SCRIPTS_DIR}/environment.yml"
ENV PIP_REQUIREMENTS_FILE "${KONDUIT_SCRIPTS_DIR}/requirements.txt"
ENV KONDUIT_PRE_SETUP_FILE "${KONDUIT_SCRIPTS_DIR}/pre.sh"
ENV KONDUIT_POST_SETUP_FILE "${KONDUIT_SCRIPTS_DIR}/post.sh"
ENV KONDUIT_CONFIG_FILE "${KONDUIT_WORK_DIR}/config.yml"
ENV KONDUIT_INIT_SCRIPT "${KONDUIT_WORK_DIR}/../init.sh"
ENV KONDUIT_RUN_SCRIPT "${KONDUIT_WORK_DIR}/../run.sh"
ENV KONDUIT_INIT_AND_RUN_SCRIPT "${KONDUIT_WORK_DIR}/../init_and_run.sh"

RUN mkdir "${KONDUIT_ROOT_DIR}" "${KONDUIT_WORK_DIR}" "${KONDUIT_SCRIPTS_DIR}"

ADD konduit "${KONDUIT_ROOT_DIR}"
ADD init.sh ${KONDUIT_INIT_SCRIPT}
ADD run.sh ${KONDUIT_RUN_SCRIPT}
ADD init_and_run.sh ${KONDUIT_INIT_AND_RUN_SCRIPT}

RUN apt clean && \
    apt update --fix-missing && \
    DEBIAN_FRONTEND=noninteractive && \
    apt install -y --no-install-recommends libglib2.0-0 && \
    apt install -y build-essential htop procps curl tree wget less libgl1-mesa-glx vim nano && \
    wget -q https://repo.anaconda.com/miniconda/Miniconda3-latest-Linux-x86_64.sh -O ~/miniconda.sh && \
    bash ~/miniconda.sh -b -p "${MINICONDA_ROOT_DIR}" && \
    conda install -y -c conda-forge python=3.7 openjdk=8 && \
    conda clean --all -y && \
    rm -rf "${MINICONDA_ROOT_DIR}"/pkgs && \
    chmod u+x ${KONDUIT_INIT_SCRIPT} && \
    chmod u+x ${KONDUIT_RUN_SCRIPT} && \
    chmod u+x ${KONDUIT_INIT_AND_RUN_SCRIPT}

CMD ["bash", "-c", "${KONDUIT_INIT_AND_RUN_SCRIPT}"]
