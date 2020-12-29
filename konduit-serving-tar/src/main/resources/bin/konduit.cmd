SET SCRIPT_DIR=%~dp0

CALL %SCRIPT_DIR%\..\conf\konduit-serving-env.cmd

java -jar -Dvertx.cli.usage.prefix=konduit %SCRIPT_DIR%\..\konduit.jar %*
