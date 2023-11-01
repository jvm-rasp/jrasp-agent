package watch

import (
	"github.com/shirou/gopsutil/v3/process"
	"jrasp-daemon/defs"
	"jrasp-daemon/java_process"
	"jrasp-daemon/utils"
	"jrasp-daemon/zlog"
)

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
		case _, ok := <-w.JavaProcessExitTicker.C:
			if !ok {
				return
			}
			w.scanJavaExit()
		case _, ok := <-w.HeartBeatReportTicker.C:
			if !ok {
				return
			}
			w.logHeartBeat()
		}
	}
}

// 小时级别同步Java全部信息
func (w *Watch) logJavaInfo() {
	w.JavaProcessSyncMap.Range(func(pid, p interface{}) bool {
		processJava := (p).(*java_process.JavaProcess)
		zlog.Infof(defs.WATCH_DEFAULT, "[LogReport]", utils.ToString(processJava))
		return true
	})
}

// 秒级别监听进程的退出情况
// Java进程数量比普通进程少的多
func (w *Watch) scanJavaExit() {
	w.JavaProcessSyncMap.Range(func(pid, p interface{}) bool {
		exists, err := process.PidExists(pid.(int32))
		if err != nil || !exists {
			w.JavaProcessSyncMap.Delete(pid)
			zlog.Infof(defs.JAVA_PROCESS_SHUTDOWN, "java process exit", "java pid: %d", pid)
		}
		return true
	})
}

// 分钟级别同步Java pid信息
func (w *Watch) logHeartBeat() {
	hb := NewHeartBeat()
	w.JavaProcessSyncMap.Range(func(pid, p interface{}) bool {
		processJava := (p).(*java_process.JavaProcess)
		hb.Append(processJava)
		return true
	})
	zlog.Infof(defs.HEART_BEAT, "[logHeartBeat]", hb.toJsonString())
}
