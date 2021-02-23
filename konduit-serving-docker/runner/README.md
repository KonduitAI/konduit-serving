A runner docker image to deploy a konduit-serving server from a directory containing `setup.sh`, `environment.yml`, `requirements.txt` and `config.yml` and other supporting files. `environment.yml` is supposed to contain the conda + pip dependencies combined for a conda environment. `requirements.txt` contains the pip related dependencies. Whereas, `config.yml` contains the yaml configuration for a konduit-serving server. Finally, the `setup.sh` script takes care of any missing installations/setup in the docker image.

# Building Image
To build a CPU version of the image, simply run:
```shell
bash build.sh
```

# Usage
To use the image you can navigate to a directory containing the supporting files and attach it as a volume and then run the server with the following command:
```shell
docker run --env KONDUIT_SERVING_PORT=9008 -p 9008:9008 -v $(pwd):/root/konduit/work konduit/konduit-serving
```

# Predict
After the docker image is successfully running, you can use cURL to send requests to the server:
```shell
curl -F "image=@test_image.jpg" localhost:9008/predict
```