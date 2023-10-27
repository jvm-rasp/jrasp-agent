package java_process

import (
	"fmt"
	"jrasp-daemon/defs"
	"jrasp-daemon/environ"
	"jrasp-daemon/socket"
	"jrasp-daemon/userconfig"
	"jrasp-daemon/zlog"
	"net/http"
	"os"
	"path/filepath"
	"strconv"
	"strings"
	"time"

	"github.com/shirou/gopsutil/v3/process"
)

type JavaProcess struct {
	JavaPid    int32                `json:"javaPid"`   // 进程信息
	StartTime  string               `json:"startTime"` // 启动时间
	CmdLines   []string             `json:"cmdLines"`  // 命令行信息
	AppNames   []string             `json:"appNames"`  // tomcat应用名
	AgentMode  userconfig.AgentMode `json:"agentMode"` // agent 运行模式
	ServerIp   string               `json:"serverIp"`  // 内置jetty开启的IP:端口
	ServerPort string               `json:"serverPort"`

	env     *environ.Environ   // 环境变量
	Cfg     *userconfig.Config `json:"-"` // 配置
	process *process.Process   // process 对象

	httpClient *http.Client

	InjectedStatus InjectType `json:"injectedStatus"`

	needUpdateParameters bool // 是否需要更新参数

	needUpdateModules bool // 是否需要刷新模块

	// 模块配置信息
	moduleConfigs []userconfig.ModuleConfig

	// agent 配置参数
	agentConfigs map[string]interface{}

	// JVM环境和系统参数
	PropertiesMap map[string]string `json:"-"`

	ProcessId string `json:"processId"` // 唯一id

	IsContainer bool `json:"isContainer"` // 是否在容器中

	InContainerPid int32 `json:"inContainerPid"` // 容器内的Pid

	RaspVersion string `json:"raspVersion"` // 注入的rasp版本

	Conn socket.AgentConn `json:"-"`
}

func NewJavaProcess(p *process.Process, cfg *userconfig.Config, env *environ.Environ) *JavaProcess {
	javaProcess := &JavaProcess{
		JavaPid:              p.Pid,
		process:              p,
		env:                  env,
		Cfg:                  cfg,
		AgentMode:            cfg.AgentMode,
		moduleConfigs:        cfg.ModuleConfigs,
		agentConfigs:         cfg.AgentConfigs,
		needUpdateParameters: true,
		needUpdateModules:    true,
	}
	return javaProcess
}

func (jp *JavaProcess) WaiteConn() bool {
	// 60s内完成重连
	for i := 0; i < 24; i++ {
		v, ok := socket.SocketConns.Load(jp.ProcessId)
		if ok {
			jp.Conn = v.(socket.AgentConn)
			zlog.Infof(defs.AGENT_CONN_REGISTER, "conn match java process",
				"java process: %d, processId: %s, conn: %s",
				jp.JavaPid, jp.ProcessId, jp.Conn.GetConn().RemoteAddr().String())
			return true
		}
		time.Sleep(time.Second * time.Duration(5))
	}
	return false
}

// 屏蔽指定特征的Java进程
func (jp *JavaProcess) FilterCmdLine(javaCmdLineWhiteList []string) bool {
	cmdLines, err := jp.process.CmdlineSlice()
	if err != nil {
		zlog.Warnf(defs.WATCH_DEFAULT, "get process cmdLines error", `{"pid":%d,"err":%v}`, jp.JavaPid, err)
		return true
	}
	jp.CmdLines = cmdLines

	for _, v := range jp.CmdLines {
		var items = javaCmdLineWhiteList
		for _, item := range items {
			if strings.Contains(v, item) {
				zlog.Infof(defs.DEFAULT_INFO, "java process will ignore", "Java Pid: %d, cmdLineWhite: %s", jp.JavaPid, item)
				return true
			}
		}
	}
	return false
}

func (jp *JavaProcess) CheckContainer() {

	if !isProcessInContainer(jp.JavaPid) {
		return
	}
	jp.IsContainer = true

	innerPid, err := jp.GetInContainerPidByHostPid()
	if err != nil {
		zlog.Warnf(defs.DEFAULT_INFO, "get Java process innner pid failed, process will ignore", "Java Pid: %d, err:%v", jp.JavaPid, err)
		return
	}

	if innerPid == "" {
		zlog.Warnf(defs.DEFAULT_INFO, "get Java process innner pid failed, process will ignore", "Java Pid: %d", jp.JavaPid)
		return
	}

	innerPidInt, err := strconv.ParseInt(innerPid, 10, 32)
	if err != nil || innerPidInt <= 0 {
		zlog.Warnf(defs.DEFAULT_INFO, "convert innerPid to int32 failed, process will ignore", "Java Pid: %d, err:%v", jp.JavaPid, err)
		return
	}
	jp.InContainerPid = int32(innerPidInt)
	return
}

func isProcessInContainer(pid int32) bool {
	procPath := fmt.Sprintf("/proc/%d", pid)
	cgroupPath := filepath.Join(procPath, "cgroup")
	cgroupData, err := os.ReadFile(cgroupPath)
	if err != nil {
		return false
	}

	if strings.Contains(string(cgroupData), "/docker/") ||
		strings.Contains(string(cgroupData), "/kubepods/") {
		return true
	}

	return false
}
