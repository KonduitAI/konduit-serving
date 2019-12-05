import argparse
import subprocess
from shutil import copyfile
import os
import sys
import re
from distutils.util import strtobool


if __name__ == "__main__":
    """
    Example:

    ./build_jar.py --os linux-x86_64
    """
    parser = argparse.ArgumentParser(description="Build a Konduit JAR.")

    parser.add_argument(
        "--os",
        type=str,
        required=True,
        choices=[
            "windows-x86_64",
            "linux-x86_64",
            "linux-x86_64-gpu",
            "macosx-x86_64",
            "linux-armhf",
            "windows-x86_64-gpu",
        ],
        help="the javacpp.platform to use: windows-x86_64,linux-x86_64,linux-x86_64-gpu"
        " macosx-x86_64,linux-armhf,windows-x86_64-gpu ",
    )

    parser.add_argument(
        "--usePython",
        type=str,
        default="true",
        help="whether to bundle python " "or not (typically not encouraged with arm",
    )

    parser.add_argument(
        "--usePmml",
        type=str,
        default="true",
        help="whether to use pmml or not,"
        " not encouraged if agpl license is an issue",
    )

    parser.add_argument(
        "--source", type=str, help="the path to the model server", default="."
    )

    parser.add_argument(
        "--target",
        type=str,
        help="the path to the model server output",
        default="konduit.jar",
    )

    args = parser.parse_args()

    command = [
        args.source + os.sep + "mvnw",
        "-Puberjar",
        "clean",
        "install",
        "-Dmaven.test.skip=true",
        "-Djavacpp.platform=" + args.os,
    ]

    if "arm" in args.os:
        command.append("-Dchip=arm")
    elif "gpu" in args.os:
        command.append("-Dchip=gpu")
    else:
        command.append("-Dchip=cpu")

    if strtobool(args.usePython):
        command.append("-Ppython")
    if strtobool(args.usePmml):
        command.append("-Ppmml")

    with open(os.path.join(args.source, "pom.xml"), "r") as pom:
        content = pom.read()
        regex = r"<version>(\d+.\d+.\d+)</version>"
        version = re.findall(regex, content)

    print("Running command: " + " ".join(command))
    subprocess.call(command, shell=sys.platform.startswith('win'), cwd=args.source)

    # Copy the jar file to the path specified by the "target" argument
    copyfile(
        os.path.join(
            args.source,
            "konduit-serving-uberjar",
            "target",
            "konduit-serving-uberjar-{}-bin.jar".format(version[0]),
        ),
        os.path.join(args.source, args.target),
    )

    # Copy the built jar file to the "python/tests" folder if it exists.
    if os.path.isdir(os.path.join(args.source, "python", "tests")):
        copyfile(os.path.join(args.source, args.target), os.path.join(args.source, "python", "tests", "konduit.jar"))
