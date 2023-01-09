package java_process

import (
	"fmt"
	"io/ioutil"
	"jrasp-daemon/defs"
	"jrasp-daemon/zlog"
	"os"
	"path/filepath"
	"strings"
)

// 是否加载了jar文件
func IsLoaderJar(pid int32, jarName string) bool {
	path := fmt.Sprintf("/proc/%d/maps", pid)
	_, err := os.Stat(path)
	if err != nil {
		zlog.Warnf(defs.LOAD_JAR, "[isLoaderJar]", fmt.Sprintf("进程%d的maps文件不存在,err:%v", pid, err))
		return false
	}
	buf, err := ioutil.ReadFile(path)
	if err != nil {
		zlog.Warnf(defs.LOAD_JAR, "[isLoaderJar]", fmt.Sprintf("打开进程%d的maps文件失败,err:%v", pid, err))
		return false
	}
	if strings.Contains(string(buf), jarName) {
		return true
	}
	return false
}

func Check(pid int32) bool {
	// 判断socket文件是否存在
	sockfile := filepath.Join(os.TempDir(), fmt.Sprintf(".java_pid%d", pid))
	if exist(sockfile) {
		return true
	}
	return false
}
