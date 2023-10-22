package java_process

import (
	"errors"
	"fmt"
	"jrasp-daemon/defs"
	"jrasp-daemon/utils"
	"jrasp-daemon/zlog"
	"os"
	"os/exec"
	"path/filepath"
	"runtime"
	"strings"
)

// 与Java进程通过attach方式通信

const (
	ATTACH_JAVA_PID        = ".java_pid%d"
	LINUX_PROC_ROOT        = "/proc/%d/root"
	libDir          string = "lib"
	moduleDir       string = "module"
)

// 执行attach
func (jp *JavaProcess) Attach() error {
	// 执行attach并检查java_pid文件
	err := jp.execCmd()
	if err != nil {
		return err
	}

	// 判断socket文件是否存在
	success := jp.CheckAttachStatus()
	if !success {
		return errors.New("attach socket file not found!")
	}

	return nil
}

func (jp *JavaProcess) execCmd() error {
	zlog.Debugf(defs.ATTACH_DEFAULT, "[Attach]", "attach to jvm[%d] start...", jp.JavaPid)
	// logPath 转换成绝对路径
	logPathAbs, err := filepath.Abs(jp.Cfg.LogPath)
	if err != nil {
		zlog.Warnf(defs.ATTACH_DEFAULT, "[Attach]", "logPath: %s, logPathAbs: %s", jp.Cfg.LogPath, logPathAbs)
	}
	// 通过attach 传递给目标jvm的参数
	// 必需要配置为daemonIp
	agentArgs := fmt.Sprintf("raspHome=%s;coreVersion=%s;key=%s;logPath=%s;daemonIp=%s;daemonPort=%s;",
		jp.env.InstallDir, jp.Cfg.Version, jp.env.BuildDecryptKey, logPathAbs, jp.env.Ip, jp.Cfg.DaemonPort)
	// jattach pid load instrument false jrasp-launcher.jar
	cmd := exec.Command(
		filepath.Join(jp.env.InstallDir, "bin", getJattachExe()),
		fmt.Sprintf("%d", jp.JavaPid),
		"load", "instrument", "false", fmt.Sprintf("%s=%s", filepath.Join(jp.env.InstallDir, "lib", "jrasp-launcher-"+jp.Cfg.Version+".jar"), agentArgs),
	)

	zlog.Debugf(defs.ATTACH_DEFAULT, "[Attach]", "cmdArgs:%s", cmd.Args)
	// 权限切换在 jattach 里面做了，直接在root权限下执行命令就行
	if err := cmd.Start(); err != nil {
		zlog.Warnf(defs.ATTACH_DEFAULT, "[Attach]", "cmd.Start error:%v", err)
		return err
	}

	if err := cmd.Wait(); err != nil {
		zlog.Warnf(defs.ATTACH_DEFAULT, "[Attach]", "cmd.Wait error:%v", err)
		// release 仅在 wait 调用失败时使用
		err = cmd.Process.Release()
		if err != nil {
			zlog.Warnf(defs.ATTACH_DEFAULT, "[Attach]", "cmd.Process.Release error:%v", err)
			return err
		}
		return errors.New("cmd.Wait error!")
	}

	return nil
}

func (jp *JavaProcess) GetInContainerPidByHostPid() (string, error) {
	containerTmpPath := filepath.Join(fmt.Sprintf(LINUX_PROC_ROOT, jp.JavaPid), "tmp")
	tmpFiles, err := os.ReadDir(containerTmpPath)
	if err != nil {
		zlog.Warnf(defs.ATTACH_READ_TOKEN, "ReadDir tmpFiles error", "err:%v", err)
		return "", err
	}

	for _, tmpFile := range tmpFiles {
		if tmpFile.IsDir() && strings.HasPrefix(tmpFile.Name(), defs.PERF_DATA_FILE_PREFIX) {
			// 一个用户下的全部进程pid
			userPidFiles := filepath.Join(containerTmpPath, tmpFile.Name())
			pidFiles, err := os.ReadDir(userPidFiles)
			if err != nil {
				continue
			}
			// TODO 存在多个pid的场景如何解决
			if len(pidFiles) > 0 {
				return pidFiles[0].Name(), nil
			}
		}
	}

	return "", errors.New("not found pid file int /tmp")
}

func (jp *JavaProcess) CopyJar2Proc() error {
	containerRootPath := filepath.Join("/proc", fmt.Sprintf("%d", jp.JavaPid), "root")
	libPath := filepath.Join(jp.env.InstallDir, libDir)
	containerLibPath := filepath.Join(containerRootPath, libPath)
	err := utils.CreateDir(containerLibPath)
	if err != nil {
		return err
	}
	files, err := os.ReadDir(libPath)
	if err != nil {
		return err
	}

	// 复制lib下文件到容器中
	// 容器目录下没有对应版本文件，复制
	// 不可以覆盖
	for _, fileInfo := range files {
		fromFile := filepath.Join(libPath, fileInfo.Name())
		toFile := filepath.Join(containerLibPath, fileInfo.Name())
		if strings.Contains(fileInfo.Name(), jp.Cfg.Version) && !utils.PathExists(toFile) {
			err = utils.CopyFile(fromFile, toFile, os.ModePerm)
			if err != nil {
				return err
			}
		}
	}

	// module
	modulePath := filepath.Join(jp.env.InstallDir, moduleDir)
	containerModulePath := filepath.Join(containerRootPath, modulePath)
	err = utils.CreateDir(containerModulePath)
	if err != nil {
		return err
	}
	files, err = os.ReadDir(modulePath)
	if err != nil {
		return err
	}

	// 复制覆盖即可
	for _, fileInfo := range files {
		if strings.Contains(fileInfo.Name(), jp.Cfg.Version) && !utils.PathExists(filepath.Join(containerModulePath, fileInfo.Name())) {
			err = utils.CopyFile(filepath.Join(modulePath, fileInfo.Name()), filepath.Join(containerModulePath, fileInfo.Name()), os.ModePerm)
			if err != nil {
				return err
			}
		}
	}
	return nil
}

func (jp *JavaProcess) CheckAttachStatus() bool {
	var sockfile string
	if jp.IsContainer {
		sockfile = filepath.Join(fmt.Sprintf(LINUX_PROC_ROOT, jp.JavaPid), os.TempDir(), fmt.Sprintf(ATTACH_JAVA_PID, jp.JavaPid))
	} else {
		sockfile = filepath.Join(os.TempDir(), fmt.Sprintf(ATTACH_JAVA_PID, jp.JavaPid))
	}
	return utils.PathExists(sockfile)
}

func getJattachExe() string {
	switch runtime.GOOS {
	case "darwin":
		return "jattach_darwin"
	case "linux":
		return "jattach_linux"
	case "windows":
		return "jattach.exe"
	default:
		return "UNKNOWN"
	}
}
