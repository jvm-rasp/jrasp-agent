package userconfig

import (
	"fmt"
	"jrasp-daemon/defs"
	"time"

	"github.com/spf13/viper"
)

// AgentMode 运行模式
type AgentMode string

const (
	VERSION string    = "1.1.1"
	STATIC  AgentMode = "static"  // static模式：  被动注入
	DYNAMIC AgentMode = "dynamic" // dynamic模式： 主动注入
	DISABLE AgentMode = "disable" // disbale模式: (主动/被动)注入的退出、禁止注入
)

type Config struct {
	Version string `json:"version"` // agent 版本
	// java agent 运行模式
	AgentMode AgentMode `json:"agentMode"` // 需要显示配置

	// 配置id
	ConfigId int `json:"configId"`

	// 激活激活时间如: 15:10
	ActiveTime string `json:"activeTime"`

	Namespace string `json:"namespace"`

	// 日志配置
	LogLevel int    `json:"logLevel"`
	LogPath  string `json:"logPath"`

	// 性能诊断配置
	EnablePprof bool `json:"enablePprof"`
	PprofPort   int  `json:"pprofPort"`

	// 进程扫描定时器配置
	LogReportTicker       uint32 `json:"logReportTicker"`
	ScanTicker            uint32 `json:"scanTicker"`
	RebootTicker          uint32 `json:"rebootTicker"`
	PidExistsTicker       uint32 `json:"pidExistsTicker"`
	ProcessInjectTicker   uint32 `json:"processInjectTicker"`
	HeartBeatReportTicker uint   `json:"heartBeatReportTicker"`
	DependencyTicker      uint32 `json:"dependencyTicker"`

	// nacos 配置
	NamespaceId string   `json:"namespaceId"` // 命名空间
	DataId      string   `json:"dataId"`      // 配置id
	NacosIps    []string `json:"nacosIps"`    // nacos 服务端ip列表

	// jrasp-daemon 自身配置
	BinFileUrl  string `json:"binFileUrl"`  // 下载链接
	BinFileHash string `json:"binFileHash"` // hash

	// module列表
	ModuleConfigs []ModuleConfig `json:"moduleConfigs"` // 模块配置

	// 模块更新
	ModuleAutoUpdate bool `json:"moduleAutoUpdate"` // 本地磁盘没有缓存，模块对象存储服务上下载

	// agent参数更新
	AgentConfigs map[string]interface{} `json:"agentConfigs"`

	// EnablePid
	EnablePid bool `json:"enablePid"` // 是否允许创建pid文件，防止重复启动
}

// ModuleConfig module信息
type ModuleConfig struct {
	ModuleName  string `json:"moduleName"`  // 名称，如tomcat-hook
	ModuleType  string `json:"moduleType"`  // 模块类型：hook、algorithm
	DownLoadURL string `json:"downLoadURL"` // 下载链接
	Md5         string `json:"md5"`         // 插件hash
	// TODO 参数类型使用interface{}, 尽量保存原始类型
	Parameters map[string][]interface{} `json:"parameters"` // 参数列表
}

func InitConfig() (*Config, error) {
	var (
		v   *viper.Viper
		err error
		c   Config
	)

	v = viper.New()
	v.SetConfigName("config") // 文件名称
	v.SetConfigType("json")   // 文件类型

	// 安装目录下的config
	v.AddConfigPath("../config")
	v.AddConfigPath("./config")

	setDefaultValue(v) // 设置系统默认值
	// 读取配置文件值，并覆盖系统默尔值
	if err = v.ReadInConfig(); err != nil {
		// TODO
		fmt.Printf("%s [WARN] can not read config file, use default config.\n", time.Now().Format(defs.DATE_FORMAT))
	}

	// 配置对象
	err = v.Unmarshal(&c)
	if err != nil {
		// TODO
		fmt.Printf("unmarshal config failed: %v\n", err)
	}
	return &c, nil
}

// 给参数设置默认值
func setDefaultValue(vp *viper.Viper) {
	vp.SetDefault("Version", VERSION)
	vp.SetDefault("AgentMode", STATIC)
	vp.SetDefault("Namespace", "jrasp")
	vp.SetDefault("EnableAttach", false)
	vp.SetDefault("EnableAuth", true)
	vp.SetDefault("LogLevel", 0)
	// 当前可执行文件的父目录下的logs
	vp.SetDefault("LogPath", "../logs")
	vp.SetDefault("EnablePprof", false)
	vp.SetDefault("PprofPort", 6753)
	vp.SetDefault("Password", "123456")
	vp.SetDefault("Username", "admin")
	vp.SetDefault("EnableDeleyExit", false)
	vp.SetDefault("EnableResourceCheck", false)

	vp.SetDefault("LogReportTicker", 6)
	vp.SetDefault("ScanTicker", 30)
	vp.SetDefault("RebootTicker", 7*24*60)
	vp.SetDefault("PidExistsTicker", 10)
	vp.SetDefault("ProcessInjectTicker", 30)
	vp.SetDefault("HeartBeatReportTicker", 5)
	vp.SetDefault("DependencyTicker", 12*60*60)

	vp.SetDefault("EnableBlock", false)
	vp.SetDefault("EnableRceBlock", false)

	vp.SetDefault("AttachTime", -1)

	vp.SetDefault("NamespaceId", "") // default 空间

	vp.SetDefault("NacosIps", []string{"132.232.32.246"})
	vp.SetDefault("DataId", "")

	// 可执行文件配置,默认为空，不需要更新
	vp.SetDefault("BinFileUrl", "")
	vp.SetDefault("BinFileHash", "")

	// 默认自动更新
	vp.SetDefault("ModuleAutoUpdate", true)

	vp.SetDefault("EnablePid", true)

	vp.SetDefault("ConfigId", -1)

}

// IsDynamicMode IsDynamic 是否是动态注入模式
func (config *Config) IsDynamicMode() bool {
	return config.AgentMode == DYNAMIC
}

// IsStaticMode IsNormal 是否是正常模式
func (config *Config) IsStaticMode() bool {
	return config.AgentMode == STATIC
}

// IsDisable 是否是禁用模式
func (config *Config) IsDisable() bool {
	return config.AgentMode == DISABLE
}
