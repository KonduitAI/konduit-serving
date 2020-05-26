#  /* ******************************************************************************
#   * Copyright (c) 2020 Konduit K.K.
#   *
#   * This program and the accompanying materials are made available under the
#   * terms of the Apache License, Version 2.0 which is available at
#   * https://www.apache.org/licenses/LICENSE-2.0.
#   *
#   * Unless required by applicable law or agreed to in writing, software
#   * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
#   * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
#   * License for the specific language governing permissions and limitations
#   * under the License.
#   *
#   * SPDX-License-Identifier: Apache-2.0
#   ******************************************************************************/

import io
import os
import re
import sys
import requests
import argparse
import itertools
from time import time, sleep
from datetime import datetime
from hurry.filesize import size
from requests.auth import HTTPBasicAuth
from subprocess import check_output, Popen, PIPE, STDOUT

valid_platforms = {"windows-x86_64", "linux-x86_64", "macosx-x86_64", "linux-armhf"}
valid_chips = {"cpu", "arm", "cuda-10.0", "cuda-10.1", "cuda-10.1-redist", "cuda-10.2", "cuda-10.2-redist"}
valid_spins = {"minimal", "python", "pmml", "all"}

target_directory = "builds"
logs_directory = "builds/logs"
auth_file_path = os.path.expanduser("~/TOKEN")

if not os.path.isdir(target_directory):
    os.mkdir(target_directory)

if not os.path.isdir(logs_directory):
    os.mkdir(logs_directory)


class CancelledError(Exception):
    def __init__(self, msg):
        self.msg = msg
        Exception.__init__(self, msg)

    def __str__(self):
        return self.msg

    __repr__ = __str__


class BufferReader(io.BytesIO):
    def __init__(self, buf=b'',
                 callback=None,
                 cb_args=(),
                 cb_kwargs=None):
        self._callback = callback
        self._cb_args = cb_args
        self._cb_kwargs = {} if cb_kwargs is None else cb_kwargs
        self._progress = 0
        self._len = len(buf)
        io.BytesIO.__init__(self, buf)

    def __len__(self):
        return self._len

    def read(self, n=-1):
        chunk = io.BytesIO.read(self, n)
        self._progress += int(len(chunk))
        self._cb_kwargs.update({
            'total_size': self._len,
            'total_read': self._progress
        })
        if self._callback:
            try:
                self._callback(*self._cb_args, **self._cb_kwargs)
            except Exception:  # catches exception from the callback
                raise CancelledError('The upload was cancelled.')
        return chunk


def progress(total_size=None, total_read=None):
    if total_read >= total_size:
        sys.stdout.write('\r')
        sys.stdout.flush()
    else:
        bars = 50
        completed = int(total_read / total_size * bars)
        sys.stdout.write('\rUploading: [{}{}]'.format('█' * completed, '.' * (bars - completed)) +
                         "{:<20}".format(" ({}/{})".format(size(total_read), size(total_size))))
        sys.stdout.flush()


def get_chip(chip):
    return "gpu" if "cuda" in chip else chip


def get_chip_artifact_string(chip):
    return ("gpu-" + chip) if "cuda" in chip else chip


def get_cuda_version(chip):
    split = chip.split("-")
    if len(split) < 2:
        return "10.2"
    else:
        return split[1]


if __name__ == "__main__":
    """
    Example:

    python create-binaries.py -p windows-x86_64,linux-x86_64 -c cpu,cuda-10.1 -s minimal,all --upload
    """

    parser = argparse.ArgumentParser(description="Create specified binaries and upload as well if specified.")

    parser.add_argument(
        "--platforms",
        "-p",
        type=str,
        default="windows-x86_64,macosx-x86_64,linux-x86_64",
        help="Comma separated list of platforms that the binaries need to be compiled for. Accepted values in "
             "the list are one of: {}. Default: '%(default)s'".format(valid_platforms)
    )

    parser.add_argument(
        "--chips",
        "-c",
        type=str,
        default="cpu,cuda-10.0,cuda-10.1,cuda-10.2",
        help="Comma separated list of chips to generate binaries for. Accepted values in the list are one of:"
             " {}. Default: '%(default)s'".format(valid_chips)
    )

    parser.add_argument(
        "--spins",
        "-s",
        type=str,
        default="minimal,python,pmml,all",
        help="Comma separated list of spins to generate binaries for. Accepted values in the list are one of:"
             " {}. Default: '%(default)s'".format(valid_spins)
    )

    parser.add_argument(
        "--upload",
        "-u",
        action='store_true',
        help="A flag to specify if the generated binaries are to be uploaded for distribution"
    )

    parser.add_argument(
        "--just-upload",
        "-ju",
        action='store_true',
        help="A flag to specify if the build needs to be skipped before file uploads. Useful if the binaries are "
             "already build and just needs uploading"
    )

    args = parser.parse_args()

    error = False
    errors = ["", "Please resolve the following issues to continue..."]

    error_index = 0

    platforms = set(args.platforms.split(','))
    if not platforms <= valid_platforms:
        error = True
        error_index += 1
        errors.append(" ({}): --platform or -p should contain comma-separated values from the set: {}"
                      .format(error_index, valid_platforms))

    chips = set(args.chips.split(','))
    if not chips <= valid_chips:
        error = True
        error_index += 1
        errors.append(" ({}): --chips or -c should contain comma-separated values from the set: {}"
                      .format(error_index, valid_chips))

    spins = set(args.spins.split(','))
    if not spins <= valid_spins:
        error = True
        error_index += 1
        errors.append(" ({}): --spins or -s should contain comma-separated values from the set: {}"
                      .format(error_index, valid_spins))

    just_upload = args.just_upload
    upload = args.upload or just_upload

    if upload:
        if not os.path.exists(auth_file_path):
            error = True
            error_index += 1
            errors.append(" ({}): Define a file at {} in the format of 'username:github_personal_access_token' "
                          "for uploading binaries".format(error_index, auth_file_path))
        else:
            with open(auth_file_path) as auth_file:
                auth_data = auth_file.read().split(":")
                if len(auth_data) == 2:
                    auth = HTTPBasicAuth(auth_data[0].strip(), auth_data[1].strip())
                else:
                    error = True
                    error_index += 1
                    errors.append(" ({}): Invalid auth format in {}. Should be in the format of "
                                  "'username:github_personal_access_token'".format(error_index, auth_file_path))

    if error:
        errors.append("")
        print("\n".join(errors))
        exit(-1)

    with open("pom.xml", "r") as pom:
        content = pom.read()
        regex = r"<version>(\d+.\d+.\d+\S*)</version>"
        project_version = re.findall(regex, content)[0]

    all_artifact_permutations = list(itertools.product(*[spins, platforms, chips]))

    # Filtering out permutations where platform is a macosx derivative and platform is a cuda derivative.
    artifact_permutations = sorted(list(filter(lambda permutation:
                                               not ("cuda" in permutation[2] and "macosx" in permutation[1]),
                                               all_artifact_permutations)))

    # Filtering out permutations where platform is a macosx derivative and platform is a cuda derivative.
    skipped_artifact_permutations = sorted(list(filter(lambda permutation:
                                                       "cuda" in permutation[2] and "macosx" in permutation[1],
                                                       all_artifact_permutations)))

    artifact_name_pattern = "konduit-serving-uberjar-{}{}".format(project_version, "-{}-{}-{}.jar")

    artifact_names = [artifact_name_pattern.format(spin,
                                                   platform,
                                                   get_chip_artifact_string(chip))
                      for (spin, platform, chip) in artifact_permutations]
    skipped_artifact_names = [artifact_name_pattern.format(spin,
                                                           platform,
                                                           get_chip_artifact_string(chip))
                              for (spin, platform, chip) in skipped_artifact_permutations]

    artifact_names_indexed = ["({:02d}): {}".format(index + 1, name) for index, name in enumerate(artifact_names)]
    skipped_artifact_names_indexed = ["({:02d}): {}".format(index + 1, name) for index, name in
                                      enumerate(skipped_artifact_names)]

    if len(skipped_artifact_names_indexed) > 0:
        print("\nSkipped following {} artifact(s) due to CUDA not available for 'macosx-x86_64'\n---\n{}"
              .format(len(skipped_artifact_names_indexed),
                      "\n".join(skipped_artifact_names_indexed)))

    if len(artifact_names_indexed) > 0:
        print("---\nThe following {} artifact(s) will be created at location: {}\n---\n{}"
              .format(len(artifact_names),
                      os.path.abspath(target_directory),
                      "\n".join(artifact_names_indexed)))
    else:
        print("---\nNo artifacts can be generated with the given inputs. See the skipped artifacts abive to verify if "
              "the expected artifacts are skipped.\n")
        exit()

    for index, artifact in enumerate(artifact_permutations):
        platform = artifact[1]
        chip = artifact[2]
        spin = artifact[0]

        artifact_name = artifact_name_pattern.format(spin, platform, get_chip_artifact_string(chip))
        output_path = os.path.join(target_directory, artifact_name)
        command = ["python", "build_jar.py", "-p", platform, "-c",
                   get_chip(chip), "-s", spin, "-cv", get_cuda_version(chip), "--target", output_path]
        check_output_command = command.copy()
        check_output_command.append("-sbc")

        print("------------------------------")
        maven_build_command = check_output(check_output_command, shell=sys.platform.startswith("win")).decode('utf-8') \
            .replace('\n', ' ').replace('\r', '')
        print("({:02d}): Compiling for ({} | {} | {})\n---".format(index + 1, platform, chip, spin))

        start_time = time()
        with open(os.path.join(logs_directory,
                               "{:02d}-{}".format(index + 1, artifact_name.replace(".jar", ".log"))),
                  'w') \
                as logs_output_file:

            print("Logs: {}".format(os.path.abspath(logs_output_file.name)))
            print("Using: {}".format(maven_build_command))

            if not just_upload:
                process = Popen(args=command, shell=sys.platform.startswith("win"),
                                stdout=PIPE,
                                stderr=STDOUT)

                while True:
                    output = process.stdout.readline()
                    if output == b'' and process.poll() is not None:
                        sys.stdout.write('\r')
                        sys.stdout.flush()
                        break
                    else:
                        logs_output_file.write(output.decode("utf-8"))

                        found = re.findall(r"\[INFO\]\WBuilding\W(.*)\W{}\W+\[(\d+)/(\d+)\]"
                                           .format(project_version.replace(".", "\\.")), output.decode("utf-8"),
                                           re.MULTILINE)
                        if len(found) > 0:
                            found = found[-1]
                            current_artifact = found[0]
                            done = int(found[1])
                            total = int(found[2])
                            if done != -1 and total != -1 and done <= total and current_artifact:
                                sys.stdout.write('\rBuilding: [{}{}]'.format('█' * done, '.' * (total - done)) +
                                                 (" [{}/{}] {:<40}".format(done,
                                                                           total,
                                                                           "({})".format(current_artifact))))
                                sys.stdout.flush()

                elapsed_time = divmod(time() - start_time, 60)

                print("Binary created at: {}\nElapsed time: {}".format(os.path.abspath(output_path),
                                                                       "{:02d}:{:02d} mins".format(int(elapsed_time[0]),
                                                                                                   int(elapsed_time[
                                                                                                           1]))))
                print("STATS: {} | {}".format(size(os.path.getsize(output_path)),
                                              datetime.fromtimestamp(os.path.getmtime(output_path))
                                              .strftime("%Y-%m-%d %I:%M %p")))
            else:
                print("Skipping build for {}".format(os.path.abspath(output_path)))

            if upload:
                release_id = "25710929"
                base_release_url = "https://api.github.com/repos/KonduitAI/konduit-serving/releases"
                release_response = requests.get("{}/{}/assets".format(base_release_url, release_id))
                if not release_response.status_code == 200:
                    print("Fetching release data failed:\nPath: {}\nStatus Code: {}\nDetails: {}"
                          .format(release_response.url,
                                  release_response.status_code,
                                  release_response.text))
                else:
                    assets = release_response.json()
                    old_asset = [asset for asset in assets if asset["name"] == artifact_name]
                    if len(old_asset) > 0:
                        old_asset_id = old_asset[0]["id"]
                    else:
                        old_asset_id = -1

                    do_upload = old_asset_id == -1

                    if old_asset_id != -1:
                        '''
                        Old assets takes time to refesh before they are deleted. So if there's a 404 we'll recheck the
                        asset id
                        '''
                        while True:
                            delete_response = requests.delete("{}/assets/{}".format(base_release_url, old_asset_id),
                                                              auth=auth)
                            if not delete_response.status_code == 204:
                                if not delete_response.status_code == 404:
                                    print("Deleting old artifacts failed:\nPath: {}\nStatus Code: {}\nDetails: {}"
                                          .format(delete_response.url,
                                                  delete_response.status_code,
                                                  delete_response.text))
                                    do_upload = False
                                    break
                                else:
                                    release_response = requests.get("{}/{}/assets".format(base_release_url, release_id))
                                    if not release_response.status_code == 200:
                                        print("Fetching release data failed:\nPath: {}\nStatus Code: {}\nDetails: {}"
                                              .format(release_response.url,
                                                      release_response.status_code,
                                                      release_response.text))
                                        break
                                    else:
                                        assets = release_response.json()
                                        old_asset = [asset for asset in assets if asset["name"] == artifact_name]
                                        if len(old_asset) > 0:
                                            old_asset_id = old_asset[0]["id"]
                                        else:
                                            old_asset_id = -1
                                            do_upload = True
                                            break
                            else:
                                do_upload = True
                                break
                            sleep(1)

                    if do_upload:
                        with open(output_path, 'rb') as upload_file:
                            body = BufferReader(upload_file.read(), progress)

                            upload_response = requests.post("{}/{}/assets?name={}"
                                                            .format(base_release_url,
                                                                    release_id,
                                                                    artifact_name).replace("api.", "uploads."),
                                                            data=body,
                                                            headers={
                                                                'Content-Type': 'application/octet-stream',
                                                                'Content-Length': str(os.path.getsize(output_path))
                                                            },
                                                            auth=auth)

                            if upload_response.status_code == 201:
                                print("Uploaded {filename} at URL: " +
                                      "https://github.com/KonduitAI/konduit-serving/releases"
                                      "/download/cli_base/{filename}"
                                      .format(filename=artifact_name))
                            else:
                                print("Upload failed:\nPath: {}\nStatus Code: {}\nDetails: {}"
                                      .format(upload_response.url,
                                              upload_response.status_code,
                                              upload_response.text))

    print("Done!")
