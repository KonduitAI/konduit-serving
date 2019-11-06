import argparse
import subprocess
from shutil import copyfile
import os
import re
from distutils.util import strtobool

with open('pom.xml', 'r') as pom:
    content = pom.read()
    regex = r"<version>(\d+.\d+.\d+)</version>"
    results = re.findall(regex, content)

PROJECT_VERSION = results[0]

if __name__ == '__main__':
    '''
    Example:

    ./build_jar.py --os linux-x86_64
    '''
    parser = argparse.ArgumentParser(description='Build a Konduit JAR.')

    parser.add_argument('--os', type=str,
                        required=True,
                        choices=['windows-x86_64', 'linux-x86_64', 'linux-x86_64-gpu',
                                 'macosx-x86_64', 'linux-armhf', 'windows-x86_64-gpu'],
                        help='the javacpp.platform to use: windows-x86_64,linux-x86_64,linux-x86_64-gpu'
                             ' macosx-x86_64,linux-armhf,windows-x86_64-gpu ')

    parser.add_argument('--usePython', type=str, default='true',
                        help='whether to bundle python '
                             'or not (typically not encouraged with arm')

    parser.add_argument('--usePmml', type=str, default='true',
                        help='whether to use pmml or not,'
                             ' not encouraged if agpl license is an issue')

    parser.add_argument('--source', type=str,
                        help='the path to the model server', default='.')

    parser.add_argument('--target', type=str,
                        help='the path to the model server output', default='konduit.jar')

    args = parser.parse_args()
    command = args.source + os.sep + 'mvnw -Puberjar clean install -Dmaven.test.skip=true'
    command += ' -Djavacpp.platform=' + args.os
    if 'arm' in args.os:
        command += ' -Dchip=arm -Parm'
    elif 'gpu' in args.os:
        command += ' -Dchip=gpu -Pgpu'
    else:
        command += ' -Dchip=cpu -Pcpu'
    if strtobool(args.usePython):
        command += ' -Ppython'
    if strtobool(args.usePmml):
        command += ' -Ppmml'

    print('Running command: ' + command)
    subprocess.run(command, cwd=args.source, shell=True, check=True)
    copyfile(
        os.path.join(args.source, 'konduit-serving-uberjar', 'target',
                     'konduit-serving-uberjar-{}-bin.jar'.format(PROJECT_VERSION)),
        args.target
    )
