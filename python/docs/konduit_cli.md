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
  init           Initialize the konduit-python CLI.
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
  input_data_type: NUMPY
  output_data_type: NUMPY
  log_timings: True
  extra_start_args: -Xmx8g
  jar_path: konduit.jar
steps:
  python_step:
    type: PYTHON
    python_path: .
    python_code_path: ./simple.py
    python_inputs:
      first: NDARRAY
    python_outputs:
      second: NDARRAY
client:
    port: 1337
```

This Konduit experiment describes how your model will be run, what inputs it takes and
what types of output it will generate. In essence, this configuration will run the script
`simple.py` on input data that you can specify. To serve this Konduit pipeline you can
just run:

```shell script
konduit serve --config yaml/konduit.yaml
```

and to get predictions from it you can use:

```shell script
konduit predict-numpy --config yaml/konduit.yaml --numpy_data ../data/input-0.npy 

```

Finally, to shut down the Konduit server again after you're donw with it, simply use

```shell script
konduit stop-server --config yaml/konduit.yaml
``` 