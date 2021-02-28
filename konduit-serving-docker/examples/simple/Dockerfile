FROM konduit/konduit-serving-builder AS builder
ADD builder ${SCRIPTS_DIR}
RUN init

FROM konduit:konduit-serving
ADD runner/scripts ${KONDUIT_SCRIPTS_DIR}
ADD runner/work ${KONDUIT_WORK_DIR}
COPY --from=builder /root/miniconda /root/miniconda
RUN init
CMD ["bash", "-c", "${KONDUIT_RUN_SCRIPT}"]
