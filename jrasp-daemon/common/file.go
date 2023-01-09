package common

import (
	"os"
)

// PidFile 防止进程启动多个
type PidFile struct {
	dir string
	f   *os.File
}

func New(dir string) *PidFile {
	return &PidFile{
		dir: dir,
	}
}