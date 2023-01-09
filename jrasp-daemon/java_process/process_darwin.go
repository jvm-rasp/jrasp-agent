package java_process

import (
	"fmt"
	"jrasp-daemon/utils"
	"os"
	"path/filepath"
)

func IsLoaderJar(pid int32, jarName string) bool {
	return utils.OpenFiles(pid, jarName)
}

func Check(pid int32) bool {
	// 判断socket文件是否存在
	sockfile := filepath.Join(os.TempDir(), fmt.Sprintf(".java_pid%d", pid))
	if exist(sockfile) {
		return true
	}
	return false
}
