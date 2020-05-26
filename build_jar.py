import argparse
import subprocess
from shutil import copyfile
import os
import sys
import re

os_choices = [
    "windows-x86_64",
    "linux-x86_64",
    "macosx-x86_64",
    "linux-armhf",
]


def get_platform():
    if sys.platform.startswith("win32"):
        return "windows-x86_64"
    elif sys.platform.startswith("darwin"):
        return "macosx-x86_64"
    elif sys.platform.startswith("linux"):
        return "linux-x86_64"
    else:
        raise RuntimeError("Please specify '--os'. Possible values are: " + os_choices.__str__())


if __name__ == "__main__":
    """
    Example:

    ./build_jar.py --os linux-x86_64
    """
    parser = argparse.ArgumentParser(description="Build a Konduit JAR.")

    parser.add_argument(
        "--os",
        "-p",
        type=str,
        default=get_platform(),
        help="the javacpp.platform to use: Possible values are: " + os_choices.__str__(),
    )

    parser.add_argument(
        "--spin", "-s",
        type=str,
        default="all",
        choices=["minimal", "python", "pmml", "all"],
        help="whether to bundle Python, PMML, both or neither. Python bundling is"
        + "not encouraged with ARM, and PMML bundling is not encouraged if agpl"
        + "license is an issue.",
    )

    parser.add_argument(
        "--chip", "-c",
        type=str,
        default="cpu",
        choices=["cpu", "gpu", "arm"],
        help="Specifies the chip architecture which could be cpu, gpu (CUDA) or arm."
    )

    parser.add_argument(
        "--cuda-version", "-cv",
        type=str,
        default="10.2",
        choices=["10.0", "10.1", "10.1-redist", "10.2", "10.2-redist"],
        help="Specifies the CUDA version to compile when `--chip == gpu`",
    )

    parser.add_argument(
        "--options", "-o",
        action='append',
        help="Extra java property to add to the build command. Can be used multiple times."
             "Format: -o <property1.name>=<property1.value>"
    )

    parser.add_argument(
        "--source",
        "-so",
        type=str,
        help="the path to the model server",
        default="."
    )

    parser.add_argument(
        "--target",
        "-t",
        type=str,
        help="the path to the model server output",
        default="konduit.jar",
    )

    parser.add_argument(
        "--show_build_command",
        "-sbc",
        help="show build command without running it",
        action="store_true",
    )

    args = parser.parse_args()

    command = [args.source + os.sep + "mvnw", "-Puberjar,tensorflow", "clean", "install", "-Dmaven.test.skip=true",
               "-Djavacpp.platform=" + args.os, "-Dchip={}".format(args.chip)]

    if args.chip == "gpu":
        command.append("-Pgpu,intel")

    if args.spin == "all" or args.spin == "python":
        command.append("-Ppython")
    if args.spin == "all" or args.spin == "pmml":
        command.append("-Ppmml")

    command.append("-Dspin.version={}".format(args.spin))

    if args.chip == "gpu":
        command.append("-Dcuda.version={}".format(args.cuda_version))

        if args.cuda_version == "10.0":
            command.append("-Dcuda.javacpp.version=10.0-7.4-1.5")
        elif args.cuda_version == "10.1":
            command.append("-Dcuda.javacpp.version=10.1-7.6-1.5.2")
        elif args.cuda_version == "10.1-redist":
            command.append("-Pcuda-10.1-redist")
            command.append("-Dcuda.javacpp.version=10.1-7.6-1.5.2")
        elif args.cuda_version == "10.2-redist":
            command.append("-Pcuda-10.2-redist")

    if args.options:
        for option in args.options:
            command.append("-D{}".format(option))

    command.append("-Denforcer.skip=true")

    with open(os.path.join(args.source, "pom.xml"), "r") as pom:
        content = pom.read()
        regex = r"<version>(\d+.\d+.\d+\S*)</version>"
        version = re.findall(regex, content)

    if args.show_build_command:
        print(" ".join(command))
        exit(0)

    print("Running command: " + " ".join(command))
    subprocess.call(command, shell=sys.platform.startswith("win"), cwd=args.source)

    # Copy the jar file to the path specified by the "target" argument

    if args.chip == "gpu":
        uberjar_file = "konduit-serving-uberjar-{}-{}-{}-gpu-cuda-{}.jar".format(
            version[0], args.spin, args.os, args.cuda_version
        )
    else:
        uberjar_file = "konduit-serving-uberjar-{}-{}-{}-{}.jar".format(
            version[0], args.spin, args.os, args.chip
        )

    copyfile(
        os.path.join(
            args.source,
            "konduit-serving-uberjar",
            "target",
            uberjar_file,
        ),
        os.path.join(args.source, args.target),
    )

    # Copy the built jar file to the "python/tests" folder if it exists.
    if os.path.isdir(os.path.join(args.source, "python", "tests")):
        copyfile(
            os.path.join(args.source, args.target),
            os.path.join(args.source, "python", "tests", "konduit.jar"),
        )
