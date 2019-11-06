cd "$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )" || exit

rm konduit.jar # Removing previously created jar file from the konduit-python folder

cd ../..

rm konduit.jar # Removing previously created jar file from the root folder

python build_jar.py --os linux-x86_64

mv konduit.jar docker/konduit-python/konduit.jar

cd docker/konduit-python || exit

docker build -t konduit/konduit-python .