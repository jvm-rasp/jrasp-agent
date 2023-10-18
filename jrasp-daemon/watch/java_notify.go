package watch

import (
	"io/ioutil"
	"jrasp-daemon/defs"
	"jrasp-daemon/zlog"
	"log"
	"os"
	"path/filepath"
	"strconv"
	"strings"

	"github.com/fsnotify/fsnotify"
	"github.com/shirou/gopsutil/v3/process"
)

// PERF_DATA_FILE_PREFIX The file name prefix for JVM PerfData shared memory files.
const PERF_DATA_FILE_PREFIX = "hsperfdata_"

func (w *Watch) NotifyJavaProcess() {
	pathWatcher, err := fsnotify.NewWatcher()
	if err != nil {
		zlog.Fatalf(defs.WATCH_DEFAULT, "fsnotify.NewWatcher(1)", "err:%v", err)
		return
	}
	defer func() {
		if pathWatcher != nil {
			_ = pathWatcher.Close()
		}
	}()

	pidWatcher, err := fsnotify.NewWatcher()
	if err != nil {
		zlog.Fatalf(defs.WATCH_DEFAULT, "fsnotify.NewWatcher(2)", "err:%v", err)
		return
	}

	defer func() {
		if pidWatcher != nil {
			_ = pidWatcher.Close()
		}
	}()

	go func() {
		for {
			select {
			case _ = <-w.ctx.Done():
				return
			case event, ok := <-pidWatcher.Events:
				if !ok {
					return
				}
				if event.Op&fsnotify.Create == fsnotify.Create {
					pid := getPidFromPath(event.Name)
					if pid > 0 {
						p, err := process.NewProcess(int32(pid))
						if err != nil {
							continue
						}
						w.JavaProcessHandlerChan <- p
					}
				}
				if event.Op&fsnotify.Remove == fsnotify.Remove {
					pid := getPidFromPath(event.Name)
					if pid > 0 {
						w.removeExitedJavaProcess(int32(pid))
					}
				}
			case err, ok := <-pathWatcher.Errors:
				if err != nil {
					zlog.Errorf(defs.WATCH_DEFAULT, "pathWatcher.Errors", "err:%v", err)
				}
				if !ok {
					return
				}
			}
		}
	}()

	dir, err := ioutil.ReadDir(os.TempDir())
	if err != nil {
		return
	}
	zlog.Infof(defs.WATCH_DEFAULT, "starting watch temp dir", "temp dir is: %v", os.TempDir())
	PathSep := string(os.PathSeparator)
	for _, fi := range dir {
		if fi.IsDir() {
			if isHsPerfDataDir(fi.Name()) {
				dirPath := os.TempDir() + PathSep + fi.Name()
				pidWatcher.Add(dirPath)
				w.appendPidToChan(dirPath)
			}
		}
	}

	go func() {
		for {
			select {
			case _ = <-w.ctx.Done():
				return
			case event, ok := <-pathWatcher.Events:
				if !ok {
					return
				}
				_, fileName := filepath.Split(event.Name)
				if event.Op&fsnotify.Create == fsnotify.Create {
					if isHsPerfDataDir(fileName) {
						pidWatcher.Add(event.Name)
					}
				}
				if event.Op&fsnotify.Remove == fsnotify.Remove {
					if isHsPerfDataDir(fileName) {
						pidWatcher.Remove(event.Name)
					}
				}
			case err, ok := <-pathWatcher.Errors:
				if !ok {
					return
				}
				log.Println("error:", err)
			}
		}
	}()

	err = pathWatcher.Add(os.TempDir())
	if err != nil {
		zlog.Fatalf(defs.WATCH_DEFAULT, "pathWatcher.Add tmp dir", "err:%v", err)
	}

	// block
	<-defs.Sig
}

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
		if err == nil {
			p, err := process.NewProcess(int32(pid))
			if err != nil {
				continue
			}
			w.JavaProcessHandlerChan <- p
		}
	}
	return nil
}

func getPidFromPath(filePath string) int {
	_, fileName := filepath.Split(filePath)
	pid, err := strconv.Atoi(fileName)
	if err != nil {
		return -1
	}
	return pid
}

func isHsPerfDataDir(name string) bool {
	return strings.HasPrefix(name, PERF_DATA_FILE_PREFIX)
}

// 定时扫描全量进程
func (w *Watch) ProcessScan() {
	for {
		select {
		case _, ok := <-w.JavaProcessScanTicker.C:
			if !ok {
				continue
			}
			processes, err := process.Processes()
			if err != nil {
				continue
			}

			// 选择Java进程
			// 容器或者非容器进程特征相同
			for _, processe := range processes {
				exe, err := processe.Exe()
				if err != nil {
					if exe != "" && strings.HasSuffix(exe, "java") {
						w.JavaProcessHandlerChan <- processe
					}
				}
			}
		}
	}
}
