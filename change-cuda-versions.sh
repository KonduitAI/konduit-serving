#!/usr/bin/env bash

#
# /* ******************************************************************************
#  *
#  *
#  * This program and the accompanying materials are made available under the
#  * terms of the Apache License, Version 2.0 which is available at
#  * https://www.apache.org/licenses/LICENSE-2.0.
#  *
#  *  See the NOTICE file distributed with this work for additional
#  *  information regarding copyright ownership.
#  * Unless required by applicable law or agreed to in writing, software
#  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
#  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
#  * License for the specific language governing permissions and limitations
#  * under the License.
#  *
#  * SPDX-License-Identifier: Apache-2.0
#  ******************************************************************************/
#





set -e

VALID_VERSIONS=( 9.2 10.0 10.1 10.2 11.0  11.2 )

usage() {
  echo "Usage: $(basename $0) [-h|--help] <cuda version to be used>
where :
  -h| --help Display this help text
  valid cuda version values : ${VALID_VERSIONS[*]}
" 1>&2
  exit 1
}

if [[ ($# -ne 1) || ( $1 == "--help") ||  $1 == "-h" ]]; then
  usage
fi

VERSION=$1

check_cuda_version() {
  for i in ${VALID_VERSIONS[*]}; do [ $i = "$1" ] && return 0; done
  echo "Invalid CUDA version: $1. Valid versions: ${VALID_VERSIONS[*]}" 1>&2
  exit 1
}


check_cuda_version "$VERSION"

case $VERSION in
  11.2)
    VERSION2="8.1"
    VERSION3="1.5.5"
    ;;
  11.1)
    VERSION2="8.0"
    VERSION3="1.5.5"
    ;;
  11.0)
    VERSION2="8.0"
    VERSION3="1.5.4"
    ;;
  10.2)
    VERSION2="7.6"
    VERSION3="1.5.3"
    ;;
  10.1)
    VERSION2="7.6"
    VERSION3="1.5.2"
    ;;
  10.0)
    VERSION2="7.4"
    VERSION3="1.5"
    ;;
  9.2)
    VERSION2="7.2"
    VERSION3="1.5"
    ;;
esac

sed_i() {
  sed -i "" -e "$1" "$2" > "$2.tmp" && mv "$2.tmp" "$2"
}

export -f sed_i

echo "Updating CUDA versions in pom.xml files to CUDA $1";

SOURCE="${BASH_SOURCE[0]}"
while [ -h "$SOURCE" ]; do # resolve $SOURCE until the file is no longer a symlink
  DIR="$( cd -P "$( dirname "$SOURCE" )" >/dev/null 2>&1 && pwd )"
  SOURCE="$(readlink "$SOURCE")"
  [[ $SOURCE != /* ]] && SOURCE="$DIR/$SOURCE" # if $SOURCE was a relative symlink, we need to resolve it relative to the path where the symlink file was located
done
BASEDIR="$( cd -P "$( dirname "$SOURCE" )" >/dev/null 2>&1 && pwd )"

#Artifact ids, ending with "-8.0", "-9.0", etc. nd4j-cuda, etc.
find "$BASEDIR" -name 'pom.xml' -not -path '*target*' \
  -exec bash -c "sed_i 's/\(artifactId>nd4j-cuda-\)[0-9.]*<\/artifactId>/\1'$VERSION'<\/artifactId>/g' '{}'" \;

#Artifact ids, ending with "-8.0-platform", "-9.0-platform", etc. nd4j-cuda-platform, etc.
find "$BASEDIR" -name 'pom.xml' -not -path '*target*' \
  -exec bash -c "sed_i 's/\(artifactId>nd4j-cuda-\)[0-9.]*-platform<\/artifactId>/\1'$VERSION'-platform<\/artifactId>/g' '{}'" \;

#Artifact ids, ending with "-8.0-preset", "-9.0-preset", etc. nd4j-cuda-preset, etc.
find "$BASEDIR" -name 'pom.xml' -not -path '*target*' \
  -exec bash -c "sed_i 's/\(artifactId>nd4j-cuda-\)[0-9.]*-preset<\/artifactId>/\1'$VERSION'-preset<\/artifactId>/g' '{}'" \;

#Profiles ids, ending with "-8.0", "-9.0", etc. test-nd4j-cuda, etc.
find "$BASEDIR" -name 'pom.xml' -not -path '*target*' \
  -exec bash -c "sed_i 's/\(test-nd4j-cuda-\)[0-9.]*</\1'$VERSION'</g' '{}'" \;

#Artifact ids, ending with "-8.0", "-9.0", etc. deeplearning4j-cuda, etc.
find "$BASEDIR" -name 'pom.xml' -not -path '*target*' \
  -exec bash -c "sed_i 's/\(artifactId>deeplearning4j-cuda-\)[0-9.]*<\/artifactId>/\1'$VERSION'<\/artifactId>/g' '{}'" \;

#Artifact ids, ending with "-8.0-platform", "-9.0-platform", etc. deeplearning4j-cuda-platform, etc.
find "$BASEDIR" -name 'pom.xml' -not -path '*target*' \
  -exec bash -c "sed_i 's/\(artifactId>deeplearning4j-cuda-\)[0-9.]*-platform<\/artifactId>/\1'$VERSION'-platform<\/artifactId>/g' '{}'" \;

#CUDA versions, like <cuda.version>9.1</cuda.version>
find "$BASEDIR" -name 'pom.xml' -not -path '*target*' \
  -exec bash -c "sed_i 's/\(cuda.version>\)[0-9.]*<\/cuda.version>/\1'$VERSION'<\/cuda.version>/g' '{}'" \;

#cuDNN versions, like <cudnn.version>7.0</cudnn.version>
find "$BASEDIR" -name 'pom.xml' -not -path '*target*' \
  -exec bash -c "sed_i 's/\(cudnn.version>\)[0-9.]*<\/cudnn.version>/\1'$VERSION2'<\/cudnn.version>/g' '{}'" \;

#JavaCPP versions, like <javacpp-presets.cuda.version>1.4</javacpp-presets.cuda.version>
find "$BASEDIR" -name 'pom.xml' -not -path '*target*' \
  -exec bash -c "sed_i 's/\(javacpp-presets.cuda.version>\).*<\/javacpp-presets.cuda.version>/\1'$VERSION3'<\/javacpp-presets.cuda.version>/g' '{}'" \;

echo "Done updating CUDA versions.";
