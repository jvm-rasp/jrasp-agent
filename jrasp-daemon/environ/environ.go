package environ

import (
	"errors"
	"jrasp-daemon/defs"
	"jrasp-daemon/utils"
	"net"
	"os"
	"path/filepath"
	"runtime"

	"github.com/shirou/gopsutil/v3/cpu"
	"github.com/shirou/gopsutil/v3/disk"
	"github.com/shirou/gopsutil/v3/mem"
)

var (
	BuildDateTime   = ""
	BuildGitBranch  = ""
	BuildGitCommit  = ""
	BuildDecryptKey = ""
)

const GB = 1024 * 1024 * 1024

type Environ struct {
	InstallDir  string `json:"installDir"`  // 安装目录
	HostName    string `json:"hostName"`    // 主机/容器名称
	Ip          string `json:"ip"`          // ipAddress
	OsType      string `json:"osType"`      // 操作系统类型
	BinFileHash string `json:"binFileHash"` // 磁盘可执行文件的md5值

	// 系统信息
	TotalMem  uint64 `json:"totalMem"`  // 总内存 GB
	CpuCounts int    `json:"cpuCounts"` // logic cpu cores
	FreeDisk  uint64 `json:"freeDisk"`  // 可用磁盘空间 GB
	Version   string `json:"version"`   // rasp 版本

	// 编译信息
	BuildDateTime  string `json:"buildDateTime"`
	BuildGitBranch string `json:"buildGitBranch"`
	BuildGitCommit string `json:"buildGitCommit"`

	// 密钥版本, 密钥最好是固化到代码中，golang二进制文件不太容易反编译，当需要更新密钥时，更新整个daemon即可
	// 必须为16位
	// 安全原因：不支持配置
	BuildDecryptKey string `json:"buildDecryptKey"`

	PidFile string `json:"pidFile"`
}

func NewEnviron() (*Environ, error) {
	// 可执行文件路径
	execPath, err := filepath.Abs(os.Args[0])
	if err != nil {
		return nil, err
	}

	// install dir
	execDir := filepath.Dir(filepath.Dir(execPath))

	// md5 值
	md5Str, err := utils.GetFileMd5(execPath)
	if err != nil {
		return nil, err
	}
	// ip
	ipAddress, err := getExternalIP()
	if err != nil {
		return nil, err
	}

	// mem
	memInfo, _ := mem.VirtualMemory()

	// disk
	FreeDisk, err := GetInstallDisk(execDir)

	// cpu cnt
	cpuCounts, err := cpu.Counts(true)

	env := &Environ{
		HostName:        getHostname(),
		Ip:              ipAddress,
		InstallDir:      execDir,
		OsType:          runtime.GOOS,
		BinFileHash:     md5Str,
		TotalMem:        memInfo.Total / GB,
		CpuCounts:       cpuCounts,
		FreeDisk:        FreeDisk,
		Version:         defs.JRASP_DAEMON_VERSION,
		BuildGitBranch:  BuildGitBranch,
		BuildDateTime:   BuildDateTime,
		BuildDecryptKey: BuildDecryptKey,
		BuildGitCommit:  BuildGitCommit,
		PidFile:         filepath.Join(execDir, defs.DAEMON_PID_FILE),
	}
	return env, nil
}

func getHostname() string {
	hostname, _ := os.Hostname()
	return hostname
}

func getExternalIP() (string, error) {
	ifaces, err := net.Interfaces()
	if err != nil {
		return "", err
	}
	for _, iface := range ifaces {
		if iface.Flags&net.FlagUp == 0 {
			continue // interface down
		}
		if iface.Flags&net.FlagLoopback != 0 {
			continue // loopback interface
		}
		addrs, err := iface.Addrs()
		if err != nil {
			return "", err
		}
		for _, addr := range addrs {
			var ip net.IP
			switch v := addr.(type) {
			case *net.IPNet:
				ip = v.IP
			case *net.IPAddr:
				ip = v.IP
			}
			if ip == nil || ip.IsLoopback() {
				continue
			}
			ip = ip.To4()
			if ip == nil {
				continue // not an ipv4 address
			}
			return ip.String(), nil
		}
	}
	return "", errors.New("are you connected to the network?")
}

func GetInstallDisk(path string) (free uint64, err error) {
	state, err := disk.Usage(path)
	if err != nil {
		return 0, err
	}
	free = state.Free / GB
	return free, nil
}
