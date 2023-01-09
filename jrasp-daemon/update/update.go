package update

import (
	"io/fs"
	"io/ioutil"
	"jrasp-daemon/defs"
	"jrasp-daemon/environ"
	"jrasp-daemon/userconfig"
	"jrasp-daemon/utils"
	"jrasp-daemon/zlog"
	"os"
	"path"
	"path/filepath"
	"strings"
)

const MODULE_KEY = "module"

type Update struct {
	cfg       *userconfig.Config
	env       *environ.Environ
	moduleDir string
}

func NewUpdateClient(cfg *userconfig.Config, env *environ.Environ) *Update {
	return &Update{
		cfg:       cfg,
		env:       env,
		moduleDir: filepath.Join(env.InstallDir, MODULE_KEY),
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
	// 配置中可执行文件hash不为空，并且与env中可执行文件hash不相同
	if this.cfg.BinFileHash != "" && this.cfg.BinFileHash != this.env.BinFileHash {
		newFilePath := filepath.Join(this.env.InstallDir, "bin", "jrasp-daemon.tmp")
		err := this.DownLoad(this.cfg.BinFileUrl, newFilePath)
		if err != nil {
			zlog.Errorf(defs.DOWNLOAD, "download  jrasp-daemon file", "err:%v,down load url:%s", err, this.cfg.BinFileUrl)
			return
		}
		newHash, err := utils.GetFileMd5(newFilePath)
		if err != nil {
			zlog.Errorf(defs.DOWNLOAD, "download  jrasp-daemon file", "get file hash err:%v", err)
			return
		}
		// 校验下载文件的hash
		if newHash == this.cfg.BinFileHash {
			this.replace()
		} else {
			zlog.Errorf(defs.DOWNLOAD, "[BUG]check new file hash err", "newFileHash:%s,configHash:%s", newHash, this.cfg.BinFileHash)
			err := os.Remove(newFilePath)
			if err != nil {
				zlog.Errorf(defs.DOWNLOAD, "[BUG]delete broken file err", "newFileHash:%s", newHash)
				return
			}
		}
	} else {
		zlog.Infof(defs.DOWNLOAD, "no need to update jrasp-daemon",
			"config.binFileHash:%s,disk.binFileHash:%s", this.cfg.BinFileHash, this.env.BinFileHash)
	}
}

// replace 文件rename
func (this *Update) replace() {
	// 增加可执行权限
	err := os.Chmod("jrasp-daemon.tmp", 0700)
	if err != nil {
		zlog.Infof(defs.DOWNLOAD, "chmod +x jrasp-demon.tmp", "err:%v", err)
	}
	err = os.Rename("jrasp-daemon.tmp", "jrasp-daemon")
	if err == nil {
		zlog.Infof(defs.DOWNLOAD, "update jrasp-daemon file success", "rename jrasp-daemon file success,daemon process will exit...")
		// 再次check
		success, _ := utils.PathExists("jrasp-daemon")
		if success {
			os.Exit(0) // 进程退出
		}
	} else {
		zlog.Errorf(defs.DOWNLOAD, "[BUG]rename jrasp-daemon file error", "jrasp-daemon file will delete")
		_ = os.Remove("jrasp-daemon.tmp")
	}
}

// DownLoadModuleFiles 模块升级
// TODO 疑似bug
func (this *Update) DownLoadModuleFiles() {
	if !this.cfg.ModuleAutoUpdate {
		zlog.Infof(defs.DOWNLOAD, "moduleAutoUpdate is disabled", "close module update from remote")
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

func (this *Update) downLoad(fileHashMap map[string]string) {
	for _, m := range this.cfg.ModuleConfigMap {
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
			if tmpFileHash == m.Md5 {
				zlog.Infof(defs.DOWNLOAD, "check file hash success", "tmpfilePath:%s,tmpFileHash:%v", tmpFileName, tmpFileHash)
				newFilePath := filepath.Join(this.moduleDir, m.ModuleName+".jar")
				// 覆盖旧插件
				// todo bug：存在无法rename的场景
				err := os.Rename(tmpFileName, newFilePath)
				if err != nil {
					zlog.Errorf(defs.DOWNLOAD, "[BUG]rename file name failed", "tmpFileName:%s,newFilePath:%s,err:%v", tmpFileName, newFilePath, err)
					_ = os.Remove(tmpFileName)
					continue
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
		_, ok := this.cfg.ModuleConfigMap[nameWithOutExt]
		if !ok {
			// 不在配置列表中的插件，执行删除
			_ = os.Remove(fileAbsPath)
			zlog.Warnf(defs.DOWNLOAD, "module is not in config,wile delete", "name:%s", name)
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
