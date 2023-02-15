package config

import (
	"github.com/spf13/viper"
	"jrasp-attach/common"
	"log"
	"time"
)

type Config struct {

	// agent 版本
	Version string `json:"version"`

	// 配置id
	ConfigId int `json:"configId"`

	Namespace string `json:"namespace"`

	// 日志配置
	LogPath  string `json:"logPath"`

	// module列表
	ModuleConfigs []ModuleConfig `json:"moduleConfigs"` // 模块配置

	// agent参数更新
	AgentConfigs map[string]interface{} `json:"agentConfigs"`
}

// ModuleConfig module信息
type ModuleConfig struct {
	ModuleName  string                   `json:"moduleName"`  // 名称，如tomcat-hook
	ModuleType  string                   `json:"moduleType"`  // 模块类型：hook、algorithm
	DownLoadURL string                   `json:"downLoadURL"` // 下载链接
	Md5         string                   `json:"md5"`         // 插件hash
	Parameters  map[string][]interface{} `json:"parameters"`  // 参数列表
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
		log.Fatalf("%s [WARN] can not read config file, use default config.\n", time.Now().Format(common.DATE_FORMAT))
	}

	// 配置对象
	err = v.Unmarshal(&c)
	if err != nil {
		log.Fatalf("unmarshal config failed: %v\n", err)
	}
	return &c, nil
}

func setDefaultValue(vp *viper.Viper) {
	vp.SetDefault("Version", common.VERSION)
	vp.SetDefault("Namespace", "jrasp")
	vp.SetDefault("EnableAttach", false)
	vp.SetDefault("EnableAuth", true)
	vp.SetDefault("LogLevel", 0)
	// TODO  ../logs 在windows 上是否兼容
	vp.SetDefault("LogPath", "../logs")
	vp.SetDefault("EnablePprof", false)
}
