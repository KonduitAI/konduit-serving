#!/usr/bin/env bash
ls
cd target
ls
mkdir -p extracted
cd extracted
ls
jar xf ../konduit-serving-meta-0.1.0-SNAPSHOT.jar
mv META-INF/konduit-serving/ai.konduit.serving.annotation.runner.CanRun META-INF/konduit-serving/PipelineStepRunner
mv META-INF/konduit-serving/ai.konduit.serving.annotation.json.JsonName META-INF/konduit-serving/JsonNameMapping
mv META-INF/konduit-serving/ai.konduit.serving.annotation.module.RequiresDependencies META-INF/konduit-serving/ModuleRequiresDependencies
jar cf ../konduit-serving-meta-0.1.0-SNAPSHOT.jar META-INF/konduit-serving/PipelineStepRunner META-INF/konduit-serving/JsonNameMapping META-INF/konduit-serving/ModuleRequiresDependencies