package main

import (
	"context"
	"embed"
	"fmt"
	"jrasp-daemon/common"
	"jrasp-daemon/defs"
	"jrasp-daemon/environ"
	"jrasp-daemon/nacos"
	"jrasp-daemon/update"
	"jrasp-daemon/userconfig"
	"jrasp-daemon/utils"
	"jrasp-daemon/watch"
	"jrasp-daemon/zlog"
	"log"
	"net/http"
	"os"
	"os/signal"
	"path/filepath"
	"runtime"
	"syscall"
)

//go:embed resource
var resource embed.FS

func init() {
	signal.Notify(defs.Sig, syscall.SIGINT, syscall.SIGTERM, syscall.SIGKILL)
}

func main() {

	fmt.Print(defs.LOGO)
	// main协程退出之后，其他协程立即退出
	// 与Java存在区别
	ctx, cancelFun := context.WithCancel(context.TODO())

	// 环境变量初始化
	env, err := environ.NewEnviron()
	if err != nil {
		fmt.Printf("env init error %s\n", err.Error())
		return
	}

	// 配置初始化
	conf, err := userconfig.InitConfig()
	if err != nil {
		fmt.Printf("userconfig init error %s\n", err.Error())
		return
	}

	// zap日志初始化
	zlog.InitLog(conf.LogLevel, conf.LogPath, env.HostName, env.Ip)

	// 创建pid文件
	pidFile := common.New(env.PidFile)
	pidFile.Lock()
	defer pidFile.Unlock()

	// 根据操作系统和架构释放对应的jattach文件
	extractFiles()

	// jrasp-daemon 启动标志
	zlog.Infof(defs.START_UP, "daemon startup", `{"agentMode":"%s"}`, conf.AgentMode)

	zlog.Infof(defs.CONFIG_ID, "config id", `{"configId":%d}`, conf.ConfigId)

	// 日志配置值
	zlog.Infof(defs.LOG_VALUE, "log config value", "logLevel:%d,logPath:%s", conf.LogLevel, conf.LogPath)

	// 环境信息打印
	zlog.Infof(defs.ENV_VALUE, "env config value", utils.ToString(env))

	// 配置信息打印
	zlog.Infof(defs.CONFIG_VALUE, "user config value", utils.ToString(conf))

	// 配置客户端初始化
	nacos.NacosInit(conf, env)

	// 可执行文件下载
	ossClient := update.NewUpdateClient(conf, env)

	// 下载最新的可执行文件
	ossClient.UpdateDaemonFile()

	// 下载模块插件
	ossClient.DownLoadModuleFiles()

	newWatch := watch.NewWatch(conf, env, ctx)

	// 定时重启功能
	go newWatch.Reboot()

	// 加强版的jps工具
	go newWatch.NotifyJavaProcess()

	// 进程注入
	go newWatch.DoAttach()

	// 进程状态定时上报
	go newWatch.JavaStatusTimer()

	// start pprof for debug
	go debug(conf)

	// block main
	<-defs.Sig
	cancelFun()
}

func debug(conf *userconfig.Config) {
	if conf.EnablePprof {
		err := http.ListenAndServe(fmt.Sprintf(":%d", conf.PprofPort), nil)
		if err != nil {
			zlog.Errorf(defs.DEBUG_PPROF, "pprof ListenAndServe failed", "err:%s", err.Error())
		}
	}
}

func getRaspHome() (string, error) {
	runDir, err := filepath.Abs(filepath.Dir(os.Args[0]))
	raspHome, err := filepath.Abs(filepath.Dir(runDir))
	if err != nil {
		return "", err
	}
	return raspHome, nil
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

func extractFiles() {
	raspHome, err := getRaspHome()
	if err != nil {
		log.Fatalln("get jrasp home error:%v", err)
		return
	}
	filePath := filepath.Join(raspHome, "bin", getJattachExe())
	// 判断文件是否存在，如果存在就不重复释放文件了
	_, err = os.Stat(filePath)
	if err == nil || os.IsExist(err) {
		return
	}
	var data []byte
	switch runtime.GOOS {
	case "windows":
		data, _ = resource.ReadFile("resource/jattach.exe")
		break
	case "linux":
		arch := runtime.GOARCH
		if arch == "amd64" {
			data, _ = resource.ReadFile("resource/jattach_linux_amd64")
		} else {
			data, _ = resource.ReadFile("resource/jattach_linux_aarch64")
		}
		break
	case "darwin":
		data, _ = resource.ReadFile("resource/jattach_darwin")
		break
	}
	err = os.WriteFile(filePath, data, 0777)
	if err != nil {
		log.Fatalf("extract file failed", "err:%s", err.Error())
	}
}
