FROM ubuntu:20.04
MAINTAINER Shams Ul Azeem <shams@konduit.ai>

ARG SCRIPT_DIR

ENV PATH "/root/miniconda/bin:/root/konduit/bin:$PATH"

RUN mkdir /root/konduit

ADD ${SCRIPT_DIR}/konduit-serving-tar/target/konduit-serving-tar-0.1.0-SNAPSHOT-dist /root/konduit

RUN apt clean && \
    apt update --fix-missing && \
    DEBIAN_FRONTEND=noninteractive && \
    apt install -y --no-install-recommends libglib2.0-0 && \
    apt install -y build-essential htop procps curl tree wget less libgl1-mesa-glx vim nano && \
    wget -q https://repo.anaconda.com/miniconda/Miniconda3-latest-Linux-x86_64.sh -O ~/miniconda.sh && \
    bash ~/miniconda.sh -b -p $HOME/miniconda && \
    conda clean --all -y && \
    rm -rf /root/miniconda/pkgs

CMD ["echo", "This is the konduit-serving builder image..."]
