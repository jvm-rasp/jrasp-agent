package defs

import "os"

const JRASP_DAEMON_VERSION = "1.1.5"

const SUPPORT_URL = "https://www.jrasp.com"

const DATE_FORMAT = "2006-01-02 15:04:05.000"

// jvm-rasp logo
// http://patorjk.com/software/taag/#p=testall&h=0&v=0&f=Avatar&t=JRASP
const LOGO = "       _   _____                _____   _____  \n" +
	"      | | |  __ \\      /\\      / ____| |  __ \\ \n" +
	"      | | | |__) |    /  \\    | (___   | |__) |\n" +
	"  _   | | |  _  /    / /\\ \\    \\___ \\  |  ___/ \n" +
	" | |__| | | | \\ \\   / ____ \\   ____) | | |   \n" +
	"  \\____/  |_|  \\_\\ /_/    \\_\\ |_____/  |_|\n" +
	":: JVM RASP ::        (v" + JRASP_DAEMON_VERSION + ".RELEASE) " + SUPPORT_URL + "\n"

const START_LOG_ID = 1000

const (
	START_UP                 int = START_LOG_ID + 0
	LOG_VALUE                int = START_LOG_ID + 1
	ENV_VALUE                int = START_LOG_ID + 2
	CONFIG_VALUE             int = START_LOG_ID + 3
	DEBUG_PPROF              int = START_LOG_ID + 4
	HTTP_TOKEN               int = START_LOG_ID + 5
	ATTACH_DEFAULT           int = START_LOG_ID + 6
	ATTACH_READ_TOKEN        int = START_LOG_ID + 7
	LOAD_JAR                 int = START_LOG_ID + 8
	UTILS_OpenFiles          int = START_LOG_ID + 9
	WATCH_DEFAULT            int = START_LOG_ID + 10
	HEART_BEAT               int = START_LOG_ID + 11 // 心跳
	NACOS_INIT               int = START_LOG_ID + 12
	NACOS_LISTEN_CONFIG      int = START_LOG_ID + 13
	DOWNLOAD                 int = START_LOG_ID + 14
	UPLOAD                   int = START_LOG_ID + 15
	DEPENDENCY_INFO          int = START_LOG_ID + 16
	AGENT_SUCCESS_EXIT       int = START_LOG_ID + 17 // agent 卸载成功
	JAVA_PROCESS_STARTUP     int = START_LOG_ID + 18 // 发现java启动
	JAVA_PROCESS_SHUTDOWN    int = START_LOG_ID + 19 // 发现java退出
	AGENT_SUCCESS_INIT       int = START_LOG_ID + 20 // agent 加载成功(attach成功)
	UPDATE_MODULE_PARAMETERS int = START_LOG_ID + 21 // 更新参数
	DEFAULT_ERROR            int = START_LOG_ID + 22 // 常规错误日志
	DEFAULT_INFO             int = START_LOG_ID + 23 // 常规INFO日志
	NACOS_INFO               int = START_LOG_ID + 24 // nacos 服务状态
	AGENT_CONFIG_UPDATE      int = START_LOG_ID + 25 // agent配置更新

	CREATE_PID_FILE int = START_LOG_ID + 26 // 创建pid文件
	LOCK_PID_FILE   int = START_LOG_ID + 27 // 锁定pid文件
	CLEAN_PID_FILE  int = START_LOG_ID + 28 // 清空pid文件
	WRITE_PID_FILE  int = START_LOG_ID + 29 // 写pid文件

	CONFIG_ID int = START_LOG_ID + 30 // 配置文件id

	WATCH_DOCKER         int = START_LOG_ID + 31 // 容器监视id
	MDNS_SEARCH          int = START_LOG_ID + 32 // mdns搜索服务
	RESOURCE_NAME_UPDATE int = START_LOG_ID + 33 // 资源名称更新

	DEFAULT_METIRC    int = START_LOG_ID + 40
	FILE_OPENS_METIRC int = START_LOG_ID + 41
)

const DAEMON_PID_FILE = "pid"

const EXIT_CODE_2 = 2

const FILE_MODE_ONLY_ROOT = 0600

var Sig = make(chan os.Signal, 1)
