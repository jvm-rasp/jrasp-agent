package main

import (
	"context"
	"fmt"
	"jrasp-daemon/common"
	"jrasp-daemon/defs"
	"jrasp-daemon/environ"
	"jrasp-daemon/monitor"
	"jrasp-daemon/remote"
	"jrasp-daemon/socket"
	"jrasp-daemon/update"
	"jrasp-daemon/userconfig"
	"jrasp-daemon/utils"
	"jrasp-daemon/watch"
	"jrasp-daemon/zlog"
	"net/http"
	"os/signal"
	"syscall"
)

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
		return
	}

	// zap日志初始化
	zlog.InitLog(conf.LogLevel, conf.LogPath, env.HostName, env.Ip)

	// 创建pid文件
	pidFile := NewPidFile(env.PidFile)
	pidFile.Lock()
	defer pidFile.Unlock()

	// jrasp-daemon 启动标志
	zlog.Infof(defs.START_UP, "daemon startup", `{"agentMode":"%s"}`, conf.AgentMode)

	zlog.Infof(defs.CONFIG_ID, "config id", `{"configId":%d}`, conf.ConfigId)

	// 日志配置值
	zlog.Debugf(defs.LOG_VALUE, "zlog config value", "logLevel:%d,logPath:%s", conf.LogLevel, conf.LogPath)

	// 环境信息打印
	zlog.Infof(defs.ENV_VALUE, "env config value", utils.ToString(env))

	// 配置信息打印
	zlog.Infof(defs.CONFIG_VALUE, "user config value", "") // utils.ToString(conf)

	// 可执行文件下载
	ossClient := update.NewUpdateClient(conf, env)

	// 清理无效或者过期的pid目录
	ossClient.CleanPidFiles()

	// 下载最新的可执行文件
	ossClient.UpdateDaemonFile()

	// 下载最新的agent jar
	ossClient.DownLoadAgentFiles()

	// 下载模块插件
	ossClient.DownLoadModuleFiles()

	newWatch := watch.NewWatch(conf, env, ctx)

	go remote.WatchRemoteConfig(conf, env)

	go monitor.MonitorFileDescriptor(ctx, conf.MaxFileUsedPercent, conf.FileCheckFrequency)

	// 加强版的jps工具
	go newWatch.ProcessScan()

	daemonSocket := socket.NewDaemonSocket(conf.DaemonPort)

	go daemonSocket.Start(ctx)

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

type PidFile struct {
	dir string
	f   *os.File
}

func NewPidFile(dir string) *PidFile {
	return &PidFile{
		dir: dir,
	}
}

func (p *PidFile) Lock() error {
	// create pid file
	f, err := os.OpenFile(p.dir, os.O_WRONLY|os.O_CREATE, os.FileMode(defs.FILE_MODE_ONLY_ROOT))
	if err != nil {
		zlog.Errorf(defs.CREATE_PID_FILE, "create daemon pid file", "open or create pid file err:%v", err)
		os.Exit(defs.EXIT_CODE_2)
	}

	// try lock pid file
	err = syscall.Flock(int(f.Fd()), syscall.LOCK_EX|syscall.LOCK_NB)
	if err != nil {
		zlog.Errorf(defs.CREATE_PID_FILE, "cannot flock pid file", "err:%v", err)
		os.Exit(defs.EXIT_CODE_2)
	}

	// update pid fie content
	err = os.Truncate(p.dir, 0)
	if err != nil {
		zlog.Errorf(defs.CLEAN_PID_FILE, "clean pid file content", "err:%v", err)
		os.Exit(defs.EXIT_CODE_2)
	}

	_, err = fmt.Fprintf(f, "%d", os.Getpid())
	if err != nil {
		zlog.Errorf(defs.WRITE_PID_FILE, "update pid file content", "write pid[%d] to file error: %v", os.Getpid(), err)
		os.Exit(defs.EXIT_CODE_2)
	}
	p.f = f
	return nil
}

func (p *PidFile) Unlock() error {
	defer p.f.Close()
	return syscall.Flock(int(p.f.Fd()), syscall.LOCK_UN)
}

