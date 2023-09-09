package update

import (
	"fmt"
	"github.com/shirou/gopsutil/v3/process"
	"io/fs"
	"io/ioutil"
	"jrasp-daemon/defs"
	"jrasp-daemon/environ"
	"jrasp-daemon/java_process"
	"jrasp-daemon/userconfig"
	"jrasp-daemon/utils"
	"jrasp-daemon/zlog"
	"os"
	"path"
	"path/filepath"
	"runtime"
	"strconv"
	"strings"
)

const MODULE_KEY = "module"

const AGENT_KEY = "lib"

type Update struct {
	cfg       *userconfig.Config
	env       *environ.Environ
	moduleDir string
	agentDir  string
}

func NewUpdateClient(cfg *userconfig.Config, env *environ.Environ) *Update {
	return &Update{
		cfg:       cfg,
		env:       env,
		moduleDir: filepath.Join(env.InstallDir, MODULE_KEY),
		agentDir:  filepath.Join(env.InstallDir, AGENT_KEY),
	}
}

// DownLoad
// url 文件下载链接
// filePath 下载的绝对路径
func (this *Update) DownLoad(url, filePath string) error {
	err := utils.DownLoadFile(url, filePath)
	if err != nil {
		return err
	}
	return nil
}

// UpdateDaemonFile 更新守护进程
func (this *Update) UpdateDaemonFile() {
	// 判断bin目录下是否存在daemon.del文件，如果存在则删除
	exist := utils.PathExists(this.env.BinFileName + ".del")
	if exist {
		err := os.Remove(this.env.BinFileName + ".del")
		if err != nil {
			zlog.Errorf(defs.DOWNLOAD, "[BUG] delete daemon.del error", "err: %v", err)
		}
	}

	cfgMd5, err := this.cfg.RaspBinConfigs.GetMD5ByName(path.Join(runtime.GOOS, runtime.GOARCH, this.env.BinFileName))
	if err == nil && cfgMd5 != this.env.BinFileHash {
		newFilePath := filepath.Join(this.env.InstallDir, "bin", this.cfg.RaspBinConfigs.FileName)
		defer this.clean(newFilePath)
		err = this.DownLoad(this.cfg.RaspBinConfigs.DownloadUrl, newFilePath)
		if err != nil {
			zlog.Errorf(defs.DOWNLOAD, fmt.Sprintf("download %v file error", this.cfg.RaspBinConfigs.FileName), "err:%v", err)
			return
		}
		newHash, err := utils.GetFileMd5(newFilePath)
		if err != nil {
			zlog.Errorf(defs.DOWNLOAD, fmt.Sprintf("get file %v hash err", this.cfg.RaspBinConfigs.FileName), "err: %v", err)
			return
		}
		// 校验下载文件的hash
		if newHash == this.cfg.RaspBinConfigs.Md5 {
			//data, err := utils.ReadFileFromZip(newFilePath, this.env.BinFileName)
			data, err := utils.ReadFileFromZipByPath(newFilePath, path.Join(runtime.GOOS, runtime.GOARCH, this.env.BinFileName))
			if err != nil {
				zlog.Errorf(defs.DOWNLOAD, "[BUG] read zip file data error", "err: %v", err)
				return
			}
			// 兼容windows环境下直接写daemon导致文件被占用的错误，应该先把运行中的daemon重命名为临时文件，再将新的文件覆盖
			err = os.Rename(this.env.BinFileName, this.env.BinFileName+".del")
			if err != nil {
				zlog.Errorf(defs.DOWNLOAD, "[BUG] rename daemon to daemon.del error", "err: %v", err)
				return
			}
			err = os.WriteFile(this.env.BinFileName, data, 0777)
			if err != nil {
				zlog.Errorf(defs.DOWNLOAD, "[BUG] replace daemon error", "err: %v", err)
			} else {
				zlog.Infof(defs.DOWNLOAD, "update jrasp-daemon file success", "write jrasp-daemon file success,daemon process will exit...")
				this.clean(newFilePath)
				// 再次check
				success := utils.PathExists(this.env.BinFileName)
				if success {
					os.Exit(0) // 进程退出
				}
			}
		} else {
			zlog.Errorf(defs.DOWNLOAD, "[BUG] check new file hash err", "newFileHash:%s, configHash:%s", newHash, this.cfg.RaspBinConfigs.Md5)
		}
	}
}

// DownLoadAgentFiles 模块升级
func (this *Update) DownLoadAgentFiles() {
	// 获取磁盘上的agent 包
	files, err := ioutil.ReadDir(this.agentDir)
	if err != nil {
		zlog.Errorf(defs.DOWNLOAD, "list disk agent file failed", "err:%v", err)
		return
	}

	// 1.先检测磁盘上的全部插件的名称、hash
	fileHashMap := this.calAgentHash(files)
	// 2.判断是否需要重新下载
	isDown := false
	for _, item := range this.cfg.RaspLibConfigs.ItemsInfo {
		for k, v := range fileHashMap {
			if k == item.FileName && v != item.Md5 {
				isDown = true
				break
			}
		}
		if isDown {
			break
		}
	}
	// 3.下载
	if isDown {
		this.uninstallAgent()
		this.downLoadAgent(fileHashMap)
	}
}

// DownLoadModuleFiles 模块升级
func (this *Update) DownLoadModuleFiles() {
	if !this.cfg.ModuleAutoUpdate {
		zlog.Debugf(defs.DOWNLOAD, "moduleAutoUpdate is disabled", "close module update from remote")
		return
	}
	// 获取磁盘上的插件
	files, err := ioutil.ReadDir(this.moduleDir)
	if err != nil {
		zlog.Errorf(defs.DOWNLOAD, "list disk module file failed", "err:%v", err)
		return
	}

	// 1.先检测磁盘上的全部插件的名称、hash
	fileHashMap := this.calDiskHash(files)

	// 2.下载
	this.downLoad(fileHashMap)
}

func (this *Update) uninstallAgent() {
	dirEntry, _ := os.ReadDir(filepath.Join(this.env.InstallDir, "run"))
	if len(dirEntry) > 0 {
		zlog.Infof(defs.DOWNLOAD, "starting uninstall agent", "update success will exit...")
	}
	for _, item := range dirEntry {
		if item.IsDir() {
			pid, _ := strconv.Atoi(item.Name())
			proc, err := process.NewProcess(int32(pid))
			if err != nil {
				zlog.Infof(defs.DOWNLOAD, fmt.Sprintf("process %v not exist will ignore", pid), "err:%v", err)
				continue
			}
			javaProcess := java_process.NewJavaProcess(proc, this.cfg, this.env)
			// 获取进程的注入状态
			javaProcess.GetAndMarkStatus()
			// 如果进程已注入则立即进行卸载
			if javaProcess.SuccessInject() {
				javaProcess.ExitInjectImmediately()
			}
		}
	}
}

func (this *Update) downLoadAgent(fileHashMap map[string]string) {
	// 1.先下载zip包
	newFilePath := filepath.Join(this.env.InstallDir, "lib", this.cfg.RaspLibConfigs.FileName)
	defer this.clean(newFilePath)
	err := this.DownLoad(this.cfg.RaspLibConfigs.DownloadUrl, newFilePath)
	if err != nil {
		zlog.Errorf(defs.DOWNLOAD, fmt.Sprintf("download %v file error", this.cfg.RaspLibConfigs.FileName), "err:%v", err)
		return
	}
	newHash, err := utils.GetFileMd5(newFilePath)
	if err != nil {
		zlog.Errorf(defs.DOWNLOAD, fmt.Sprintf("get file %v hash err", this.cfg.RaspLibConfigs.FileName), "err: %v", err)
		return
	}
	// 2.校验下载文件的hash
	if newHash == this.cfg.RaspLibConfigs.Md5 {
		// 4.释放zip文件
		for _, item := range this.cfg.RaspLibConfigs.ItemsInfo {
			data, err := utils.ReadFileFromZip(newFilePath, item.FileName)
			if err != nil {
				zlog.Errorf(defs.DOWNLOAD, "[BUG] read zip file data error", "err: %v", err)
				continue
			}
			err = os.WriteFile(filepath.Join(this.env.InstallDir, "lib", item.FileName), data, 0777)
			if err != nil {
				zlog.Errorf(defs.DOWNLOAD, fmt.Sprintf("[BUG] write file %v error", item.FileName), "err: %v", err)
			}
		}
		this.clean(newFilePath)
		os.Exit(0) // 进程退出
	} else {
		zlog.Errorf(defs.DOWNLOAD, "[BUG] check new file hash err", "newFileHash:%s,configHash:%s", newHash, this.cfg.RaspBinConfigs.Md5)
	}
}

func (this *Update) downLoad(fileHashMap map[string]string) {
	for _, m := range this.cfg.ModuleConfigs {
		hash, ok := fileHashMap[m.ModuleName]
		if !ok || hash != m.Md5 {
			// 下载文件
			tmpFileName := filepath.Join(this.moduleDir, m.ModuleName+".tmp")
			err := utils.DownLoadFile(m.DownLoadURL, tmpFileName) // module.tmp
			if err != nil {
				zlog.Errorf(defs.DOWNLOAD, "[BUG]download file failed", "tmpFileName:%s,err:%v", tmpFileName, err)
				_ = os.Remove(tmpFileName)
				continue
			}
			// hash 校验
			tmpFileHash, err := utils.GetFileMd5(tmpFileName)
			if err != nil {
				zlog.Errorf(defs.DOWNLOAD, "[BUG]cal file hash failed", "filePath:%s,err:%v", tmpFileName, err)
				_ = os.Remove(tmpFileName)
				continue
			}
			// 校验成功，修改名称
			if tmpFileHash == strings.ToLower(m.Md5) {
				zlog.Infof(defs.DOWNLOAD, "check file hash success", "tmpfilePath:%s,tmpFileHash:%v", tmpFileName, tmpFileHash)
				newFilePath := filepath.Join(this.moduleDir, m.ModuleName+".jar")
				// 覆盖旧插件
				err := os.Rename(tmpFileName, newFilePath)
				if err != nil {
					zlog.Errorf(defs.DOWNLOAD, "[BUG]rename file name failed", "tmpFileName:%s,newFilePath:%s,err:%v", tmpFileName, newFilePath, err)
					_ = os.Remove(tmpFileName)
					continue
				}
			} else {
				// 检验失败打印日志，并删除临时文件
				zlog.Errorf(defs.DOWNLOAD, "[BUG]check file hash failed", "tmpfilePath:%s,tmpFileHash:%v,configHash:%v", tmpFileName, tmpFileHash, m.Md5)
				err := os.Remove(tmpFileName)
				if err != nil {
					zlog.Errorf(defs.DOWNLOAD, "[BUG]delete broken file err", "file path: %s", tmpFileName)
					return
				}
			}
		}
	}
}

func (this *Update) calDiskHash(files []fs.FileInfo) map[string]string {
	var fileHashMap = make(map[string]string)
	for _, file := range files {
		name := file.Name()
		fileAbsPath := filepath.Join(this.moduleDir, name)
		ext := path.Ext(name) // .jar
		nameWithOutExt := strings.TrimSuffix(name, ext)

		var notExisted = true
		for i := 0; i < len(this.cfg.ModuleConfigs); i++ {
			moduleConfig := this.cfg.ModuleConfigs[i]
			if moduleConfig.ModuleName == nameWithOutExt {
				notExisted = false
			}
		}

		// 不在配置列表中的插件，执行删除
		if notExisted {
			_ = os.Remove(fileAbsPath)
			zlog.Infof(defs.DOWNLOAD, "module is not in config, will delete", "name:%s", name)
			continue
		}

		// 计算hash
		if !file.IsDir() && strings.HasSuffix(name, ".jar") {
			hash, err := utils.GetFileMd5(fileAbsPath)
			if err != nil {
				zlog.Errorf(defs.DOWNLOAD, "[Fix it] calc file hash error", "file:%s,err:%v", name, err)
			} else {
				fileHashMap[nameWithOutExt] = hash // bug fix  名称不带后缀
			}
		}
	}
	return fileHashMap
}

func (this *Update) calAgentHash(files []fs.FileInfo) map[string]string {
	var fileHashMap = make(map[string]string)
	for _, file := range files {
		name := file.Name()
		fileAbsPath := filepath.Join(this.agentDir, name)
		// 计算hash
		if !file.IsDir() && strings.HasSuffix(name, ".jar") {
			hash, err := utils.GetFileMd5(fileAbsPath)
			if err != nil {
				zlog.Errorf(defs.DOWNLOAD, "[Fix it] calc file hash error", "file:%s,err:%v", name, err)
			} else {
				fileHashMap[name] = hash // bug fix  名称不带后缀
			}
		}
	}
	return fileHashMap
}

func (this *Update) clean(filePath string) {
	exists := utils.PathExists(filePath)
	if exists {
		err := os.Remove(filePath)
		if err != nil {
			zlog.Errorf(defs.DOWNLOAD, "[BUG] delete file err", "err:%s", err)
		}
	}
}

func (this *Update) CleanPidFiles() {
	dirEntry, _ := os.ReadDir(filepath.Join(this.env.InstallDir, "run"))
	if len(dirEntry) > 0 {
		zlog.Infof(defs.DOWNLOAD, "clean pid", "starting clean invalid pid")
	}
	for _, item := range dirEntry {
		if item.IsDir() {
			pid, _ := strconv.Atoi(item.Name())
			_, err := process.NewProcess(int32(pid))
			if err == process.ErrorProcessNotRunning {
				err = os.RemoveAll(filepath.Join(this.env.InstallDir, "run", item.Name()))
				if err == nil {
					zlog.Infof(defs.DOWNLOAD, "clean invalid pid file success", "pid:%s", item.Name())
				} else {
					zlog.Errorf(defs.DOWNLOAD, "[BUG] clean invalid pid file err", "pid file: %s, err:%s", item.Name(), err)
				}
			}
		}
	}
}
