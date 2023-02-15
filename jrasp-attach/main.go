package main

import (
	"flag"
	"fmt"
	"io/ioutil"
	"jrasp-attach/attach"
	"jrasp-attach/common"
	"jrasp-attach/config"
	"jrasp-attach/socket"
	"log"
	"net/url"
	"os"
	"os/exec"
	"path/filepath"
	"runtime"
	"strconv"
	"strings"
)

var (
	version bool
	pid     int
	stop    bool
	list    bool
	data    string
	unload  string
	cf      string
)

func init() {
	flag.BoolVar(&version, "v", false, "usage for attach version. example: ./attach -v")
	flag.IntVar(&pid, "p", -1, "usage for attach java pid and update params. example: ./attach -p <pid>")
	flag.BoolVar(&stop, "s", false, "usage for stop agent. example: ./attach -p <pid> -s")
	flag.BoolVar(&list, "l", false, "usage for list transform class. example: ./attach -p <pid> -l")
	flag.StringVar(&unload, "u", "", "usage for unload module. example: ./attach -p <pid> -u rce-hook")
}

func main() {
	flag.Parse()
	if version {
		log.Printf(" inject version: %s", common.VERSION)
		return
	}

	if pid > 0 {
		// 配置初始化
		config, err := config.InitConfig()
		if err != nil {
			log.Fatalf("config init error %s", err.Error())
			return
		}

		// get jrasp home
		raspHome, err := getRaspHome()
		if err != nil {
			log.Fatalln("get jrasp home error:%v", err)
			return
		}
		// attach
		log.Printf("attach java process,pid: %d", pid)

		// jattach pid load instrument false jrasp-launcher-1.1.1.jar
		// 读取版本号
		version, err := readVsersion(raspHome)
		if err != nil || version == "" {
			version = common.VERSION
		}

		// logPath 转换成绝对路径
		logPathAbs, err := filepath.Abs(config.LogPath)
		if err != nil {
			log.Fatalf("logPath: %s, logPathAbs: %s", config.LogPath, logPathAbs)
		}

		cmd := exec.Command(
			filepath.Join(raspHome, "bin", getJattachExe()), fmt.Sprintf("%d", pid),
			"load", "instrument", "false",
			fmt.Sprintf("%s=%s", filepath.Join(raspHome, "lib", common.AGENT_NAME+"-"+version+".jar"),
				fmt.Sprintf("raspHome=%s;coreVersion=%s;key=%s;logPath=%s;", raspHome, version, common.BuildDecryptKey, logPathAbs)),
		)

		if err := cmd.Start(); err != nil {
			log.Fatalf("cmd.Start error:%v", err)
			return
		}

		if err := cmd.Wait(); err != nil {
			log.Fatalf("cmd.Wait error:%v", err)
			// release 仅在 wait 调用失败时使用
			err = cmd.Process.Release()
			if err != nil {
				log.Fatalf("cmd.Process.Release error:%v", err)
				return
			}
			return
		}

		// 判断socket文件是否存在
		success := attach.Check(pid)
		if !success {
			return
		}

		ip, port, flag := ReadTokenFile(raspHome, pid)
		if flag == false {
			return
		}
		log.Printf("command socket init success: [%s:%s]", ip, port)

		// attach 之后建立socket
		sock := socket.NewSocketClient(ip, port)

		sock.Send("false", socket.FLUSH)
		// 更新全局参数&模块参数
		UpdateParameter(sock, config)

		if stop == false && list == false && data == "" && unload == "" && cf == "" {
			log.Println("attach jvm success")
			return
		} else if stop == true {
			// stop
			log.Print("stop agent")
			sock.SendExit()
			return
		} else if list == true {
			// list
			log.Println("list transform class:")
			sock.List()
			return
		} else if data != "" {
			// update data
			log.Println("update module data," + data)
			sock.SendParameters(data)
			return
		} else if unload != "" {
			// unload module
			log.Println("unload module: " + unload)
			sock.UnloadModule(unload)
			return
		} else if cf != "" {
			// update config
			log.Println("update agent config: " + cf)
			sock.UpdateAgentConfig(cf)
			return
		}
	}
}

func UpdateParameter(client *socket.SocketClient, c *config.Config) {
	// 更新全局参数
	var config = ""
	for k, v := range c.AgentConfigs {
		if k != "" {
			config += k + "=" + join2(v) + ";"
		}
	}
	client.UpdateAgentConfig(config)

	// 更新模块参数
	for _, v := range c.ModuleConfigs {
		if v.Parameters != nil && len(v.Parameters) > 0 {
			var param = v.ModuleName + ":"
			for key, value := range v.Parameters {
				param += key + "=" + join(value) + ";"
			}
			client.SendParameters(param)
		}
	}
}

func join(params []interface{}) string {
	var paramSlice []string
	for _, param := range params {
		switch param.(type) {
		case string:
			paramSlice = append(paramSlice, url.PathEscape(param.(string))) // 使用url编码防止存在特殊字符
		case bool:
			paramSlice = append(paramSlice, strconv.FormatBool(param.(bool)))
		case []string:
			paramSlice = append(paramSlice, strings.Join(param.([]string), ","))
		case int8, int16, int32, int, int64, uint8, uint16, uint32, uint, uint64:
			paramSlice = append(paramSlice, strconv.Itoa(param.(int)))
		case float64:
			strV := strconv.FormatFloat(param.(float64), 'f', -1, 64)
			paramSlice = append(paramSlice, strV)
		case float32:
			ft := param.(float32)
			strV := strconv.FormatFloat(float64(ft), 'f', -1, 64)
			paramSlice = append(paramSlice, strV)
		// TODO 其他类型
		default:
			log.Fatalf("[data type not support]", "param: %s", param)
		}
	}
	return strings.Join(paramSlice, ",")
}

func join2(param interface{}) string {
	var paramSlice []string
	switch param.(type) {
	case string:
		paramSlice = append(paramSlice, url.PathEscape(param.(string))) // 使用url编码防止存在特殊字符
	case bool:
		paramSlice = append(paramSlice, strconv.FormatBool(param.(bool)))
	case []string:
		paramSlice = append(paramSlice, strings.Join(param.([]string), ","))
	case int8, int16, int32, int, int64, uint8, uint16, uint32, uint, uint64:
		paramSlice = append(paramSlice, strconv.Itoa(param.(int)))
	case float64:
		strV := strconv.FormatFloat(param.(float64), 'f', -1, 64)
		paramSlice = append(paramSlice, strV)
	case float32:
		ft := param.(float32)
		strV := strconv.FormatFloat(float64(ft), 'f', -1, 64)
		paramSlice = append(paramSlice, strV)
	// TODO 其他类型
	default:
		log.Fatalf("[data type not support]", "param: %s", param)
	}
	return strings.Join(paramSlice, ",")
}

func getRaspHome() (string, error) {
	runDir, err := filepath.Abs(filepath.Dir(os.Args[0]))
	raspHome, err := filepath.Abs(filepath.Dir(runDir))
	if err != nil {
		return "", err
	}
	return raspHome, nil
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

func ReadTokenFile(raspHome string, pid int) (string, string, bool) {
	tokenFilePath := filepath.Join(raspHome, "run", fmt.Sprintf("%d", pid), ".jrasp.token")
	exist, err := PathExists(tokenFilePath)
	if err != nil {
		log.Fatalf("check token file[%s],error:%v", tokenFilePath, err)
		return "", "", false
	}
	// 文件存在
	if exist {
		ip, port, err := splitContent(tokenFilePath)
		if err != nil {
			return "", "", false
		}
		return ip, port, true
	} else {
		log.Fatalf("attach token file[%s] not exist", tokenFilePath)
		return "", "", false
	}
}

func splitContent(tokenFilePath string) (string, string, error) {
	fileContent, err := ioutil.ReadFile(tokenFilePath)
	if err != nil {
		log.Fatalf("read attach token file[%s],error:%v", tokenFilePath, err)
		return "", "", err
	}
	fileContentStr := string(fileContent)
	fileContentStr = strings.Replace(fileContentStr, " ", "", -1) // 字符串去掉"\n"和"空格"
	fileContentStr = strings.Replace(fileContentStr, "\n", "", -1)
	tokenArray := strings.Split(fileContentStr, ";")
	//log.Printf("token file content:%s", fileContentStr)
	if len(tokenArray) == 3 {
		return tokenArray[1], tokenArray[2], nil
	}
	log.Fatalf("[Fix it] token file content bad,tokenFilePath:%s,fileContentStr:%s", tokenFilePath, fileContentStr)
	return "", "", err
}

func PathExists(path string) (bool, error) {
	_, err := os.Stat(path)
	if err == nil {
		return true, nil
	}
	if os.IsNotExist(err) {
		return false, nil
	}
	return false, err
}

func readVsersion(raspHome string) (string, error) {
	versionFile := filepath.Join(raspHome, "VERSION.txt")
	exist, err := PathExists(versionFile)
	if err != nil {
		log.Fatalf("read version file[%s],error:%v", versionFile, err)
		return "", err
	}
	// 文件存在
	if exist {
		fileContent, err := ioutil.ReadFile(versionFile)
		if err != nil {
			log.Fatalf("read version file[%s],error:%v", versionFile, err)
			return "", err
		}
		versionStr := string(fileContent)
		// 去除换行与空格
		versionStr = strings.Replace(versionStr, " ", "", -1)
		versionStr = strings.Replace(versionStr, "\n", "", -1)
		versionStr = strings.Replace(versionStr, "\r", "", -1)
		return versionStr, nil
	} else {
		log.Fatalf("version token file[%s] not exist", versionFile)
		return "", nil
	}

}
