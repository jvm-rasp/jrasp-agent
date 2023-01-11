#!/bin/bash

echo "build jrasp-attach(GoLang) project start:" $(date +"%Y-%m-%d %H:%M:%S")

cd $(dirname $0) || exit 1
cd ../
projectpath=`pwd`
echo "current dir:${projectpath}"

cd ${projectpath} || exit 1

go build -v -ldflags "$flags" -o ${projectpath}/bin/attach  || exit 1

echo "build jrasp-attach(GoLang) project end:" $(date +"%Y-%m-%d %H:%M:%S")

exit 0
