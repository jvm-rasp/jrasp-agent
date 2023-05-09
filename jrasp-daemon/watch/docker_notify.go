package watch

import (
	"fmt"
	cp "github.com/otiai10/copy"
	"github.com/shirou/gopsutil/v3/process"
	"jrasp-daemon/defs"
	"jrasp-daemon/utils"
	"jrasp-daemon/zlog"
	"os"
	"path/filepath"
	"regexp"
	"strings"
)

func (w *Watch) ContainerTimer() {
	for {
		select {
		case _ = <-w.ctx.Done():
			return
		case _, ok := <-w.ContainerTicker.C:
			if !ok {
				return
			}
			w.NotifyContainer()
		}
	}
}

func (w *Watch) NotifyContainer() {
	processList, err := process.Processes()
	if err != nil {
		zlog.Errorf(defs.WATCH_DOCKER, "获取进程列表出错", "err:%v", err)
		return
	}
	for _, process := range processList {
		name, err := process.Cmdline()
		if err != nil {
			zlog.Errorf(defs.WATCH_DOCKER, "获取进程命令出错", "err:%v", err)
			continue
		}
		if strings.HasSuffix(name, "stop") {
			// 如果不是容器环境则跳过
			if !checkIsContainer(process.Pid) {
				continue
			}
			execPath, err := process.Cwd()
			if err != nil {
				zlog.Errorf(defs.WATCH_DOCKER, "获取进程路径出错", "err:%v", err)
				continue
			}
			tomcat := filepath.Join(fmt.Sprintf("/proc/%v", process.Pid), "root", execPath, "../")
			if !checkExistRASP(tomcat) {
				containerId := getContainerIdByPid(int(process.Pid))
				zlog.Warnf(defs.WATCH_DOCKER, "发现容器未安装RASP", "container id: %v", containerId)
				copySelfToContainer(w.env.InstallDir, tomcat)
			}
		}
	}
}

func checkExistRASP(tomcatPath string) bool {
	rasp := filepath.Join(tomcatPath, "jrasp")
	exist, err := utils.PathExists(rasp)
	if err != nil || !exist {
		return false
	}
	return true
}

func copySelfToContainer(installDir string, tomcatPath string) {
	rasp := filepath.Join(tomcatPath, "jrasp")
	opt := cp.Options{
		Skip: func(info os.FileInfo, src, dest string) (bool, error) {
			if src == filepath.Join(installDir, "pid") {
				return true, nil
			}

			if src == filepath.Join(installDir, "run") {
				return true, nil
			}

			if src == filepath.Join(installDir, "tmp") {
				return true, nil
			}

			return false, nil
		},
	}
	err := cp.Copy(installDir, rasp, opt)
	if err != nil {
		zlog.Errorf(defs.WATCH_DOCKER, "复制RASP至容器内出错", "err:%v", err)
	}
}

func checkIsContainer(pid int32) bool {
	dockerenv := filepath.Join(fmt.Sprintf("/proc/%v", pid), "root", ".dockerenv")
	exist, err := utils.PathExists(dockerenv)
	if err != nil || !exist {
		return false
	}
	return true
}

func getContainerIdByPid(pid int) string {
	proc, err := getProcessInfoByPid(pid)
	if err != nil {
		zlog.Errorf(defs.WATCH_DOCKER, "获取进程信息出错", "err:%v", err)
	}
	topProc, err := getTopProcess(proc)
	if err != nil {
		zlog.Errorf(defs.WATCH_DOCKER, "获取父进程信息出错", "err:%v", err)
	}
	cmd, err := topProc.Cmdline()
	if err != nil {
		zlog.Errorf(defs.WATCH_DOCKER, "获取进程cmd出错", "err:%v", err)
	}
	var re = regexp.MustCompile(`(?m)[a-z0-9]{64}`)
	containerId := re.FindString(cmd)
	return containerId
}

func getProcessInfoByPid(pid int) (*process.Process, error) {
	proc, err := process.NewProcess(int32(pid))
	if err != nil {
		return nil, err
	}
	return proc, nil
}

func getTopProcess(proc *process.Process) (*process.Process, error) {
	parent, err := proc.Parent()
	if err != nil {
		return nil, err
	}
	if parent.Pid == 1 {
		return proc, nil
	} else {
		return getTopProcess(parent)
	}
}
