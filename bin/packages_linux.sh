#!/bin/bash

# jrasp's target dir
JRASP_TARGET_DIR=../target/jrasp

# jrasp's version
JRASP_VERSION=$(cat ./VERSION.txt)

# exit shell with err_code
# $1 : err_code
# $2 : err_msg
exit_on_err()
{
    [[ ! -z "${2}" ]] && echo "${2}" 1>&2
    exit ${1}
}

# jrasp-agent
mvn clean package -Dmaven.test.skip=false -f ../pom.xml \
    || exit_on_err 1 "package jrasp-agent failed."

# jrasp-inject
cd ../jrasp-attach/build && ./build.sh || exit_on_err 1 "go build jrasp-attach failed." ;
cd - || exit_on_err 1 "cd jrasp-attach dir failed.";

# jrasp-daemon
cd ../jrasp-daemon/build && ./build.sh || exit_on_err 1 "go build jrasp-daemon failed." ;
cd - || exit_on_err 1 "cd jrasp-daemon dir failed.";

# reset the target dir
mkdir -p ${JRASP_TARGET_DIR}/bin
mkdir -p ${JRASP_TARGET_DIR}/lib
mkdir -p ${JRASP_TARGET_DIR}/module
mkdir -p ${JRASP_TARGET_DIR}/config
mkdir -p ${JRASP_TARGET_DIR}/logs
mkdir -p ${JRASP_TARGET_DIR}/run
mkdir -p ${JRASP_TARGET_DIR}/tmp

# copy jar to TARGET_DIR
cp ../jrasp-core/target/jrasp-core-*.jar ${JRASP_TARGET_DIR}/lib/ \
    && cp ../jrasp-launcher/target/jrasp-launcher-*.jar ${JRASP_TARGET_DIR}/lib/ \
    && cp ../jrasp-bridge/target/jrasp-bridge-*.jar ${JRASP_TARGET_DIR}/lib/

# for jrasp-daemon
cp ../jrasp-daemon/bin/jrasp-daemon ${JRASP_TARGET_DIR}/bin/jrasp-daemon

# for sh
cp ../jrasp-daemon/bin/*.sh ${JRASP_TARGET_DIR}/bin/

# for config
cp ../jrasp-daemon/config/config.json ${JRASP_TARGET_DIR}/config/

# for jrasp-attach
cp ../jrasp-attach/bin/attach ${JRASP_TARGET_DIR}/bin/attach

# for module
cp ../jrasp-module/**/target/*.jar ${JRASP_TARGET_DIR}/module/  || exit_on_err 1 "copy jrasp module jar failed."

# for tools
cp ../tools/jattach_linux ${JRASP_TARGET_DIR}/bin/jattach_linux  || exit_on_err 1 "copy jrasp tools failed."

# for VERSION.txt
cp ./VERSION.txt ${JRASP_TARGET_DIR}/VERSION.txt

# make it execute able
chmod +x ${JRASP_TARGET_DIR}/bin/*

# tar the jrasp.tar.gz
cd ../target/
tar -zcvf jrasp-${JRASP_VERSION}-bin-linux.tar.gz jrasp/
cd -

echo "package jrasp-${JRASP_VERSION}-bin-linux.tar.gz finish."
