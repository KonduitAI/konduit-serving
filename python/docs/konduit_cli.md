# Konduit CLI for quick Python experiments

Install with `python setup.py install` from this folder. This will expose a CLI tool called
`konduit`. Assuming you have put a `konduit.jar` in the `tests` folder as described
in the main `README.md`, you can test this tool as follows:

```shell script
cd tests
konduit --help
```

which should prompt all currently available commands, i.e.

```text
Usage: konduit [OPTIONS] COMMAND [ARGS]...

Options:
  --help  Show this message and exit.

Commands:
  build          Build the underlying konduit.jar (again).
  init           Initialize the konduit CLI.
  predict-numpy  Get predictions for your pipeline from numpy input.
  serve          Serve a pipeline from a konduit.yaml
  stop-server    Stop the Konduit server associated with a given config...
```

For more help on individual commands you can do
```shell script
konduit serve --help
```

to get help for the `serve` command (and all others in the same way)

```text
Usage: konduit serve [OPTIONS]

  Serve a pipeline from a konduit.yaml

Options:
  --config TEXT          Relative or absolute path to your konduit serving YAML
                       file.
  --start_server TEXT  Whether to start the server instance after 
                       initialization.
  --help               Show this message and exit.
```

## Basic usage

You should be in the `tests` folder still. There are a lot of YAML files in the `yaml` folder
to test Konduit with. Let's say we want to serve the pipeline described in `yaml/konduit.yaml`.
Let's have a look at it first:

```yaml
serving:
  http_port: 1337
steps:
  tensorflow_step:
    type: TENSORFLOW
    model_loading_path: bert_mrpc_frozen.pb
    input_names:
      - IteratorGetNext:0
      - IteratorGetNext:1
      - IteratorGetNext:4
    output_names:
      - loss/Softmax
    parallel_inference_config:
      workers: 1
    input_data_types:
      IteratorGetNext:0: INT32
      IteratorGetNext:1: INT32
      IteratorGetNext:4: INT32
```

This Konduit experiment describes how your model will be run, what inputs it takes and
what types of output it will generate. In essence, this configuration will serve a tensorflow BERT model. 
To serve this Konduit pipeline you can just run:

```shell script
konduit serve --config yaml/konduit_tensorflow.yaml
```

and to get predictions from it you can use:

```shell script
konduit predict-numpy --port 1337 --numpy_data ../data/input-0.npy,../data/input-1.npy,../data/input-4.npy --input_names IteratorGetNext:0,IteratorGetNext:1,IteratorGetNext:4
```

Finally, to shut down the Konduit server again after you're done with it, simply use

```shell script
konduit stop-server --pid 117468
``` 