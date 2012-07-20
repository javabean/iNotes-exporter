#!/bin/sh

ARGS=$@
SLEEP_TIME=60
JAVA_BIN=/usr/local/bin/java
INOTES_JAR=/usr/local/share/iNotes-exporter/target/iNotes-exporter-1.2-jar-with-dependencies.jar

while true ; do $JAVA_BIN -jar $INOTES_JAR $ARGS ; $SLEEP_TIME ;done
