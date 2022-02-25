@echo off

REM
REM ******************************************************************************
REM Copyright (c) 2022 Konduit K.K.
REM
REM This program and the accompanying materials are made available under the
REM terms of the Apache License, Version 2.0 which is available at
REM https://www.apache.org/licenses/LICENSE-2.0.
REM
REM Unless required by applicable law or agreed to in writing, software
REM distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
REM WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
REM License for the specific language governing permissions and limitations
REM under the License.
REM
REM SPDX-License-Identifier: Apache-2.0
REM *****************************************************************************
REM

REM Uncomment to use the environment variables

REM #  Environment variable name for storing port number for the konduit server.
REM #  This variable will be prioritizes over the port set through the configuration files.
REM -------------------------------------------------------------------------------------
REM SET KONDUIT_SERVING_PORT=8080
REM -------------------------------------------------------------------------------------

REM #  An environment variable for setting the working directory for konduit serving.
REM #  The working directory contains the runtime files generated by vertx or konduit-serving itself.
REM #  The runtime files could contain logs, running process details, vertx cache files etc.
REM -------------------------------------------------------------------------------------
REM SET KONDUIT_WORKING_DIR=%USERPROFILE%\.konduit-serving
REM -------------------------------------------------------------------------------------

REM #  Environment variable specifying vertx runtime and cache directory.
REM -------------------------------------------------------------------------------------
REM SET KONDUIT_VERTX_DIR=%KONDUIT_WORKING_DIR%\vertx
REM -------------------------------------------------------------------------------------

REM #  Environment variable specifying build data directory where build logs for the build CLI are kept.
REM -------------------------------------------------------------------------------------
REM SET KONDUIT_BUILD_DIR=%KONDUIT_WORKING_DIR%\build
REM -------------------------------------------------------------------------------------

REM #  Environment variable specifying profiles data directory where details of individual profiles are kept.
REM -------------------------------------------------------------------------------------
REM SET KONDUIT_PROFILES_DIR=%KONDUIT_WORKING_DIR%\profiles
REM -------------------------------------------------------------------------------------

REM #  This variable is responsible for setting the path where the log files for a konduit server
REM #  is kept for the `\logs` endpoint.
REM -------------------------------------------------------------------------------------
REM SET KONDUIT_ENDPOINT_LOGS_DIR=%KONDUIT_WORKING_DIR%\endpoint_logs
REM -------------------------------------------------------------------------------------

REM #  Default directory for containing the command line logs for konduit-serving
REM -------------------------------------------------------------------------------------
REM SET KONDUIT_COMMAND_LOGS_DIR=%KONDUIT_WORKING_DIR%\command_logs
REM -------------------------------------------------------------------------------------

REM #  Sets the directory where the file uploads are kept for Vertx BodyHandler
REM -------------------------------------------------------------------------------------
REM SET KONDUIT_FILE_UPLOADS_DIR=%TEMP%
REM -------------------------------------------------------------------------------------