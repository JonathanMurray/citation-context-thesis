#!/usr/bin/env bash

cd bin

export CLASSPATH=".:/opt/commons-lang3-3.3.2:/opt/weka-3-7-12/*:/opt/jsoup/*:/opt/java-trove/3.1a1/lib/*:/opt/commons-lang3-3.3.2/*" 
export EXJOBB_HOME="/home/jonathan/Documents/eclipse-workspace/exjobb"
export RESOURCES_DIR="/home/jonathan/Documents/eclipse-workspace/exjobb/resources"
export EXJOBB_IN_TERMINAL="1"

java -Xmx4g main.$1
