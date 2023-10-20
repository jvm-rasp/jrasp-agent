package java_process

import (
	"jrasp-daemon/defs"
	"jrasp-daemon/environ"
	"jrasp-daemon/userconfig"
	"jrasp-daemon/zlog"
	"net/http"
	"time"

	"github.com/shirou/gopsutil/v3/process"
)

// 注入状态
type InjectType string

const (
	NOT_INJECT InjectType = "not inject" // 未注入

	SUCCESS_INJECT InjectType = "success inject" // 注入正常
	FAILED_INJECT  InjectType = "failed inject"  // 注入时失败

	SUCCESS_EXIT InjectType = "success uninstall agent" // agent卸载成功
	FAILED_EXIT  InjectType = "failed uninstall agent"  // agent卸载失败

	FAILED_DEGRADE  InjectType = "failed degrade"  // 降级失败时后失败
	SUCCESS_DEGRADE InjectType = "success degrade" // 降级正常
)

const (
	libDir    string = "lib"
	moduleDir string = "module"
)

type ModuleSendConfig struct {
	ModuleName string `json:"moduleName"`
	Parameters string `json:"parameters"`
}

type JavaProcess struct {
	JavaPid    int32                `json:"javaPid"`   // 进程信息
	StartTime  string               `json:"startTime"` // 启动时间
	CmdLines   []string             `json:"cmdLines"`  // 命令行信息
	AppNames   []string             `json:"appNames"`  // tomcat应用名
	AgentMode  userconfig.AgentMode `json:"agentMode"` // agent 运行模式
	ServerIp   string               `json:"serverIp"`  // 内置jetty开启的IP:端口
	ServerPort string               `json:"serverPort"`

	env     *environ.Environ   // 环境变量
	cfg     *userconfig.Config // 配置
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

	Uuid string `json:"uuid"` //

	IsContainer bool `json:"isContainer"` // 是否在容器中

	InContainerPid int32 `json:"inContainerPid"` // 容器内的Pid

	RaspVersion string `json:"raspVersion"` // 注入的rasp版本
}

func NewJavaProcess(p *process.Process, cfg *userconfig.Config, env *environ.Environ) *JavaProcess {
	javaProcess := &JavaProcess{
		JavaPid:              p.Pid,
		process:              p,
		env:                  env,
		cfg:                  cfg,
		AgentMode:            cfg.AgentMode,
		moduleConfigs:        cfg.ModuleConfigs,
		agentConfigs:         cfg.AgentConfigs,
		needUpdateParameters: true,
		needUpdateModules:    true,
	}
	return javaProcess
}

// UpdateParameters 更新模块参数
func (jp *JavaProcess) UpdateParameters() bool {
	return true
}

func (jp *JavaProcess) IsInject() bool {
	return jp.InjectedStatus == SUCCESS_INJECT || jp.InjectedStatus == FAILED_INJECT
}

func (jp *JavaProcess) SuccessInject() bool {
	return jp.InjectedStatus == SUCCESS_INJECT
}

func (jp *JavaProcess) MarkExitInject() {
	jp.InjectedStatus = SUCCESS_EXIT
}

func (jp *JavaProcess) MarkFailedExitInject() {
	jp.InjectedStatus = FAILED_EXIT
}

func (jp *JavaProcess) MarkSuccessInjected() {
	jp.InjectedStatus = SUCCESS_INJECT
}

func (jp *JavaProcess) MarkFailedInjected() {
	jp.InjectedStatus = FAILED_INJECT
}

func (jp *JavaProcess) MarkNotInjected() {
	jp.InjectedStatus = NOT_INJECT
}

func (jp *JavaProcess) SetCmdLines() {
	cmdLines, err := jp.process.CmdlineSlice()
	if err != nil {
		zlog.Warnf(defs.WATCH_DEFAULT, "get process cmdLines error", `{"pid":%d,"err":%v}`, jp.JavaPid, err)
	}
	jp.CmdLines = cmdLines
}

func (jp *JavaProcess) SetStartTime() int64 {
	startTime, err := jp.process.CreateTime()
	if err != nil {
		zlog.Warnf(defs.WATCH_DEFAULT, "get process startup time error", `{"pid":%d,"err":%v}`, jp.JavaPid, err)
	}
	timeUnix := time.Unix(startTime/1000, 0)
	timsStr := timeUnix.Format(defs.DATE_FORMAT)
	jp.StartTime = timsStr
	return startTime
}
