package java_process

import (
	"errors"
	"fmt"
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

	"github.com/shirou/gopsutil/process"
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
	// 通过attach 传递给目标jvm的参数
	agentArgs := fmt.Sprintf("raspHome=%s;coreVersion=%s", jp.env.InstallDir, jp.cfg.Version)
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

// CheckRunDir run/pid目录
func (jp *JavaProcess) CheckRunDir() bool {
	runPidFilePath := filepath.Join(jp.env.InstallDir, "run", fmt.Sprintf("%d", jp.JavaPid))
	exist, err := utils.PathExists(runPidFilePath)
	if err != nil || !exist {
		return false
	}
	return true
}

func (jp *JavaProcess) ReadTokenFile() bool {
	// todo 增加重试次数
	tokenFilePath := filepath.Join(jp.env.InstallDir, "run", fmt.Sprintf("%d", jp.JavaPid), ".jrasp.token")
	exist, err := utils.PathExists(tokenFilePath)
	if err != nil {
		zlog.Infof(defs.ATTACH_READ_TOKEN, "[token file]", "check token file[%s],error:%v", tokenFilePath, err)
		return false
	}

	// 文件存在
	if exist {
		ip, port, err := splitContent(tokenFilePath)
		if err != nil {
			return false
		}
		jp.ServerIp = ip
		jp.ServerPort = port
		zlog.Debugf(defs.ATTACH_READ_TOKEN, "[ip:port]", "ip: %s,port: %s", ip, port)
		return true
	}
	zlog.Errorf(defs.ATTACH_READ_TOKEN, "[token file]", "attach token file[%s] not exist", tokenFilePath)
	return false
}

func splitContent(tokenFilePath string) (string, string, error) {
	fileContent, err := ioutil.ReadFile(tokenFilePath)
	if err != nil {
		zlog.Errorf(defs.ATTACH_READ_TOKEN, "[token file]", "read attach token file[%s],error:%v", tokenFilePath, err)
		return "", "", err
	}
	fileContentStr := string(fileContent)
	fileContentStr = strings.Replace(fileContentStr, " ", "", -1) // 字符串去掉"\n"和"空格"
	fileContentStr = strings.Replace(fileContentStr, "\n", "", -1)
	tokenArray := strings.Split(fileContentStr, ";")
	zlog.Debugf(defs.ATTACH_READ_TOKEN, "[token file]", "token file content:%s", fileContentStr)
	if len(tokenArray) == 3 {
		return tokenArray[1], tokenArray[2], nil
	}
	zlog.Errorf(defs.ATTACH_READ_TOKEN, "[Attach]", "[Fix it] token file content bad,tokenFilePath:%s,fileContentStr:%s", tokenFilePath, fileContentStr)
	return "", "", err
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

func (jp *JavaProcess) GetPid() int32 {
	return jp.JavaPid
}

func (jp *JavaProcess) SetStartTime() {
	startTime, err := jp.process.CreateTime()
	if err != nil {
		zlog.Warnf(defs.WATCH_DEFAULT, "get process startup time error", `{"pid":%d,"err":%v}`, jp.JavaPid, err)
	}
	timeUnix := time.Unix(startTime/1000, 0)
	timsStr := timeUnix.Format(defs.DATE_FORMAT)
	jp.StartTime = timsStr
}

func (jp *JavaProcess) GetAndMarkStatus() {
	if jp.CheckRunDir() {
		success := jp.ReadTokenFile()
		if success {
			jp.MarkSuccessInjected() // 已经注入过
		} else {
			jp.MarkFailedExitInject() // 退出失败，文件异常
		}
	} else {
		jp.MarkNotInjected() // 未注入过
	}
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
