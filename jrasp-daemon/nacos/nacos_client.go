package nacos

import (
	"errors"
	"github.com/asaskevich/govalidator"
	"github.com/nacos-group/nacos-sdk-go/clients"
	"github.com/nacos-group/nacos-sdk-go/common/constant"
	"github.com/nacos-group/nacos-sdk-go/vo"
	"io/ioutil"
	"jrasp-daemon/defs"
	"jrasp-daemon/environ"
	"jrasp-daemon/userconfig"
	"jrasp-daemon/utils"
	"jrasp-daemon/zlog"
	"net/url"
	"os"
	"path/filepath"
	"strconv"
	"strings"
)

type NacosInfo struct {
	Status  bool     `json:"status"`    // true 初始化成功，false，失败
	Message string   `json:"message"`   // 初始化信息
	IpAddrs []string `json:"serverIps"` // nacos 服务端ip列表
}

func NacosInit(cfg *userconfig.Config, env *environ.Environ) {

	clientConfig := constant.ClientConfig{
		NamespaceId:         cfg.NamespaceId,
		TimeoutMs:           5000,
		NotLoadCacheAtStart: true,
		LogDir:     filepath.Join(env.InstallDir, "tmp", "nacos", "log"),
		CacheDir:   filepath.Join(env.InstallDir, "tmp", "nacos", "cache"),
		RotateTime: "24h",
		MaxAge:     3,
		LogLevel:   "error",
	}

	info := &NacosInfo{
		IpAddrs: cfg.NacosIps,
		Message: "",
		Status:  true, // 默认为true
	}

	var serverConfigs []constant.ServerConfig

	for i := 0; i < len(cfg.NacosIps); i++ {
		result, err := parseAddress(cfg.NacosIps[i]) // 兼容多种格式: ip、ip:port、http://ip:port/nacos
		if err != nil {
			zlog.Errorf(defs.NACOS_INIT, "[registerStatus]", "nacos server:%v, err:%v", cfg.NacosIps, err)
			info.Message += err.Error()
			info.Status = false
			continue
		}
		serverConfig := constant.ServerConfig{
			IpAddr:      result["ip"].(string),
			ContextPath: result["path"].(string),
			Port:        result["port"].(uint64),
			Scheme:      result["scheme"].(string),
		}
		serverConfigs = append(serverConfigs, serverConfig)
	}

	// 将服务注册到nacos
	namingClient, _ := clients.NewNamingClient(
		vo.NacosClientParam{
			ClientConfig:  &clientConfig,
			ServerConfigs: serverConfigs,
		},
	)

	registerStatus, err := namingClient.RegisterInstance(vo.RegisterInstanceParam{
		Ip:          env.HostName,
		Port:        8848,
		ServiceName: env.HostName,
		Weight:      10,
		Enable:      true,
		Healthy:     true,
		Ephemeral:   true,
		Metadata:    map[string]string{"raspVersion": defs.JRASP_DAEMON_VERSION},
		ClusterName: "DEFAULT",       // 默认值DEFAULT
		GroupName:   "DEFAULT_GROUP", // 默认值DEFAULT_GROUP
	})
	if err != nil {
		zlog.Errorf(defs.NACOS_INIT, "[registerStatus]", "registerStatus:%t,nacos server:%v,err:%v", registerStatus, cfg.NacosIps, err)
		info.Message += err.Error()
		info.Status = false
	}

	configClient, err := clients.NewConfigClient(
		vo.NacosClientParam{
			ClientConfig:  &clientConfig,
			ServerConfigs: serverConfigs,
		},
	)

	// dataId配置值为空时，使用主机名称
	var dataId = ""
	if cfg.DataId == "" {
		dataId = env.HostName
	}

	//获取配置
	err = configClient.ListenConfig(vo.ConfigParam{
		DataId: dataId,
		Group:  "DEFAULT_GROUP",
		OnChange: func(namespace, group, dataId, data string) {
			zlog.Infof(defs.NACOS_LISTEN_CONFIG, "[ListenConfig]", "group:%s,dataId=%s,data=%s", group, dataId, data)
			err = ioutil.WriteFile(filepath.Join(env.InstallDir, "cfg", "config.json"), []byte(data), 0600)
			if err != nil {
				info.Message = err.Error()
				info.Status = false
				zlog.Errorf(defs.NACOS_LISTEN_CONFIG, "[ListenConfig]", "write file to config.json,err:%v", err)
			}
			zlog.Infof(defs.NACOS_LISTEN_CONFIG, "[ListenConfig]", "config update,jrasp-daemon will exit(0)...")
			os.Exit(0)
		},
	})
	// todo nacos 初始化状态监控
	if err != nil {
		zlog.Errorf(defs.NACOS_INIT, "[NacosInit]", "configClient.ListenConfig,err:%v", err)
		info.Message += err.Error()
		info.Status = false
	}
	zlog.Infof(defs.NACOS_INFO, "[NacosInit]", utils.ToString(info))
}

func parseAddress(text string) (map[string]interface{}, error) {
	var result = make(map[string]interface{})
	// 纯域名或者纯IP
	if govalidator.IsHost(text) {
		result["ip"] = text
		result["port"] = 8848
		result["scheme"] = "http"
		result["path"] = "/nacos"
		return result, nil
	}
	// 带有域名和端口
	if (!strings.HasPrefix(text, "http://") && !strings.HasPrefix(text, "https://")) &&
		len(strings.Split(text, ":")) > 1 {
		host := strings.Split(text, ":")[0]
		port := strings.Split(text, ":")[1]
		if govalidator.IsHost(host) && govalidator.IsPort(port) {
			result["ip"] = host
			result["port"], _ = strconv.ParseUint(port, 10, 32)
			result["scheme"] = "http"
			result["path"] = "/nacos"
			return result, nil
		}
	}
	if govalidator.IsURL(text) {
		u, err := url.Parse(text)
		if err != nil {
			return nil, err
		}
		result["ip"] = u.Hostname()
		result["port"], _ = strconv.ParseUint(u.Port(), 10, 32)
		result["scheme"] = u.Scheme
		result["path"] = u.Path
		return result, nil
	}
	return nil, errors.New("nacos地址不规范")
}
