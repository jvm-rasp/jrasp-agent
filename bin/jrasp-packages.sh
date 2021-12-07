#!/bin/bash

# jrasp's target dir
JRASP_TARGET_DIR=../target/jrasp-agent


# exit shell with err_code
# $1 : err_code
# $2 : err_msg
exit_on_err()
{
    [[ ! -z "${2}" ]] && echo "${2}" 1>&2
    exit ${1}
}

# maven package the jrasp
mvn clean package -Dmaven.test.skip=false -f ../pom.xml \
    || exit_on_err 1 "package jrasp failed."

# reset the target dir
mkdir -p ${JRASP_TARGET_DIR}/bin
mkdir -p ${JRASP_TARGET_DIR}/lib
mkdir -p ${JRASP_TARGET_DIR}/system-module    # 系统模块路径(仓库，全部复制到run/pid)
mkdir -p ${JRASP_TARGET_DIR}/cfg
mkdir -p ${JRASP_TARGET_DIR}/provider
mkdir -p ${JRASP_TARGET_DIR}/required-module  # 用户必装模块路径(仓库，全部复制到run/pid)
mkdir -p ${JRASP_TARGET_DIR}/optional-module  # 用户可选的模块路径(仓库，部分复制到run/pid)
mkdir -p ${JRASP_TARGET_DIR}/run              # java进程相关数据：token、module
mkdir -p ${JRASP_TARGET_DIR}/logs             # jrasp日志：系统日志、检测日志、心跳日志等

# jrasp's version
JRASP_VERSION=$(cat ../jrasp-core/target/classes/version)
echo "${JRASP_VERSION}" > ${JRASP_TARGET_DIR}/cfg/version

# copy jar、config、script to TARGET_DIR
cp ../jrasp-core/target/jrasp-core-${JRASP_VERSION}.jar ${JRASP_TARGET_DIR}/lib/jrasp-core.jar \
 && cp ../jrasp-launcher/target/jrasp-launcher-*-jar-with-dependencies.jar ${JRASP_TARGET_DIR}/lib/jrasp-launcher.jar \
 && cp ../jrasp-spy/target/jrasp-spy-*-jar-with-dependencies.jar ${JRASP_TARGET_DIR}/lib/jrasp-spy.jar \
 && cp jrasp-logback.xml ${JRASP_TARGET_DIR}/cfg/jrasp-logback.xml \
 && cp jrasp.sh ${JRASP_TARGET_DIR}/bin/jrasp.sh

# cppy system-jar
cp ../jrasp-module-admin/target/jrasp-module-admin-*-jar-with-dependencies.jar ${JRASP_TARGET_DIR}/system-module/jrasp-module-admin.jar \
    && cp ../jrasp-system-provider/target/jrasp-system-provider-*-jar-with-dependencies.jar ${JRASP_TARGET_DIR}/provider/jrasp-system-provider.jar

# make it execute able
chmod +x ${JRASP_TARGET_DIR}/bin/*.sh


# zip the jrasp-agent.zip
cd ../target/
zip -r jrasp-agent-${JRASP_VERSION}-bin.zip jrasp-agent/

echo "package jrasp-agent-${JRASP_VERSION}-bin.zip finish."
