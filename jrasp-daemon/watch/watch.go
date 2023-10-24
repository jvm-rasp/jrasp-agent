package watch

import (
	"context"
	"jrasp-daemon/defs"
	"jrasp-daemon/environ"
	"jrasp-daemon/java_process"
	"jrasp-daemon/userconfig"
	"jrasp-daemon/utils"
	"jrasp-daemon/zlog"
	"os"
	"sync"
	"time"

	"github.com/shirou/gopsutil/v3/process"
)

// Watch 监控Java进程
type Watch struct {
	// 环境变量与配置
	env     *environ.Environ
	cfg     *userconfig.Config
	selfPid int32 // jrasp-daemon进程自身pid

	scanTicker                  *time.Ticker          // 注入定时器
	RebootTicker                *time.Ticker          // 定时器重启功能
	JavaProcessScanTicker       *time.Ticker          // 识别Java进程定时器
	PidExistsTicker             *time.Ticker          // 进程存活检测定时器
	LogReportTicker             *time.Ticker          // 进程信息定时上报
	HeartBeatReportTicker       *time.Ticker          // 心跳定时器
	ProcessSyncMap              sync.Map              // 保存所有的Java进程
	JavaProcessSyncMap          sync.Map              // 保存监听的java进程
	JavaProcessHandleChan       chan *process.Process // java 进程处理chan
	JavaPidHandleChan           chan int32            // java 进程处理chan
	JavaProcessDeleteHandleChan chan int32            // java 进程退出处理chan

	ctx context.Context
}

func NewWatch(cfg *userconfig.Config, env *environ.Environ, ctx context.Context) *Watch {
	w := &Watch{
		env:                         env,
		cfg:                         cfg,
		selfPid:                     int32(os.Getpid()),
		LogReportTicker:             time.NewTicker(time.Hour * time.Duration(cfg.LogReportTicker)),
		scanTicker:                  time.NewTicker(time.Second * time.Duration(cfg.ScanTicker)),
		JavaProcessScanTicker:       time.NewTicker(time.Second * time.Duration(cfg.JavaProcessScanTicker)),
		PidExistsTicker:             time.NewTicker(time.Second * time.Duration(cfg.PidExistsTicker)),
		HeartBeatReportTicker:       time.NewTicker(time.Minute * time.Duration(cfg.HeartBeatReportTicker)),
		RebootTicker:                time.NewTicker(time.Minute * time.Duration(cfg.RebootTicker)),
		JavaProcessHandleChan:       make(chan *process.Process, 500),
		JavaPidHandleChan:           make(chan int32, 500),
		JavaProcessDeleteHandleChan: make(chan int32, 500),
		ctx:                         ctx,
	}
	return w
}

func (w *Watch) DoAttach() {
	for {
		select {
		case _ = <-w.ctx.Done():
			return
		case p, ok := <-w.JavaPidHandleChan:
			if !ok {
				zlog.Errorf(defs.WATCH_DEFAULT, "chan shutdown", "java process handler chan closed")
			}
			go w.InjectJavaProcess(p)
		case p, ok := <-w.JavaProcessDeleteHandleChan:
			if !ok {
				zlog.Errorf(defs.WATCH_DEFAULT, "chan shutdown", "java process handler chan closed")
			}
			w.removeExitedJavaProcess(p)
		}
	}
}

// 进程状态、配置等检测
func (w *Watch) InjectJavaProcess(pid int32) {
	// 进程是否已经检查过
	_, ok := w.ProcessSyncMap.Load(pid)
	if ok {
		return
	}

	w.ProcessSyncMap.Store(pid, pid)

	p, err := process.NewProcess(pid)
	if err != nil {
		zlog.Infof(defs.DEFAULT_INFO, "process.NewProcess(pid) run failed", "Java Pid: %d, err: %v", pid, err)
		return
	}

	exe, err := p.Exe()
	if err != nil || exe == "" {
		return
	}

	// 过滤Java进程
	if !utils.IsJavaExe(exe) {
		return
	}

	javaProcess := java_process.NewJavaProcess(p, w.cfg, w.env)
	// 过滤Java进程
	if javaProcess.FilterCmdLine(w.cfg.JavaCmdLineWhiteList) {
		return
	}

	// 容器检查
	javaProcess.CheckContainer()

	// 设置java进程启动时间
	startTime := javaProcess.SetStartTime()

	if w.checkInjectStatus(javaProcess) {
		// 能够获取注入文件，注入成功
		javaProcess.MarkSuccessInjected()
	} else {
		// 获取注入文件失败，未注入或者注入失败
		javaProcess.MarkNotInjected()
	}

	// Java进程上报
	zlog.Infof(defs.JAVA_PROCESS_STARTUP, "find a java process", utils.ToString(javaProcess))

	// Java进程加入观测集合中
	w.JavaProcessSyncMap.Store(javaProcess.JavaPid, javaProcess)

	//if w.cfg.IsDisable() && javaProcess.SuccessInject() {
	//	// 关闭注入，并且已经注入状态
	//	javaProcess.ExitInjectImmediately()
	//	return
	//}

	// 注入阶段
	if w.cfg.IsDynamicMode() && !javaProcess.IsInject() {
		// java进程启动完成之后注入，防止死锁和短生命周期进程如jps等
		currentTime := time.Now().UnixMilli()
		period := currentTime - startTime
		if period > 0 && period < w.cfg.MinJvmStartTime*60*1000 {
			sleepTime := w.cfg.MinJvmStartTime*60*1000 - period
			zlog.Infof(defs.WATCH_DEFAULT, "attach java goroutine sleep",
				"java process: %d, sleep time(second): %d", javaProcess.JavaPid, sleepTime/1000)
			time.Sleep(time.Duration(sleepTime) * time.Millisecond)
		}

		// 等待结束之后确认进程存在
		exists, err := process.PidExists(javaProcess.JavaPid)
		if err != nil || !exists {
			return
		}

		// 没有注入并且支持动态注入
		w.DynamicInject(javaProcess)

		// 判断是否注入成功
		if w.checkInjectStatus(javaProcess) {
			javaProcess.MarkSuccessInjected()
		} else {
			javaProcess.MarkFailedExitInject()
		}
	}

	// 参数更新
	if !w.cfg.IsDisable() && javaProcess.SuccessInject() {
		// 等待连接就绪
		if javaProcess.WaiteConn() {
			javaProcess.Conn.SendFlushCommand("false")
			javaProcess.Conn.UpdateAgentConfig(utils.ToString(javaProcess.Cfg.AgentConfigs))
			javaProcess.Conn.UpdateModuleConfig(utils.ToString(javaProcess.Cfg.ModuleConfigs))
			zlog.Infof(defs.AGENT_CONFIG_UPDATE, "update agent/module config", "update config success")
		}
	}

}

func (w *Watch) checkInjectStatus(javaProcess *java_process.JavaProcess) bool {
	// 获取进程的注入状态
	return javaProcess.ReadTokenFile()
}

func (w *Watch) removeExitedJavaProcess(pid int32) {
	// 出错或者不存在时，删除
	w.ProcessSyncMap.Delete(pid)
	zlog.Infof(defs.JAVA_PROCESS_SHUTDOWN, "java process exit", "%d", pid)
}

func (w *Watch) DynamicInject(javaProcess *java_process.JavaProcess) {

	// linux系统上的容器进程，复制jar包
	// macos、windows 基于虚拟机实现docker
	if w.env.IsLinux() && javaProcess.IsContainer {
		err := javaProcess.CopyJar2Proc()
		if err != nil {
			// TODO
			return
		}
	}

	err := javaProcess.Attach()
	if err != nil {
		// java_process 执行失败
		zlog.Errorf(defs.WATCH_DEFAULT, "[BUG] attach to java failed", "taget jvm[%d],err:%v", javaProcess.JavaPid, err)
		javaProcess.MarkFailedInjected()
	} else {
		// load agent 之后，标记为[注入状态]，防止 agent 错误再次发生，人工介入排查
		javaProcess.MarkSuccessInjected()
		zlog.Infof(defs.AGENT_SUCCESS_INIT, "java agent init success", `{"pid":%d,"status":"%s","startTime":"%s"}`, javaProcess.JavaPid, javaProcess.InjectedStatus, javaProcess.StartTime)
	}
}
