package monitor

import (
	"context"
	"jrasp-daemon/defs"
	"jrasp-daemon/utils"
	"jrasp-daemon/zlog"
	"time"
)

type FileDescriptorInfo struct {
	// 指标
	Used        uint64 `json:"used"`        // 当前使用
	Limit       uint64 `json:"limit"`       // 最大限制
	UsedPercent uint64 `json:"usedPercent"` // 使用百分比, 范围0～100
	IsOverLimit bool   `json:"isOverLimit"` // 是否超限, true 超限，false 正常
	// 配置值
	MaxFileUsedPercent uint64 `json:"maxFileUsedPercent"` // 最大限制百分比, 范围0～100
	FileCheckFrequency uint64 `json:"fileCheckFrequency"` // 上报频率, 单位分钟
}

func MonitorFileDescriptor(ctx context.Context, maxUsedPercent uint64, fileCheckFrequency uint64) {
	ticker := time.NewTicker(time.Duration(fileCheckFrequency) * time.Minute)
	defer ticker.Stop()
	for {
		used, err := FDUsage()
		if err != nil {
			zlog.Debugf(defs.DEFAULT_METIRC, "failed to get file descriptor usage", err.Error())
			return
		}
		limit, err := FDLimit()
		if err != nil {
			zlog.Debugf(defs.DEFAULT_METIRC, "failed to get file descriptor limit", err.Error())
			return
		}

		monitorInfo := NewFileInfo(used, limit, maxUsedPercent, fileCheckFrequency)

		infoString := utils.ToString(monitorInfo)
		if monitorInfo.IsOverLimit {
			zlog.Warnf(defs.FILE_OPENS_METIRC, "file descriptors used percent check", "%s", infoString)
		} else {
			zlog.Infof(defs.FILE_OPENS_METIRC, "file descriptors used percent check", "%s", infoString)
		}
		select {
		case <-ticker.C:
		case _ = <-ctx.Done():
			return
		}
	}
}

func NewFileInfo(used uint64, limit uint64, maxFileUsedPercent, fileCheckFrequency uint64) *FileDescriptorInfo {
	monitorInfo := new(FileDescriptorInfo)
	monitorInfo.Used = used
	monitorInfo.Limit = limit
	monitorInfo.MaxFileUsedPercent = maxFileUsedPercent
	monitorInfo.FileCheckFrequency = fileCheckFrequency

	usedPercent := uint64(100 * float64(used) / float64(limit))
	monitorInfo.UsedPercent = usedPercent
	if usedPercent >= maxFileUsedPercent {
		monitorInfo.IsOverLimit = true
	}
	return monitorInfo
}
