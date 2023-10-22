package java_process

import (
	"jrasp-daemon/defs"
	"jrasp-daemon/zlog"
	"time"
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
