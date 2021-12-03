#!/usr/bin/env bash

typeset RASP_SUPPORT_URL="https://applink.feishu.cn/client/chat/chatter/add_by_link?link_token=e35l9ee2-a000-45ba-8863-75b5ca2cdbe6"

typeset RASP_HOME_DIR
[[ -z ${RASP_HOME_DIR} ]] && RASP_HOME_DIR=${PWD}/..

# define current RASP_USER
typeset RASP_USER=${USER}
[[ -z ${RASP_USER} ]] && RASP_USER=$(whoami)

# define rasp's network
typeset RASP_SERVER_NETWORK

# define rasp's lib
typeset RASP_LIB_DIR=${RASP_HOME_DIR}/lib

# define JVM OPS
typeset RASP_JVM_OPS="-Xms64M -Xmx64M -Xnoclassgc -ea"

# define target JVM Process ID
typeset TARGET_JVM_PID

# define rasp attach token file
typeset RASP_TOKEN_FILE

# define target SERVER network interface
typeset TARGET_SERVER_IP
typeset DEFAULT_TARGET_SERVER_IP="0.0.0.0"

# define target SERVER network port
typeset TARGET_SERVER_PORT

# define target NAMESPACE
typeset TARGET_NAMESPACE
typeset DEFAULT_NAMESPACE="jrasp"

# enable http auth
typeset ENABLE_AUTHTH="true"

# default username
typeset DEFAULT_USERNAME="admin"

# default password
typeset DEFAULT_PASSWORD="123456"

# login token
typeset AUTH_TOKEN


# exit shell with err_code
# $1 : err_code
# $2 : err_msg
exit_on_err() {
  [[ -n "${2}" ]] && echo "${2}" 1>&2
  exit "${1}"
}

# display usage
function usage() {
  echo "
usage: ${0} [h] [<p:> [vslFfu:a:A:d:m:I:P:C:X]]

    -h : help
         Prints the ${0} help

    -X : debug
         Prints debug message

    -p : PID
         Select target JVM process ID

    -v : version
         Prints rasp\`s version

    -l : list loaded module
         Prints loaded module list


    -F : force flush
         Force flush the rasp\`s user module library.

         flush reload user module library\`s module jar file.

         if module froze & unload occur error, ignore this error force froze & unload.
         if module reload occur error, ignore this module.


    -f : soft flush
         Soft flush the rasp\`s user module library.

         flush user module library\`s module which module jar file was changed.
         if module jar file was append, load the newest module.
         if module jar file was changed, reload the newest module.
         if module jar file was removed. remove the modules.

         if module froze & unload occur error, ignore this error force froze & unload.
         if module reload occur error, ignore this module.

    -u : unload
         Unload compliance module, support pattern expression.

         EXAMPLE:
             ${0} -p <PID> -u *debug*


    -a : active module
         Active compliance module, support pattern expression.
         module will receive event when state was activated.

         EXAMPLE:
             ${0} -p <PID> -a *debug*


    -A : frozen
         Frozen compliance module, support pattern expression.
         when module state change on frozen, it will not receive event anymore.

         EXAMPLE:
             ${0} -p <PID> -A *debug*


    -m : module detail
         Print module detail

         EXAMPLE:
             ${0} -p <PID> -m debug


    -I : IP address
         Appoint the network interface (bind ip address)
         when default, use \"${DEFAULT_TARGET_SERVER_IP}\"

         EXAMPLE:
            ${0} -p <PID> -I 192.168.0.1 -v


    -P : port
         Appoint the rasp\` network port
         when default, use random port

         EXAMPLE:
            ${0} -p <PID> -P 3658 -v


    -C : connect server only
         No attach target JVM, just connect server with appoint IP:PORT only.

         EXAMPLE:
             ${0} -C -I 192.168.0.1 -P 3658 -m debug

    -s : shutdown jrasp-agent
         Shutdown jrasp-agent && http-server

        EXAMPLE:
              ${0} -p <PID> -s

    -d : data
         Send the command & data to module's command handle method.
         <MODULE-ID>/<COMMAND-NAME>[?<PARAM1=VALUE1>[&PARAM2=VALUE2]]

         DATA:
            ${0} -p <PID> -d 'info/version'
            ${0} -p <PID> -d 'module/detail?id=rasp-info'
         BLOCK:
            ${0} -p <PID> -d 'rce/block?isBlock=true'
            ${0} -p <PID> -d 'file/block?isBlock=true'

"
}

# check rasp permission
check_permission() {

  # check PID existed
  pgrep java | grep "${TARGET_JVM_PID}" > /dev/null ||
    exit_on_err 1 "permission denied, java process ${TARGET_JVM_PID} is not existed."

  # check attach
  pgrep -U "${RASP_USER}" | grep "${TARGET_JVM_PID}" > /dev/null ||
    exit_on_err 1 "permission denied, ${RASP_USER} is not allow attach to ${TARGET_JVM_PID}."

  # check $HOME is writeable
  [[ ! -w ${HOME} ]] &&
    exit_on_err 1 "permission denied, ${HOME} is not writable."

  # check RASP-LIB is readable
  [[ ! -r ${RASP_LIB_DIR} ]] &&
    exit_on_err 1 "permission denied, ${RASP_LIB_DIR} is not readable."

  # check JAVA_HOME is accessible
  [[ ! -x "${RASP_JAVA_HOME}" ]] &&
    exit_on_err 1 "permission denied, ${RASP_JAVA_HOME} is not accessible! please set JAVA_HOME"

  # check java command is executeable
  [[ ! -x "${RASP_JAVA_HOME}/bin/java" ]] &&
    exit_on_err 1 "permission denied, ${RASP_JAVA_HOME}/bin/java is not executable!"

  # check the jvm version, we need 6+
  "${RASP_JAVA_HOME}"/bin/java -version 2>&1 | awk -F '"' '/version/&&$2<="1.5"{exit 1}' ||
    exit_on_err 1 "permission denied, please make sure target java process: ${TARGET_JVM_PID} run in JDK[6,11]"

}

# reset rasp work environment
# reset some options for env
reset_for_env() {

  # use the env JAVA_HOME for default
  [[ -n "${JAVA_HOME}" ]] &&
    RASP_JAVA_HOME="${JAVA_HOME}"

  # use the target JVM for RASP_JAVA_HOME
  [[ -z "${RASP_JAVA_HOME}" ]] &&
    RASP_JAVA_HOME="$(
      lsof -p "${TARGET_JVM_PID}" |
        grep "/bin/java" |
        awk '{print $9}' |
        xargs ls -l |
        awk '{if($1~/^l/){print $11}else{print $9}}' |
        xargs ls -l |
        awk '{if($1~/^l/){print $11}else{print $9}}' |
        sed 's/\/bin\/java//g'
    )"

  # append toos.jar to JVM_OPT
  [[ -f "${RASP_JAVA_HOME}"/lib/tools.jar ]] &&
    RASP_JVM_OPS="${RASP_JVM_OPS} -Xbootclasspath/a:${RASP_JAVA_HOME}/lib/tools.jar"

}

# attach rasp to target JVM
# return : attach jvm local info
function attach_jvm() {
  # attach target jvm
  "${RASP_JAVA_HOME}/bin/java" \
    ${RASP_JVM_OPS} \
    -jar "${RASP_LIB_DIR}/jrasp-core.jar" \
    "${TARGET_JVM_PID}" \
    "${RASP_LIB_DIR}/jrasp-launcher.jar" \
    "raspHome=${RASP_HOME_DIR};serverIp=${TARGET_SERVER_IP};serverPort=${TARGET_SERVER_PORT};namespace=${TARGET_NAMESPACE};enableAuth=${ENABLE_AUTHTH};username=${DEFAULT_USERNAME};password=${DEFAULT_PASSWORD}" ||
    exit_on_err 1 "attach JVM ${TARGET_JVM_PID} fail."

  # get network from attach result
  RASP_SERVER_NETWORK=$(grep "${TARGET_NAMESPACE}" "${RASP_TOKEN_FILE}" | awk -F ";" '{print $4";"$5}')
  [[ -z ${RASP_SERVER_NETWORK} ]] &&
    exit_on_err 1 "attach JVM ${TARGET_JVM_PID} fail, attach lose response."

}

# execute rasp command
# $1 : path
# $2 : params, eg: &name=dukun&age=31
function rasp_curl() {
  rasp_debug_curl "${1}?1=1${2}"
}

# url=$(parse_json $s1 $s2)
function parse_json() {
  echo $1 | sed 's/.*'$2':\([^,}]*\).*/\1/' | sed 's/\"//g'
}

# user login
function echo_token() {
  local host=${RASP_SERVER_NETWORK%;**}
    local port=${RASP_SERVER_NETWORK#**;}
    if [[ "$host" == "0.0.0.0" ]]; then
      host="127.0.0.1"
    fi
  echo $(curl -s -N --location --request POST "http://${host}:${port}/${TARGET_NAMESPACE}/user/login" \
             --header "Content-Type: application/x-www-form-urlencoded" \
             --data "username=${DEFAULT_USERNAME}" --data "password=${DEFAULT_PASSWORD}") | sed 's/.*"data":\([^,}]*\).*/\1/' | sed 's/\"//g'
}

# execute rasp command & exit
function rasp_curl_with_exit() {
  rasp_curl "${@}"
  exit
}

function rasp_debug_curl() {
  #local host=$(echo "${RASP_SERVER_NETWORK}" | awk -F ";" '{print $1}')
  #local port=$(echo "${RASP_SERVER_NETWORK}" | awk -F ";" '{print $2}')
  local host=${RASP_SERVER_NETWORK%;**}
  local port=${RASP_SERVER_NETWORK#**;}
  if [[ "$host" == "0.0.0.0" ]]; then
    host="127.0.0.1"
  fi
  curl -N -s "http://${host}:${port}/${TARGET_NAMESPACE}/${1}" --header "Authentication: $(echo_token)" ||
    exit_on_err 1 "target JVM ${TARGET_JVM_PID} lose response."
}

function echo_jrasp_support_url() {
  echo -e "\n点击链接获得技术支持: ${RASP_SUPPORT_URL}"
}

# the rasp main function
function main() {

  while getopts "hp:vFfRu:a:A:d:m:I:P:Clsn:X" ARG; do
    case ${ARG} in
    h)
      usage
      exit
      ;;
    p) TARGET_JVM_PID=${OPTARG}
      RASP_TOKEN_FILE="${RASP_HOME_DIR}/run/${TARGET_JVM_PID}/.jrasp.token"
      ;;
    v) OP_VERSION=1 ;;
    l) OP_MODULE_LIST=1 ;;
    F) OP_MODULE_FORCE_FLUSH=1 ;;
    f) OP_MODULE_FLUSH=1 ;;
    u)
      OP_MODULE_UNLOAD=1
      ARG_MODULE_UNLOAD=${OPTARG}
      ;;
    a)
      OP_MODULE_ACTIVE=1
      ARG_MODULE_ACTIVE=${OPTARG}
      ;;
    A)
      OP_MODULE_FROZEN=1
      ARG_MODULE_FROZEN=${OPTARG}
      ;;
    d)
      OP_DEBUG=1
      ARG_DEBUG=${OPTARG}
      ;;
    m)
      OP_MODULE_DETAIL=1
      ARG_MODULE_DETAIL=${OPTARG}
      ;;
    I) TARGET_SERVER_IP=${OPTARG} ;;
    P) TARGET_SERVER_PORT=${OPTARG} ;;
    C) OP_CONNECT_ONLY=1 ;;
    s) OP_SHUTDOWN=1 ;;
    n)
      OP_NAMESPACE=1
      ARG_NAMESPACE=${OPTARG}
      ;;
    X) set -x ;;
    ?)
      usage
      exit_on_err 1
      ;;
    esac
  done

  reset_for_env
  check_permission

  # reset IP
  [ -z "${TARGET_SERVER_IP}" ] && TARGET_SERVER_IP="${DEFAULT_TARGET_SERVER_IP}"

  # reset PORT
  [ -z "${TARGET_SERVER_PORT}" ] && TARGET_SERVER_PORT=0

  # reset NAMESPACE
  [[ ${OP_NAMESPACE} ]] &&
    TARGET_NAMESPACE=${ARG_NAMESPACE}
  [[ -z ${TARGET_NAMESPACE} ]] &&
    TARGET_NAMESPACE=${DEFAULT_NAMESPACE}

  if [[ ${OP_CONNECT_ONLY} ]]; then
    [[ 0 -eq ${TARGET_SERVER_PORT} ]] &&
      exit_on_err 1 "server appoint PORT (-P) was missing"
    RASP_SERVER_NETWORK="${TARGET_SERVER_IP};${TARGET_SERVER_PORT}"
  else
    # -p was missing
    [[ -z ${TARGET_JVM_PID} ]] &&
      exit_on_err 1 "PID (-p) was missing."
    attach_jvm
  fi

  # -v show version
  [[ -n ${OP_VERSION} ]] &&
    rasp_curl_with_exit "info/version"

  # -l list loaded modules
  [[ -n ${OP_MODULE_LIST} ]] &&
    rasp_curl_with_exit "module/list"

  # -F force flush module
  [[ -n ${OP_MODULE_FORCE_FLUSH} ]] &&
    rasp_curl_with_exit "module/flush" "&force=true"

  # -f flush module
  [[ -n ${OP_MODULE_FLUSH} ]] &&
    rasp_curl_with_exit "module/flush" "&force=false"

  # -u unload module
  [[ -n ${OP_MODULE_UNLOAD} ]] &&
    rasp_curl_with_exit "module/unload" "&action=unload&ids=${ARG_MODULE_UNLOAD}"

  # -a active module
  [[ -n ${OP_MODULE_ACTIVE} ]] &&
    rasp_curl_with_exit "module/active" "&ids=${ARG_MODULE_ACTIVE}"

  # -A frozen module
  [[ -n ${OP_MODULE_FROZEN} ]] &&
    rasp_curl_with_exit "module/frozen" "&ids=${ARG_MODULE_FROZEN}"

  # -m module detail
  [[ -n ${OP_MODULE_DETAIL} ]] &&
    rasp_curl_with_exit "module/detail" "&id=${ARG_MODULE_DETAIL}"

  # -s shutdown
  [[ -n ${OP_SHUTDOWN} ]] &&
    rasp_curl_with_exit "control/shutdown"

  # -d debug
  if [[ -n ${OP_DEBUG} ]]; then
    rasp_debug_curl "${ARG_DEBUG}"
    exit
  fi

  # default
  rasp_curl "info/version"
  # support utl
  echo_jrasp_support_url

  exit

}

main "${@}"
