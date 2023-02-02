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

# install maven plugin 插件
cd ../jrasp-encrypt
mvn clean install -Dmaven.test.skip=false -f ../pom.xml \
    || exit_on_err 1 "[JRASP ERROR] install encrypt maven plugin failed."
cd -

# jrasp-agent
mvn clean package -Dmaven.test.skip=false -f ../pom.xml \
    || exit_on_err 1 "[JRASP ERROR] package jrasp-agent failed."

# jrasp-attach
cd ../jrasp-attach/build && ./build.sh || exit_on_err 1 "[JRASP ERROR] go build jrasp-attach failed." ;
cd - || exit_on_err 1 "[JRASP ERROR] cd jrasp-attach dir failed.";

# jrasp-daemon
cd ../jrasp-daemon/build && ./build.sh || exit_on_err 1 "[JRASP ERROR] go build jrasp-daemon failed." ;
cd - || exit_on_err 1 "[JRASP ERROR] cd jrasp-daemon dir failed.";

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

# for jrasp-attach
cp ../jrasp-attach/bin/attach ${JRASP_TARGET_DIR}/bin/attach

# for config
cp ../jrasp-daemon/config/config.json ${JRASP_TARGET_DIR}/config/

# for module
cp ../jrasp-module/**/target/*-encrypted.jar ${JRASP_TARGET_DIR}/module/  || exit_on_err 1 "[JRASP ERROR] copy jrasp module jar failed."

# for tools
cp ../tools/jattach_darwin ${JRASP_TARGET_DIR}/bin/jattach_darwin  || exit_on_err 1 "[JRASP ERROR] copy jrasp tools failed."

# for config.json
cp ../jrasp-daemon/config/config.json ${JRASP_TARGET_DIR}/config/config.json

# for config.json
cp ./VERSION.txt ${JRASP_TARGET_DIR}/VERSION.txt

# make it execute able
chmod +x ${JRASP_TARGET_DIR}/bin/*

# tar the jrasp.tar.gz
cd ../target/ || exit_on_err 1 "[JRASP ERROR] cd jrasp dir error."
tar -zcvf jrasp-${JRASP_VERSION}-bin-darwin.tar.gz jrasp/
cd -

echo "package jrasp-${JRASP_VERSION}-bin-darwin.tar.gz finish."
