FROM konduit/konduit-serving-builder AS builder
RUN init

FROM konduit:konduit-serving
COPY --from=builder /root/miniconda /root/miniconda
RUN init
CMD ["bash", "-c", "${KONDUIT_RUN_SCRIPT}"]
