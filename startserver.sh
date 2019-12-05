#!/bin/bash
$JAVA_HOME/bin/java \
-DZOOKEEPER_EMBEDDED=false \
-DZOOKEEPER_HOST=localhost \
-DSPARK_HOME=/opt/spark \
-DKONDUIT_SERVING_HOME=/opt/konduit-serving \
-DKONDUIT_SERVING_LICENSE_PATH=/etc/konduit-serving/license.txt \
-DKONDUIT_SERVING_PUBLIC_KEY_PATH=/etc/konduit-serving/publickey.txt \
-DKONDUIT_SERVING_CLASS_PATH=/etc/konduit-serving/logging/*:/opt/konduit-serving/lib/*:/opt/konduit-serving/native/*:/opt/konduit-serving/jackson-2.5.1/*:/etc/konduit-serving/* \
-DKONDUIT_SERVING_SERVER_PROD_MODE=true -DKONDUIT_SERVING_LOG_DIR=/var/log/konduit-serving -Dservice.id=d4b87b2a-e4d4-401d-9fb3-62074bd16dd4 \
-Dservice.type=dataVecTransform -Dlogback.configurationFile=/etc/konduit-serving/logback.xml \
-cp /etc/konduit-serving/logging/*:/opt/konduit-serving/lib/*:/opt/konduit-serving/native/*:/opt/konduit-serving/jackson-2.5.1/*:/etc/konduit-serving/*:/opt/konduit-serving/native/*:/opt/konduit-serving/spark/*:/opt/konduit-serving/plugins/*:/opt/konduit-serving/jersey-2.0.1/*:/opt/konduit-serving/jackson-2.5.1/* io.skymind.spark.transform.SparkTransformServerChooser \
--jsonPath /var/konduit-serving/models/transform/deployment-1/model-8/ver-0.transform \
--dataVecPort 9899 \
--jsonInputType CSV \
--agentId 1f81ae45-1844-4058-a48a-d263f0f8da54222
