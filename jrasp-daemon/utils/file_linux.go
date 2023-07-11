package utils

import (
	"github.com/shirou/gopsutil/v3/process"
	"strings"
)

func OpenFiles(pid int32, jarName string) bool {
	p, err := process.NewProcess(pid)
	openFiles, err := p.OpenFiles()
	if err != nil {
		return false
	}
	for i := 0; i < len(openFiles); i++ {
		if strings.Contains(openFiles[i].Path, jarName) {
			return true
		}
	}
	return false
}
