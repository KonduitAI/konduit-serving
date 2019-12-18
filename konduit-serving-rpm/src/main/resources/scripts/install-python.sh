#!/usr/bin/env bash

DEFAULT_PYTHON_MAJOR_VERSION="3"
DEFAULT_PYTHON_VERSION="3.7"
DEFAULT_CONDA_VERSION="4.7.12.1"
DEFAULT_KONDUIT_SERVING_BACKEND="cpu"

PYTHON_MAJOR_VERSION="${PYTHON_MAJOR_VERSION:-$DEFAULT_PYTHON_MAJOR_VERSION}"
PYTHON_VERSION="${PYTHON_VERSION:-$DEFAULT_PYTHON_VERSION}"
CONDA_VERSION="${CONDA_VERSION:-$DEFAULT_CONDA_VERSION}"
KONDUIT_SERVING_BACKEND="${KONDUIT_SERVING_BACKEND:-$DEFAULT_KONDUIT_SERVING_BACKEND}"

NUMPY_VERSION="1.16.4"  # For compatibility with python 2
JUPYTER_VERSION="1.0.0"
SCIPY_VERSION="1.3.3"
REQUESTS_VERSION="2.22.0"
PANDAS_VERSION="0.24.2" # For compatibility with python 2
TENSORFLOW_VERSION="1.15.0"
KERAS_VERSION="2.2.4"
KONDUIT_VERSION="0.1.2"
SKLEARN_VERSION="0.22"
MATPLOTLIB_VERSION="3.1.2"
PYTORCH_VERSION="1.3.1"
TORCHVISION_VERSION="0.4.2"
CPUONLY_VERSION="1.0"
CUDATOOLKIT_VERSION="10.1"

echo "-------------------------------------------------"
echo "Miniconda Version: ${CONDA_VERSION}"
echo "Python Version: ${PYTHON_VERSION}"
echo "Konduit Serving Backend: ${KONDUIT_SERVING_BACKEND}"
echo "-------------------------------------------------"

bootstrap_conda() {
    local py_major="$PYTHON_MAJOR_VERSION"
    local conda_version="$CONDA_VERSION"

    local miniconda_loc=${MINICONDA_BIN:-/tmp/miniconda.sh}

    if ! [[ -d "${KONDUIT_SERVING_HOME}/miniconda" ]]; then
        declare miniconda_url
        case "$(uname -s | tr '[:upper:]' '[:lower:]')" in
            linux) miniconda_url="https://repo.continuum.io/miniconda/Miniconda${py_major}-${conda_version}-Linux-x86_64.sh";;
            darwin) miniconda_url="https://repo.continuum.io/miniconda/Miniconda${py_major}-${conda_version}-MacOSX-x86_64.sh";;
        esac

        echo Downloading Miniconda...
        test -e "${miniconda_loc}" || curl -sS -o "${miniconda_loc}" "$miniconda_url"
        echo Installing Miniconda...
        bash "${miniconda_loc}" -bfp "$KONDUIT_SERVING_HOME/miniconda"

        rm -rf /tmp/miniconda.sh
    fi

    export PATH="${KONDUIT_SERVING_HOME}/miniconda/bin:$PATH"
    hash -r

    # Install dependencies for CPU
    if [ "${KONDUIT_SERVING_BACKEND}" == "cpu" ]; then
      if ! [[ -d "${KONDUIT_SERVING_HOME}/miniconda/envs/cpu" ]]; then
          conda create -q -n cpu python="${PYTHON_VERSION}"
      fi

      conda install -p "${KONDUIT_SERVING_HOME}/miniconda/envs/cpu" -y --copy --override-channels -c conda-forge -c anaconda -c pytorch -c konduitai \
                        numpy="${NUMPY_VERSION}" \
                        requests="${REQUESTS_VERSION}" \
                        jupyter="${JUPYTER_VERSION}" \
                        scipy="${SCIPY_VERSION}" \
                        scikit-learn="${SKLEARN_VERSION}" \
                        pandas="${PANDAS_VERSION}" \
                        matplotlib="${MATPLOTLIB_VERSION}" \
                        tensorflow="${TENSORFLOW_VERSION}" \
                        keras="${KERAS_VERSION}" \
                        pytorch="${PYTORCH_VERSION}" \
                        torchvision="${TORCHVISION_VERSION}" \
                        cpuonly="${CPUONLY_VERSION}" \
                        konduit"${KONDUIT_VERSION}"
    fi

    # Install dependencies for GPU
    if [ "${KONDUIT_SERVING_BACKEND}" == "gpu" ]; then
      if ! [[ -d $KONDUIT_SERVING_HOME/miniconda/envs/gpu ]]; then
          conda create -q -n gpu python="${PYTHON_VERSION}"
      fi

      conda install -p "${KONDUIT_SERVING_HOME}/miniconda/envs/gpu" -y --copy --override-channels -c conda-forge -c anaconda -c pytorch -c konduitai \
                      numpy="${NUMPY_VERSION}" \
                      requests="${REQUESTS_VERSION}" \
                      jupyter="${JUPYTER_VERSION}" \
                      scipy="${SCIPY_VERSION}" \
                      scikit-learn="${SKLEARN_VERSION}" \
                      pandas="${PANDAS_VERSION}" \
                      matplotlib="${MATPLOTLIB_VERSION}" \
                      tensorflow-gpu="${TENSORFLOW_VERSION}" \
                      keras="${KERAS_VERSION}" \
                      pytorch="${PYTORCH_VERSION}" \
                      torchvision="${TORCHVISION_VERSION}" \
                      cudatoolkit="${CUDATOOLKIT_VERSION}" \
                      konduit"${KONDUIT_VERSION}"
    fi

    conda clean --all

    conda info --all
    conda list
}

bootstrap_conda
