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
	"strings"
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

	scanTicker                   *time.Ticker          // 注入定时器
	RebootTicker                 *time.Ticker          // 定时器重启功能
	PidExistsTicker              *time.Ticker          // 进程存活检测定时器
	LogReportTicker              *time.Ticker          // 进程信息定时上报
	HeartBeatReportTicker        *time.Ticker          // 心跳定时器
	ContainerTicker              *time.Ticker          // 容器检查定时器
	ProcessSyncMap               sync.Map              // 保存监听的java进程
	JavaProcessHandlerChan       chan *process.Process // java 进程处理chan
	JavaProcessDeleteHandlerChan chan int32            // java 进程退出处理chan

	ctx context.Context
}

func NewWatch(cfg *userconfig.Config, env *environ.Environ, ctx context.Context) *Watch {
	w := &Watch{
		env:                          env,
		cfg:                          cfg,
		selfPid:                      int32(os.Getpid()),
		LogReportTicker:              time.NewTicker(time.Hour * time.Duration(cfg.LogReportTicker)),
		scanTicker:                   time.NewTicker(time.Second * time.Duration(cfg.ScanTicker)),
		PidExistsTicker:              time.NewTicker(time.Second * time.Duration(cfg.PidExistsTicker)),
		HeartBeatReportTicker:        time.NewTicker(time.Minute * time.Duration(cfg.HeartBeatReportTicker)),
		ContainerTicker:              time.NewTicker(time.Second * time.Duration(cfg.ContainerTicker)),
		RebootTicker:                 time.NewTicker(time.Minute * time.Duration(cfg.RebootTicker)),
		JavaProcessHandlerChan:       make(chan *process.Process, 500),
		JavaProcessDeleteHandlerChan: make(chan int32, 500),
		ctx:                          ctx,
	}
	return w
}

// nacos 服务不稳定，长时间运行后容易断开连，这里设置定时重启功能（一般是1个月以上）
func (w *Watch) Reboot() {
	zlog.Debugf(defs.DEFAULT_INFO, "restart jrasp-daemon ticker start...", "reboot period:%d(Minute)", w.cfg.RebootTicker)
	for {
		select {
		case _ = <-w.ctx.Done():
			return
		case _, ok := <-w.RebootTicker.C:
			if !ok {
				return
			}
			zlog.Infof(defs.DEFAULT_INFO, "jrasp-daemon will restart...", "jrasp-deamon pid:%d", w.selfPid)
			os.Exit(0)
		}
	}
}

func (w *Watch) DoAttach() {
	for {
		select {
		case _ = <-w.ctx.Done():
			return
		case p, ok := <-w.JavaProcessHandlerChan:
			if !ok {
				zlog.Errorf(defs.WATCH_DEFAULT, "chan shutdown", "java process handler chan closed")
			}
			go w.getJavaProcessInfo(p)
		case p, ok := <-w.JavaProcessDeleteHandlerChan:
			if !ok {
				zlog.Errorf(defs.WATCH_DEFAULT, "chan shutdown", "java process handler chan closed")
			}
			w.removeExitedJavaProcess(p)
		}
	}
}

func (w *Watch) JavaStatusTimer() {
	for {
		select {
		case _ = <-w.ctx.Done():
			return
		case _, ok := <-w.LogReportTicker.C:
			if !ok {
				return
			}
			w.logJavaInfo()
		case _, ok := <-w.HeartBeatReportTicker.C:
			if !ok {
				return
			}
			w.logHeartBeat()
		}
	}
}

func (w *Watch) logJavaInfo() {
	w.ProcessSyncMap.Range(func(pid, p interface{}) bool {
		exists, err := process.PidExists(pid.(int32))
		if err != nil || !exists {
			// 出错或者不存在时，删除
			w.ProcessSyncMap.Delete(pid)
			// todo 对应的run/pid目录确认删除
			zlog.Infof(defs.JAVA_PROCESS_SHUTDOWN, "java process exit", "%d", pid)
		} else {
			processJava := (p).(*java_process.JavaProcess)
			zlog.Infof(defs.WATCH_DEFAULT, "[LogReport]", utils.ToString(processJava))
		}
		return true
	})
}

func (w *Watch) logHeartBeat() {
	hb := NewHeartBeat()
	w.ProcessSyncMap.Range(func(pid, p interface{}) bool {
		exists, err := process.PidExists(pid.(int32))
		if err != nil || !exists {
			// 出错或者不存在时，删除
			w.ProcessSyncMap.Delete(pid)
			// todo 对应的run/pid目录确认删除
			zlog.Infof(defs.JAVA_PROCESS_SHUTDOWN, "java process exit", "%d", pid)
		} else {
			processJava := (p).(*java_process.JavaProcess)
			hb.Append(processJava)
		}
		return true
	})
	zlog.Infof(defs.HEART_BEAT, "[logHeartBeat]", hb.toJsonString())
}

// 进程状态、配置等检测
func (w *Watch) getJavaProcessInfo(procss *process.Process) {
	// 判断是否已经检查过了
	_, f := w.ProcessSyncMap.Load(procss.Pid)
	if f {
		// todo 判断进程启动时间,防止进程退出后再次启动使用相同pid，10秒内重启的进程
		zlog.Debugf(defs.WATCH_DEFAULT, "java process has been monitored", "javaPid:%d", procss.Pid)
		return
	}

	javaProcess := java_process.NewJavaProcess(procss, w.cfg, w.env)

	// cmdline 信息
	javaProcess.SetCmdLines()

	// AppNames 信息
	javaProcess.SetAppNames()

	// IDEA
	for _, v := range javaProcess.CmdLines {
		if strings.Contains(v, "IDEA") || strings.Contains(v, "GoLand") || strings.Contains(v, "vscode") {
			zlog.Debugf(defs.WATCH_DEFAULT, "idea or vscode process, java process ignore.", "javaPid:%d", procss.Pid)
			return
		}
		if strings.Contains(v, "/ECCollect/") {
			zlog.Debugf(defs.WATCH_DEFAULT, "Epoint ECCollect process, java process ignore.", "javaPid:%d", procss.Pid)
			return
		}
		if strings.Contains(v, "/ECUpdate/") {
			zlog.Debugf(defs.WATCH_DEFAULT, "Epoint ECUpdate process, java process ignore.", "javaPid:%d", procss.Pid)
			return
		}
		if strings.Contains(v, "/ECReport/") {
			zlog.Debugf(defs.WATCH_DEFAULT, "Epoint ECReport process, java process ignore.", "javaPid:%d", procss.Pid)
			return
		}
		if strings.Contains(v, "/zookeeper/") {
			zlog.Debugf(defs.WATCH_DEFAULT, "ZooKeeper process, java process ignore.", "javaPid:%d", procss.Pid)
			return
		}
	}

	// 只有wrapper进程启动的才进行注入
	//if javaProcess.CmdLines[len(javaProcess.CmdLines)-1] != "stop" {
	//	zlog.Debugf(defs.WATCH_DEFAULT, "None wrapper start process, java process ignore.", "javaPid:%d", procss.Pid)
	//	return
	//}

	// 发下进程到开启注入时间
	time.Sleep(15 * time.Second)
	// 设置java进程启动时间
	startTime := javaProcess.SetStartTime()

	// 获取进程的注入状态
	javaProcess.GetAndMarkStatus()

	if w.cfg.IsDisable() && javaProcess.SuccessInject() {
		// 关闭注入，并且已经注入状态
		javaProcess.ExitInjectImmediately()
	} else if w.cfg.IsDynamicMode() && !javaProcess.IsInject() {
		// java进程启动完成之后注入，防止死锁和短生命周期进程如jps等
		currentTime := time.Now().UnixMilli()
		period := currentTime - startTime
		if period > 0 && period < w.cfg.MinJvmStartTime*60*1000 {
			sleepTime := w.cfg.MinJvmStartTime*60*1000 - period
			zlog.Infof(defs.WATCH_DEFAULT, "attach java goroutine sleep",
				"java process: %d, sleep time(second): %d", procss.Pid, sleepTime/1000)
			time.Sleep(time.Duration(sleepTime) * time.Millisecond)
		}

		// 等待结束之后确认进程存在
		exists, err := process.PidExists(procss.Pid)
		if err != nil || !exists {
			return
		}

		// 没有注入并且支持动态注入
		w.DynamicInject(javaProcess)
		// 读取token
		success := javaProcess.ReadTokenFile()
		if success {
			javaProcess.MarkSuccessInjected() // 已经注入过
		} else {
			javaProcess.MarkFailedExitInject() // 退出失败，文件异常
		}
	}

	// 参数更新
	if !w.cfg.IsDisable() && javaProcess.SuccessInject() {
		javaProcess.SoftFlush()
		javaProcess.UpdateParameters()
		zlog.Debugf(defs.AGENT_CONFIG_UPDATE, "update agent config", "update parameters success")
	}

	zlog.Infof(defs.JAVA_PROCESS_STARTUP, "find a java process", utils.ToString(javaProcess))

	// 进程加入观测集合中
	w.ProcessSyncMap.Store(javaProcess.JavaPid, javaProcess)
}

func (w *Watch) removeExitedJavaProcess(pid int32) {
	// 出错或者不存在时，删除
	w.ProcessSyncMap.Delete(pid)
	zlog.Infof(defs.JAVA_PROCESS_SHUTDOWN, "java process exit", "%d", pid)
}

func (w *Watch) DynamicInject(javaProcess *java_process.JavaProcess) {
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
