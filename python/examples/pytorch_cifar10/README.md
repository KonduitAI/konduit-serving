# Serving a CIFAR10 model on konduit

This folder contains a working example of serving a pytorch-based model on konduit.

## Installation.

Using Python3.7 create a virtual environment for this project and install the
dependencies.

```shell script
python -m venv venv
source venv/bin/active # venv\Scripts\activate.bat on Windows
pip install cython
# If you are on windows
# pip3 install torch===1.3.1 torchvision===0.4.2 -f https://download.pytorch.org/whl/torch_stable.html
pip install -r requirements.txt
```

## Building the model.
Next you can run the train script to train the model.  It'll download the data 
automatically.

```shell script
cd model
python train.py
cd ..
```

After a few minutes you should see the `model.pt` file get created.

## Serving the model on konduit.

Update the `python_path` setting in the konduit.yml file to match the expected
path separators for your Operating System.

Now it's ready to ai.konduit.serving.deploy, run the following command to install and start the server:

```shell script
konduit init --os windows-x86_64
konduit serve --config konduit.yml
```

Change the --os parameter to fit your machine's OS.

## Getting predictions from the server.

To send sample requests to the server use the `client.py` script:

```shell script
python client.py
```

After running it you should see some performance numbers.