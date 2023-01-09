package attach

import (
	"fmt"
	"log"
	"os"
	"path/filepath"
)

func Check(pid int) bool {
	// 判断socket文件是否存在
	sockfile := filepath.Join(os.TempDir(), fmt.Sprintf(".java_pid%d", pid))
	if exist(sockfile) {
		log.Println("jvm create uds socket file success")
		return true
	} else {
		log.Fatalln("jvm create socket file failed")
	}
	return false
}

func exist(filename string) bool {
	_, err := os.Stat(filename)
	return err == nil || os.IsExist(err)
}