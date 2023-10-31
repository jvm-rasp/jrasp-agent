package java_process

import (
	"errors"
	"fmt"
	"jrasp-daemon/defs"
	"jrasp-daemon/utils"
	"jrasp-daemon/zlog"
	"os"
	"path/filepath"
)

// ReadTokenFile token文件读取、解析
// 兼容容器文件读取
func (jp *JavaProcess) ReadTokenFile() bool {
	tokenFilePath := jp.getTokenFilePath()
	fileContent, err := readFileContent(tokenFilePath)
	if err != nil {
		return false
	}
	return jp.initInjectInfo(fileContent)
}

func (jp *JavaProcess) getTokenFilePath() string {
	if jp.IsContainer {
		// TODO bug
		return filepath.Join(fmt.Sprintf(defs.LINUX_PROC_ROOT, jp.JavaPid),
			jp.env.InstallDir, "run", fmt.Sprintf("%d", jp.InContainerPid), defs.JRASP_TOKEN_FILE)
	}
	return filepath.Join(jp.env.InstallDir, "run", fmt.Sprintf("%d", jp.JavaPid), defs.JRASP_TOKEN_FILE)
}

func readFileContent(filePath string) (string, error) {
	if utils.PathExists(filePath) {
		fileContent, err := os.ReadFile(filePath)
		if err != nil {
			return "", err
		}
		return string(fileContent), nil
	}
	return "", errors.New("token file does not exist")
}

func (jp *JavaProcess) initInjectInfo(jraspInfo string) bool {
	tokens, err := utils.SplitContent(jraspInfo, ";")
	if err != nil {
		return false
	}

	if len(tokens) <= 4 {
		return false
	}

	jp.ServerIp = tokens[1]
	jp.ServerPort = tokens[2]
	jp.ProcessId = tokens[3]
	jp.RaspVersion = tokens[4]

	zlog.Infof(defs.ATTACH_READ_TOKEN, "[jrasp token]",
		"ip: %s, port: %s, processId: %s, raspVersion: %s",
		jp.ServerIp, jp.ServerPort, jp.ProcessId, jp.RaspVersion)
	return true

}
