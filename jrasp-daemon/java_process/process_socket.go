package java_process

// 与Java进程通过自定义的socket方式通信

// Agent 退出
//type Response struct {
//	Code    int    `json:"code"`
//	Data    string `json:"data"`
//	Message string `json:"message"`
//}

//func (jp *JavaProcess) ExitInjectImmediately() bool {
//	// 关闭注入
//	success := jp.ShutDownAgent()
//	if success {
//		// 标记为成功退出状态
//		jp.MarkExitInject()
//		// 退出后消息立即上报
//		zlog.Infof(defs.AGENT_SUCCESS_EXIT, "java agent exit", `{"pid":%d,"status":"%s","startTime":"%s"}`, jp.JavaPid, jp.InjectedStatus, jp.StartTime)
//	} else {
//		// 标记为异常退出状态
//		jp.MarkFailedExitInject()
//		zlog.Errorf(defs.WATCH_DEFAULT, "[BUG] java agent exit failed", "java pid: %d,status:%t", jp.JavaPid, success)
//	}
//	return success
//}
