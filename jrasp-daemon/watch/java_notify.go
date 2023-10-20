package watch

import (
	"io/ioutil"
	"jrasp-daemon/defs"
	"jrasp-daemon/zlog"
	"os"
	"strconv"
	"strings"

	"github.com/shirou/gopsutil/v3/process"
)

// 定时扫描全量进程
func (w *Watch) ProcessScan() {
	// 已经启动的进程并且是非容器进程
	dir, err := ioutil.ReadDir(os.TempDir())
	if err != nil {
		return
	}
	zlog.Infof(defs.WATCH_DEFAULT, "starting watch temp dir", "temp dir is: %v", os.TempDir())
	pathSep := string(os.PathSeparator)
	for _, fi := range dir {
		if fi.IsDir() && isHsPerfDataDir(fi.Name()) {
			dirPath := os.TempDir() + pathSep + fi.Name()
			_ = w.appendPidToChan(dirPath)
		}
	}

	// 已经启动的进程，容器进程
	w.DoOnceProcessScan()

	// 运行时启动进程
	for {
		select {
		case _, ok := <-w.JavaProcessScanTicker.C:
			if !ok {
				continue
			}
			w.DoOnceProcessScan()
		case _ = <-w.ctx.Done():
			return
		}
	}
}

// 一个用户目录下进程
func (w *Watch) appendPidToChan(dirPth string) (err error) {
	dir, err := ioutil.ReadDir(dirPth)
	if err != nil {
		return err
	}
	for _, fi := range dir {
		if fi.IsDir() {
			continue
		}
		pid, err := strconv.Atoi(fi.Name())
		if err == nil && pid > 0 {
			w.JavaPidHandleChan <- int32(pid)
		}
	}
	return nil
}

func (w *Watch) DoOnceProcessScan() {
	pids, err := process.Pids()
	if err != nil {
		return
	}

	for _, pid := range pids {
		if pid > 0 {
			w.JavaPidHandleChan <- pid
		}
	}
}

func isHsPerfDataDir(name string) bool {
	return strings.HasPrefix(name, defs.PERF_DATA_FILE_PREFIX)
}
