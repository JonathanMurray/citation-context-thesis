#!/usr/bin/env bash
. config.sh
cd bin
if [ -z "$1" ]; 
  then java -Xmx4g main.CompareClassifiers; 
  else java -Xmx4g $1;
fi

