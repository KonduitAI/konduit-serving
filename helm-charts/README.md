# Konduit Serving Helm Charts

## Overview

This folder contains the [helm charts](https://helm.sh/docs/developing_charts/)
for konduit. These are examples for running konduit on kubernetes.

## Modules

[konduit-serving-single-model](./konduit-serving-single-model): Serve a single pipeline with an attached volume.

## Installation and pre requisites

Ensure a kubernetes cluster is setup. Either [docker](https://docs.docker.com/docker-for-windows/kubernetes/) or [minikube](https://kubernetes.io/docs/setup/learning-environment/minikube/) is recommended
for development environments.

For installing a konduit module in to your kubernetes cluster, first setup a
[persistent volume](https://kubernetes.io/docs/concepts/storage/persistent-volumes/)
and an associated [volume claim](https://kubernetes.io/docs/tasks/configure-pod-container/configure-persistent-volume-storage/)

Ensure you have [helm and tiller installed](https://helm.sh/docs/using_helm/)

### Port Configuration

For running konduit, you may want to configure the port the server is running on.
Ensure the port in the config.json is the same as the port you expose in the kubernetes
setup.

### Helm install

Afterwards, just compress any of the folders in this directory with:

```bash
tar -czvf folder-name.tar.gz folder-name
```

where folder-name is the name of the helm chart you want to install.

### Volume Configuration

The volume used with the konduit server should contain whatever files you want to run konduit with.
You need a config.json in order to start the server. Creating a config.json
for konduit heavily depends on your use case. For a few examples from python,
see the [client](./client) subfolder.

Use this volume for starting the konduit server. Generally, the container will
mount the volume under /usr/share within the container.

Example volumes can be found in the [konduit single model](./konduit-serving-single-model)
folder. (See volume.yml and volume-claim.yml)

Afterwards, just run:

```bash
helm install folder-name.tar.gz --set volumeName=$YOUR_VOLUME_NAME
```

Or, if you want to configure the port as well (see Port Configuration above):

```bash
helm install folder-name.tar.gz --set volumeName=$YOUR_VOLUME_NAME --set service.port=$YOUR_PORT
```

Optionally, you may also want to install a [kubernetes dashboard](https://kubernetes.io/docs/tasks/access-application-cluster/web-ui-dashboard/)
To login, follow one of [these steps](https://stackoverflow.com/questions/46664104/how-to-sign-in-kubernetes-dashboard)
and use the token configuration.

## Dashboard

This will allow specification of the volume name to be used when you want to start your konduit server.

In order to access the api within kubernetes, we need to expose the deployed pod.
One way to do this is,get the pod name and port forward the target port the container is running on:

```bash
kubectl port-forward $POD_NAME $YOUR_PORT:$YOUR_PORT
```
