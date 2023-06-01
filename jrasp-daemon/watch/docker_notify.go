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
			if strings.Index(err.Error(), "no such file or directory") < 0 {
				zlog.Errorf(defs.WATCH_DOCKER, "获取进程命令出错", "err:%v", err)
				continue
			}
		}
		if strings.HasSuffix(name, "stop") {
			// 如果不是容器环境则跳过
			if !checkIsContainer(process.Pid) {
				continue
			}
			if !checkExistRASP(process.Pid) {
				tomcat := "/usr/local/tomcat"
				containerId := getContainerIdByPid(int(process.Pid))
				zlog.Debugf(defs.WATCH_DOCKER, "发现容器未安装RASP", "container id: %v", containerId)
				//copySelfToContainer(w.env.InstallDir, tomcat)
				copySelfToContainerNew(w.env.InstallDir, tomcat, containerId)
			}
		}
	}
}

func checkExistRASP(pid int32) bool {
	rasp := fmt.Sprintf("/proc/%v/root/opt/jrasp", pid)
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

func copySelfToContainerNew(installDir string, tomcatPath string, containerId string) {
	docker, err := utils.NewDocker()
	if err != nil {
		zlog.Errorf(defs.WATCH_DOCKER, "复制RASP至容器内出错", "初始化docker错误, err:%v", err)
		return
	}
	defer docker.Close()
	raspDir := "/opt/jrasp"
	err = docker.Copy(installDir, raspDir, containerId)
	if err != nil {
		zlog.Errorf(defs.WATCH_DOCKER, "复制RASP至容器内出错", "复制文件错误, err:%v", err)
		return
	}
	// 删除logs目录
	cmd := "rm -rf logs run/* tmp/* pid"
	resp, err := docker.Exec(containerId, raspDir, strings.Split(cmd, " "), []string{}, false)
	if err != nil {
		zlog.Errorf(defs.WATCH_DOCKER, "复制RASP至容器内出错", "执行命令%v出错, err:%v", cmd, err)
		return
	}
	if strings.Index(resp, "禁止操作") > 0 {
		cmd = "busybox rm -rf logs run/* tmp/* pid"
		_, err = docker.Exec(containerId, raspDir, strings.Split(cmd, " "), []string{}, false)
		if err != nil {
			zlog.Errorf(defs.WATCH_DOCKER, "复制RASP至容器内出错", "执行命令%v出错, err:%v", cmd, err)
			return
		}
	}
	// 新建tomcat日志目录
	cmd = fmt.Sprintf("mkdir %v/logs/jrasp", tomcatPath)
	_, err = docker.Exec(containerId, raspDir, strings.Split(cmd, " "), []string{}, false)
	if err != nil {
		zlog.Errorf(defs.WATCH_DOCKER, "复制RASP至容器内出错", "执行命令%v出错, err:%v", cmd, err)
		return
	}
	cmd = fmt.Sprintf("ln -s %v logs", filepath.Join(tomcatPath, "logs", "jrasp"))
	_, err = docker.Exec(containerId, raspDir, strings.Split(cmd, " "), []string{}, false)
	if err != nil {
		zlog.Errorf(defs.WATCH_DOCKER, "复制RASP至容器内出错", "执行命令%v出错, err:%v", cmd, err)
		return
	}
	// 启动RASP
	cmd = "./service.sh"
	_, err = docker.Exec(containerId, filepath.Join(raspDir, "bin"), strings.Split(cmd, " "), []string{}, true)
	if err != nil {
		zlog.Errorf(defs.WATCH_DOCKER, "复制RASP至容器内出错", "执行命令%v出错, err:%v", cmd, err)
		return
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
	cmd, err := parent.Cmdline()
	if err != nil {
		return nil, err
	}
	if strings.Index(cmd, "namespace moby") > 0 {
		return parent, nil
	} else {
		return getTopProcess(parent)
	}
}
