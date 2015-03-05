#!/usr/bin/env bash

cd bin

export CLASSPATH=".:/opt/commons-lang3-3.3.2:/opt/weka-3-7-12/*:/opt/jsoup/*" 
export EXJOBB_HOME="/home/jonathan/Documents/eclipse-workspace/exjobb"
export RESOURCES_DIR="/home/jonathan/Documents/eclipse-workspace/exjobb/resources"


java -Xmx4g main.$1
