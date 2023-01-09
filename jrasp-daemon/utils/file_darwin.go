package utils

import (
	"bufio"
	"fmt"
	"jrasp-daemon/defs"
	"jrasp-daemon/zlog"
	"os/exec"
	"strings"
)

// OpenFiles mac上简单粗暴的命令执行
func OpenFiles(pid int32, jarName string) bool {
	cmd := exec.Command("/bin/bash", "-c", fmt.Sprintf("lsof -p %d | grep jar", pid))
	//创建获取命令输出的管道
	stdout, err := cmd.StdoutPipe()
	if err != nil {
		zlog.Warnf(defs.UTILS_OpenFiles, "[OpenFiles]", "can not obtain stdout pipe for command:%v", err)
		return false
	}
	//执行命令
	if err := cmd.Start(); err != nil {
		zlog.Warnf(defs.UTILS_OpenFiles, "[OpenFiles]", "cmd exec start err:%v", err)
		return false
	}
	//使用带缓冲的读取器
	outputBuf := bufio.NewReader(stdout)
	for {
		output, _, err := outputBuf.ReadLine()
		if err != nil {
			return false
		}
		b := strings.TrimSpace(string(output))
		if strings.Contains(b, jarName) {
			return true
		}
	}
}
