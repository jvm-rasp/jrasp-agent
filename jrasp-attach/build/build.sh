#!/bin/bash

echo "build jrasp-attach(GoLang) project start:" $(date +"%Y-%m-%d %H:%M:%S")

cd $(dirname $0) || exit 1
cd ../
projectpath=`pwd`
echo "current dir:${projectpath}"

KEY_VERSION=$(cat ../bin/DECRYPT_KEY.txt)
buildDecryptKey=${KEY_VERSION}

# 设置阿里云代理
export GOPROXY="https://mirrors.aliyun.com/goproxy/"
echo "GOPROXY:"${GOPROXY}

go mod tidy

moduleName=$(go list -m)

pkgName="common"

flags="-X '${moduleName}/${pkgName}.BuildDecryptKey=${buildDecryptKey}' "

cd ${projectpath} || exit 1

go build -v -ldflags "$flags" -o ${projectpath}/bin/attach  || exit 1

echo "build jrasp-attach(GoLang) project end:" $(date +"%Y-%m-%d %H:%M:%S")

exit 0
