package java_process

import (
	"errors"
	"fmt"
	"io"
	"io/ioutil"
	"jrasp-daemon/defs"
	"jrasp-daemon/environ"
	"jrasp-daemon/socket"
	"jrasp-daemon/userconfig"
	"jrasp-daemon/utils"
	"jrasp-daemon/zlog"
	"net/http"
	"net/url"
	"os"
	"os/exec"
	"path/filepath"
	"runtime"
	"strconv"
	"strings"
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

// 执行attach
func (jp *JavaProcess) Attach() error {
	// 执行attach并检查java_pid文件
	err := jp.execCmd()
	if err != nil {
		return err
	}

	// 判断socket文件是否存在
	success := Check(jp.JavaPid)
	if !success {
		return errors.New("check attach result error!")
	}

	return nil
}

func (jp *JavaProcess) execCmd() error {
	zlog.Debugf(defs.ATTACH_DEFAULT, "[Attach]", "attach to jvm[%d] start...", jp.JavaPid)
	// logPath 转换成绝对路径
	logPathAbs, err := filepath.Abs(jp.cfg.LogPath)
	if err != nil {
		zlog.Warnf(defs.ATTACH_DEFAULT, "[Attach]", "logPath: %s, logPathAbs: %s", jp.cfg.LogPath, logPathAbs)
	}
	// 通过attach 传递给目标jvm的参数
	agentArgs := fmt.Sprintf("raspHome=%s;coreVersion=%s;key=%s;logPath=%s;",
		jp.env.InstallDir, jp.cfg.Version, jp.transformKey(jp.env.BuildDecryptKey), logPathAbs)
	// jattach pid load instrument false jrasp-launcher.jar
	cmd := exec.Command(
		filepath.Join(jp.env.InstallDir, "bin", getJattachExe()),
		fmt.Sprintf("%d", jp.JavaPid),
		"load", "instrument", "false", fmt.Sprintf("%s=%s", filepath.Join(jp.env.InstallDir, "lib", "jrasp-launcher-"+jp.cfg.Version+".jar"), agentArgs),
	)

	zlog.Debugf(defs.ATTACH_DEFAULT, "[Attach]", "cmdArgs:%s", cmd.Args)
	// 权限切换在 jattach 里面做了，直接在root权限下执行命令就行
	if err := cmd.Start(); err != nil {
		zlog.Warnf(defs.ATTACH_DEFAULT, "[Attach]", "cmd.Start error:%v", err)
		return err
	}

	if err := cmd.Wait(); err != nil {
		zlog.Warnf(defs.ATTACH_DEFAULT, "[Attach]", "cmd.Wait error:%v", err)
		// release 仅在 wait 调用失败时使用
		err = cmd.Process.Release()
		if err != nil {
			zlog.Warnf(defs.ATTACH_DEFAULT, "[Attach]", "cmd.Process.Release error:%v", err)
			return err
		}
		return errors.New("cmd.Wait error!")
	}

	return nil
}

// keyVersion 转换为key
func (jp *JavaProcess) transformKey(keyVersion string) string {
	// todo 实现密钥转换逻辑
	return keyVersion
}

// CheckRunDir run/pid目录
func (jp *JavaProcess) CheckRunDir() bool {
	runPidFilePath := filepath.Join(jp.env.InstallDir, "run", fmt.Sprintf("%d", jp.JavaPid))
	return utils.PathExists(runPidFilePath)
}

func (jp *JavaProcess) ReadTokenFile() bool {
	tokenFilePath := filepath.Join(jp.env.InstallDir, "run", fmt.Sprintf("%d", jp.JavaPid), ".jrasp.token")
	exist := utils.PathExists(tokenFilePath)
	// 文件存在
	if exist {
		fileContent, err := ioutil.ReadFile(tokenFilePath)
		if err != nil {
			return false
		}
		if jp.InitInjectInfo(string(fileContent)) {
			return true
		}
	}
	return false
}

func (jp *JavaProcess) InitInjectInfo(jraspInfo string) bool {
	tokens, err := utils.SplitContent(jraspInfo, ";")
	if err == nil && len(tokens) >= 5 {
		jp.ServerIp = tokens[1]
		jp.ServerPort = tokens[2]
		jp.Uuid = tokens[3]
		jp.RaspVersion = tokens[4]
		zlog.Infof(defs.ATTACH_READ_TOKEN, "[ip:port:uuid:raspVersion]", "ip: %s, port: %s, uuid: %s, raspVersion: %s", jp.ServerIp, jp.ServerPort, jp.Uuid, jp.RaspVersion)
		return true
	}
	return false
}

// 读取jvm的系统参数
func (jp *JavaProcess) AttachReadJVMProperties() {

	cmd := exec.Command(filepath.Join(jp.env.InstallDir, "bin", getJattachExe()), fmt.Sprintf("%d", jp.JavaPid), "properties")

	// 创建管道来获取命令的标准输出
	stdout, err := cmd.StdoutPipe()
	if err != nil {
		zlog.Warnf(defs.ATTACH_DEFAULT, "[Attach]", "cmd.StdoutPipe error:%v", err)
		return
	}

	// 启动命令
	if err := cmd.Start(); err != nil {
		zlog.Warnf(defs.ATTACH_DEFAULT, "[Attach]", "cmd.Start error:%v", err)
		return
	}

	// 读取命令输出
	cmdOut, err := io.ReadAll(stdout)
	if err != nil {
		zlog.Warnf(defs.ATTACH_DEFAULT, "[Attach]", "ioutil.ReadAll error:%v", err)
		return
	}

	// 等待命令执行完成
	if err := cmd.Wait(); err != nil {
		zlog.Warnf(defs.ATTACH_DEFAULT, "[Attach]", "cmd.Wait error:%v", err)
		// 释放资源
		err = cmd.Process.Release()
		if err != nil {
			zlog.Warnf(defs.ATTACH_DEFAULT, "[Attach]", "cmd.Process.Release error:%v", err)
			return
		}
	}

	if jp.PropertiesMap == nil {
		jp.PropertiesMap = make(map[string]string)
	}

	if cmdOut != nil && len(cmdOut) > 0 {
		cmdOutString := string(cmdOut)
		// TODO windows 换行符号
		resultArray := strings.Split(cmdOutString, "\n")
		if resultArray != nil && len(resultArray) > 0 {
			for _, item := range resultArray {
				if item != "" {
					itemArray := strings.SplitN(item, "=", 2)
					if len(itemArray) == 2 {
						// 对值进行覆盖
						jp.PropertiesMap[itemArray[0]] = itemArray[1]
					}
				}
			}
		}
	}

}

// UpdateParameters 更新模块参数
func (jp *JavaProcess) UpdateParameters() bool {
	client := socket.NewSocketClient(jp.ServerIp, jp.ServerPort)
	// 更新全局参数
	var config string = ""
	for k, v := range jp.agentConfigs {
		if k != "" {
			config += k + "=" + join2(v) + ";"
		}
	}
	client.UpdateAgentConfig(config)
	zlog.Debugf(defs.UPDATE_MODULE_PARAMETERS, "[update agent config]", "config: %s", config)

	// 更新模块参数
	for _, v := range jp.moduleConfigs {
		if v.Parameters != nil && len(v.Parameters) > 0 {
			var param = v.ModuleName + ":"
			for key, value := range v.Parameters {
				param += key + "=" + join(value) + ";"
			}
			client.SendParameters(param)
			zlog.Debugf(defs.UPDATE_MODULE_PARAMETERS, "[update module config]", "param: %s", param)
		}
	}
	return true
}

func join(params []interface{}) string {
	var paramSlice []string
	for _, param := range params {
		switch param.(type) {
		case string:
			paramSlice = append(paramSlice, url.PathEscape(param.(string))) // 使用url编码防止存在特殊字符
		case bool:
			paramSlice = append(paramSlice, strconv.FormatBool(param.(bool)))
		case []string:
			paramSlice = append(paramSlice, strings.Join(param.([]string), ","))
		case int8, int16, int32, int, int64, uint8, uint16, uint32, uint, uint64:
			paramSlice = append(paramSlice, strconv.Itoa(param.(int)))
		case float64:
			strV := strconv.FormatFloat(param.(float64), 'f', -1, 64)
			paramSlice = append(paramSlice, strV)
		case float32:
			ft := param.(float32)
			strV := strconv.FormatFloat(float64(ft), 'f', -1, 64)
			paramSlice = append(paramSlice, strV)
		// TODO 其他类型
		default:
			zlog.Errorf(defs.UPDATE_MODULE_PARAMETERS, "[data type not support]", "param: %s", param)
		}
	}
	return strings.Join(paramSlice, ",")
}

func join2(param interface{}) string {
	var paramSlice []string
	switch param.(type) {
	case string:
		paramSlice = append(paramSlice, url.PathEscape(param.(string))) // 使用url编码防止存在特殊字符
	case bool:
		paramSlice = append(paramSlice, strconv.FormatBool(param.(bool)))
	case []string:
		paramSlice = append(paramSlice, strings.Join(param.([]string), ","))
	case int8, int16, int32, int, int64, uint8, uint16, uint32, uint, uint64:
		paramSlice = append(paramSlice, strconv.Itoa(param.(int)))
	case float64:
		strV := strconv.FormatFloat(param.(float64), 'f', -1, 64)
		paramSlice = append(paramSlice, strV)
	case float32:
		ft := param.(float32)
		strV := strconv.FormatFloat(float64(ft), 'f', -1, 64)
		paramSlice = append(paramSlice, strV)
	// TODO 其他类型
	default:
		zlog.Errorf(defs.UPDATE_MODULE_PARAMETERS, "[data type not support]", "param: %s", param)
	}
	return strings.Join(paramSlice, ",")
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

func (jp *JavaProcess) SetPid(pid int32) {
	jp.JavaPid = pid
}

func (jp *JavaProcess) SetCmdLines() {
	cmdLines, err := jp.process.CmdlineSlice()
	if err != nil {
		zlog.Warnf(defs.WATCH_DEFAULT, "get process cmdLines error", `{"pid":%d,"err":%v}`, jp.JavaPid, err)
	}
	jp.CmdLines = cmdLines
}

func (jp *JavaProcess) SetAppNames() {
	jp.AppNames = []string{}
	cwd, err := jp.process.Exe()
	if err != nil {
		zlog.Warnf(defs.WATCH_DEFAULT, "get process cwd error", `{"pid":%d,"err":%v}`, jp.JavaPid, err)
		return
	}
	webapps := getWebAppsDir(cwd)
	pathExists := utils.PathExists(webapps)
	if !pathExists {
		return
	}
	dirEntry, err := os.ReadDir(webapps)
	if err != nil {
		zlog.Warnf(defs.WATCH_DEFAULT, "read dir error", `{"pid":%d,"err":%v}`, jp.JavaPid, err)
		return
	}
	for _, item := range dirEntry {
		if item.IsDir() || item.Type() == os.ModeSymlink {
			jp.AppNames = append(jp.AppNames, item.Name())
		}
	}
	zlog.Infof(defs.RESOURCE_NAME_UPDATE, "get resource success", `{"hostName": "%v", "ip": "%v", "resourceName": "%v"}`, jp.env.HostName, jp.env.Ip, jp.AppNames)
}

func (jp *JavaProcess) GetAppNames() []string {
	return jp.AppNames
}

func (jp *JavaProcess) GetPid() int32 {
	return jp.JavaPid
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

func (jp *JavaProcess) GetAndMarkStatus() {
	jp.AttachReadJVMProperties()
	jraspInfo := jp.PropertiesMap["jrasp.info"]
	if jraspInfo != "" {
		tokenArray := strings.Split(jraspInfo, ";")
		zlog.Infof(defs.ATTACH_READ_TOKEN, "attach jvm properties success", "")
		if len(tokenArray) >= 4 {
			jp.ServerIp = tokenArray[1]
			jp.ServerPort = tokenArray[2]
			jp.Uuid = tokenArray[3]
			jp.MarkSuccessInjected()
			return
		}
	} else if jp.CheckRunDir() {
		if jp.ReadTokenFile() {
			jp.MarkSuccessInjected() // 已经注入过
			return
		}
	}
	jp.MarkFailedExitInject() // 退出失败，文件异常
}

func exist(filename string) bool {
	_, err := os.Stat(filename)
	return err == nil || os.IsExist(err)
}

func (jp *JavaProcess) IsNeedUpdateModules() bool {
	return jp.needUpdateModules
}

func (jp *JavaProcess) SetNeedUpdateModules(b bool) {
	jp.needUpdateModules = b
}

func (jp *JavaProcess) IsNeedUpdateParameters() bool {
	return jp.needUpdateParameters
}

func (jp *JavaProcess) SetNeedUpdateParameters(b bool) {
	jp.needUpdateParameters = b
}

func getJattachExe() string {
	switch runtime.GOOS {
	case "darwin":
		return "jattach_darwin"
	case "linux":
		return "jattach_linux"
	case "windows":
		return "jattach.exe"
	default:
		return "UNKNOWN"
	}
}

func getWebAppsDir(root string) string {
	findDir := filepath.Join(root, "..")
	if findDir == root {
		return ""
	}
	webapps := filepath.Join(findDir, "webapps")
	pathExists := utils.PathExists(webapps)
	if !pathExists {
		return getWebAppsDir(findDir)
	} else {
		return webapps
	}
}
