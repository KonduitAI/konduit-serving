set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

cd "${SCRIPT_DIR}/../.."

KONDUIT_VERSION=$(mvn -q -Dexec.executable="echo" -Dexec.args='${project.version}' --non-recursive exec:exec)

cd "${SCRIPT_DIR}"

if [[ -d "${SCRIPT_DIR}"/konduit ]]
then
  rm -rf "${SCRIPT_DIR}"/konduit
fi

DISTRO_DIR="${SCRIPT_DIR}"/../../konduit-serving-tar/target/konduit-serving-tar-"${KONDUIT_VERSION}"-dist
if [[ ! -d "${DISTRO_DIR}" ]]
then
  bash "${SCRIPT_DIR}"/../../build.sh CPU linux tar
fi

cp -r "${DISTRO_DIR}" "${SCRIPT_DIR}"/konduit

docker build --tag konduit/konduit-serving-builder:latest .
