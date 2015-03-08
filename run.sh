#!/usr/bin/env bash

cd bin

export CLASSPATH=".:/opt/*:/opt/commons-lang3-3.3.2:/opt/weka-3-7-12/*:/opt/jsoup/*:/opt/java-trove/3.1a1/lib/*:/opt/commons-lang3-3.3.2/*:/opt/jwi_2.3.3/*:/home/jonathan/Documents/exjobb/stanford-corenlp-full-2015-01-30/*:/opt/neo4j-community-2.1.6/lib/*" 
echo "classpath: $CLASSPATH"
export EXJOBB_HOME="/home/jonathan/Documents/eclipse-workspace/exjobb"
export RESOURCES_DIR="/home/jonathan/Documents/eclipse-workspace/exjobb/resources"
export EXJOBB_IN_TERMINAL="1"

if [ -z "$1" ]; 
  then java -Xmx4g main.CompareClassifiers; 
  else java -Xmx4g main.$1;
fi

