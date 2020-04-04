import argparse
import subprocess
from shutil import copyfile
import os
import sys
import re

os_choices = [
    "windows-x86_64",
    "linux-x86_64",
    "linux-x86_64-gpu",
    "macosx-x86_64",
    "linux-armhf",
    "windows-x86_64-gpu",
]


def get_platform():
    if sys.platform.startswith("win32"):
        return "windows-x86_64"
    elif sys.platform.startswith("darwin"):
        return "macosx-x86_64"
    elif sys.platform.startswith("linux"):
        return "linux-x86_64"
    else:
        raise Exception(
            "Please specify '--os'. Possible values are: " + os_choices.__str__()
        )


if __name__ == "__main__":
    """
    Example:

    ./build_jar.py --os linux-x86_64
    """
    parser = argparse.ArgumentParser(description="Build a Konduit JAR.")

    parser.add_argument(
        "--os",
        type=str,
        default=get_platform(),
        choices=os_choices,
        help="the javacpp.platform to use: Possible values are: " + os_choices.__str__(),
    )

    parser.add_argument(
        "--spin",
        type=str,
        default="all",
        choices=["minimal", "python", "pmml", "all"],
        help="whether to bundle Python, PMML, both or neither. Python bundling is"
        + "not encouraged with ARM, and PMML bundling is not encouraged if agpl"
        + "license is an issue.",
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

    parser.add_argument(
        "--show_build_command",
        "-sbc",
        help="show build command without running it",
        action="store_true",
    )

    args = parser.parse_args()

    platform = args.os.replace("-gpu", "")
    command = [
        args.source + os.sep + "mvnw",
        "-Puberjar,tensorflow",
        "clean",
        "install",
        "-Dmaven.test.skip=true",
        "-Djavacpp.platform=" + platform,
    ]

    if "arm" in args.os:
        arch = "arm"
    elif "gpu" in args.os:
        arch = "gpu"
    else:
        arch = "cpu"

    command.append("-Dchip={}".format(arch))

    if args.spin == "all" or args.spin == "python":
        command.append("-Ppython")
    if args.spin == "all" or args.spin == "pmml":
        command.append("-Ppmml")

    command.append("-Dspin.version={}".format(args.spin))

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
    if arch != "gpu":
        copyfile(
            os.path.join(
                args.source,
                "konduit-serving-uberjar",
                "target",
                "konduit-serving-uberjar-{}-{}-{}-{}.jar".format(
                    version[0], args.spin, args.os, arch
                ),
            ),
            os.path.join(args.source, args.target),
        )
    else:
        copyfile(
            os.path.join(
                args.source,
                "konduit-serving-uberjar",
                "target",
                "konduit-serving-uberjar-{}-{}-{}.jar".format(
                    version[0], args.spin, args.os
                ),
            ),
            os.path.join(args.source, args.target),
        )

    # Copy the built jar file to the "python/tests" folder if it exists.
    if os.path.isdir(os.path.join(args.source, "python", "tests")):
        copyfile(
            os.path.join(args.source, args.target),
            os.path.join(args.source, "python", "tests", "konduit.jar"),
        )
