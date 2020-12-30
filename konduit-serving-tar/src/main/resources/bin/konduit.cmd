SET SCRIPT_DIR=%~dp0

CALL %SCRIPT_DIR%\..\conf\konduit-serving-env.cmd

java -jar -Dvertx.cli.usage.prefix=konduit -Dlogback.configurationFile=%SCRIPT_DIR%\..\conf\logback.xml -Dlogback.configurationFile.runCommand=%SCRIPT_DIR%\..\conf\logback-run_command.xml %SCRIPT_DIR%\..\konduit.jar %*
