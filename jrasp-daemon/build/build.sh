#!/bin/bash

echo "build go project start:" $(date +"%Y-%m-%d %H:%M:%S")

# 切换到 build.sh 脚本所在的目录
cd $(dirname $0) || exit 1
cd ../
projectpath=`pwd`
echo "current dir:${projectpath}"

# 设置阿里云代理
export GOPROXY="https://mirrors.aliyun.com/goproxy/"
echo "GOPROXY:"${GOPROXY}

# 编译信息
moduleName=$(go list -m)
commit=$(git rev-parse --short HEAD)
branch=$(git rev-parse --abbrev-ref HEAD)
buildTime=$(date +%Y%m%d%H%M)

# environ.go 包名
environPkgName="environ"

flags="-X '${moduleName}/${environPkgName}.BuildGitBranch=${branch}' -X '${moduleName}/${environPkgName}.BuildGitCommit=${commit}'  -X '${moduleName}/${environPkgName}.BuildDateTime=${buildTime}' "

program=$(basename ${moduleName})

cd ${projectpath} || exit 1

go build -v -ldflags "$flags" -o ${projectpath}/bin/$program  || exit 1

echo "build go project end:" $(date +"%Y-%m-%d %H:%M:%S")

exit 0
