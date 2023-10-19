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
		case _, ok := <-w.HeartBeatReportTicker.C:
			if !ok {
				return
			}
			w.logHeartBeat()
		}
	}
}

func (w *Watch) logJavaInfo() {
	w.JavaProcessSyncMap.Range(func(pid, p interface{}) bool {
		exists, err := process.PidExists(pid.(int32))
		if err != nil || !exists {
			// 出错或者不存在时，删除
			w.JavaProcessSyncMap.Delete(pid)
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
	w.JavaProcessSyncMap.Range(func(pid, p interface{}) bool {
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
