package socket

import (
	"context"
	"encoding/binary"
	"encoding/json"
	"fmt"
	"jrasp-daemon/defs"
	"jrasp-daemon/userconfig"
	"jrasp-daemon/utils"
	"jrasp-daemon/zlog"
	"net"
	"net/url"
	"strconv"
	"strings"
	"sync"
	"time"
)

// 管理连接的conn
// todo 需要优化
var SocketConns sync.Map

type AgentConn struct {
	processId         string
	conn              net.Conn
	IsRegister        bool              // 与Java进程匹配成功
	AgentCommandChan  chan Package      // 发给agent的命令
	AgentResponseChan chan AgentMessage // 接受agent的返回
	ctx               context.Context
	AgentMessageChan  chan string
	ReadBuf           *utils.Buffer
}

type AgentMessage struct {
	Msg        string `json:"msg"`
	HostName   string `json:"hostName"`
	Level      string `json:"level"`
	Topic      string `json:"topic"`
	LogId      int    `json:"logId"`
	Pid        int    `json:"pid"`
	ProcessId  string `json:"processId"`
	Thread     string `json:"thread"`
	StackTrace string `json:"stackTrace"`
	Ts         string `json:"ts"`
}

type AgentCommandRespose struct {
	Code    int    `json:"code"`
	Message string `json:"message"`
	Type    string `json:"type"`
}

// 接收消息
func (a *AgentConn) WriteConn() {
	for {
		select {
		case _ = <-a.ctx.Done():
			return
		case p := <-a.AgentCommandChan:
			err := p.Pack(a.conn)
			if err != nil {
				continue
			}
			ticker := time.NewTicker(time.Second * time.Duration(60))
			// 阻塞等待
			for {
				select {
				case response := <-a.AgentResponseChan:
					kvArray := strings.Split(response.Msg, ";")
					var kvMap = make(map[string]string, 8)
					for i := 0; i < len(kvArray); i++ {
						kv := strings.Split(kvArray[i], "=")
						if len(kv) == 2 {
							kvMap[kv[0]] = kv[1]
						}
					}
					code := kvMap["code"]
					message := kvMap["message"]
					commandType := kvMap["type"]
					if code == "200" {
						zlog.Debugf(defs.COMMAND_RESPONSE, "command exec success", "code: %s, command: %s, response: %s",
							code, commandType, message)
					} else {
						zlog.Errorf(defs.COMMAND_RESPONSE, "command exec error", "code: %s, command: %s, response: %s",
							code, commandType, message)
					}
					// 超时计数器关闭
					ticker.Stop()
					goto END
				case <-ticker.C:
					// 超时退出
					zlog.Errorf(defs.COMMAND_RESPONSE, "wait command response timeout", "command: %d", p.Type)
					goto END
				}
			}
		END:
		}
	}
}

// 接收消息
func (a *AgentConn) ReadConn() {
	for {
		// type
		d, e := a.ReadBuf.ReadNext(1)
		if e != nil {
			return
		}
		t := d[0]

		// body size
		d, e = a.ReadBuf.ReadNext(4)
		if e != nil {
			return
		}
		l := binary.BigEndian.Uint32(d)

		// body
		d, e = a.ReadBuf.ReadNext(int(l))
		if e != nil {
			return
		}

		packet := new(Package)
		packet.Body = d
		packet.BodySize = l
		packet.Type = t

		// 日志
		var agentMessage AgentMessage
		err := json.Unmarshal(packet.Body, &agentMessage)
		if err != nil {
			return
		}

		// 命令返回的消息，写入单独的高优先通道
		if packet.Type == COMMAND_RESPONSE {
			a.AgentResponseChan <- agentMessage
		}

		// 处理java agent 产生的日志
		if !a.IsRegister {
			processId := agentMessage.ProcessId
			if processId != "" {
				a.processId = processId
				SocketConns.Store(processId, *a)
				a.IsRegister = true
				zlog.Infof(defs.AGENT_CONN_REGISTER, "store socket conn",
					"processId: %s, remote addr: %s", processId, a.GetConn().RemoteAddr().String())
			}
		}
		fmt.Println(string(packet.Body))
		// 写入日志传输通道
		a.AgentMessageChan <- string(packet.Body)
	}
}

func (a *AgentConn) registerConn(p *Package) {
	var agentMessage AgentMessage
	err := json.Unmarshal(p.Body, &agentMessage)
	if err != nil {
		return
	}
	processId := agentMessage.ProcessId
	if processId != "" {
		a.processId = processId
		SocketConns.Store(processId, *a)
		a.IsRegister = true
		zlog.Infof(defs.AGENT_CONN_REGISTER, "store socket conn",
			"processId: %s, remote addr: %s", processId, a.GetConn().RemoteAddr().String())
	}
}

func (a *AgentConn) GetConn() net.Conn {
	return a.conn
}

// SendExit  agent退出卸载
func (a *AgentConn) SendUnInstallCommand() {
	a.SendAgentMessge(AGENT_UNINSTALL, "")
}

// 更新模块参数
func (a *AgentConn) UpdateModuleConfig(m []userconfig.ModuleConfig) {
	configs := ConvertModule2String(m)
	for _, v := range configs {
		a.SendAgentMessge(MODULE_CONFIG, v)
	}
}

// SendFlushCommand 刷新模块jar包
func (a *AgentConn) SendFlushCommand(isForceFlush string) {
	a.SendAgentMessge(MODULE_FLUSH, isForceFlush)
}

// UpdateAgentConfig 更新agent全局配置
func (d *AgentConn) UpdateAgentConfig(m map[string]interface{}) {
	message := ConvertMap2String(m)
	d.SendAgentMessge(AGENT_CONFIG, message)
}

// SendMessge2Conn 往指定连接发送消息
func (a *AgentConn) SendAgentMessge(t byte, message string) {
	pkg := &Package{
		Type:      t,
		BodySize:  uint32(len(message)),
		Body:      []byte((message)),
	}
	a.AgentCommandChan <- *pkg
}

// 更改字符串形式，便于java agent 解析
// map[string]interface{} 转换成 k1=v1;k2=v1,v2,v3;
func ConvertMap2String(m map[string]interface{}) string {
	var s = ""
	for k, v := range m {
		if k != "" {
			s += k + "=" + ConvertValue2String(v) + ";"
		}
	}
	return s
}

func ConvertModule2String(m []userconfig.ModuleConfig) []string {
	var messages = []string{}
	for _, v := range m {
		if v.Parameters != nil && len(v.Parameters) > 0 {
			var param = v.ModuleName + ":"
			for key, value := range v.Parameters {
				param += key + "=" + ConvertValueArray2String(value) + ";"
			}
			messages = append(messages, param)
			zlog.Debugf(defs.UPDATE_MODULE_PARAMETERS, "[update module config]", "param: %s", param)
		}
	}
	return messages
}

func ConvertValueArray2String(values []interface{}) string {
	var paramSlice []string
	for _, value := range values {
		s := ConvertValue2String(value)
		paramSlice = append(paramSlice, s)
	}
	return strings.Join(paramSlice, ",")
}

func ConvertValue2String(value interface{}) string {
	switch value.(type) {
	case string:
		return url.PathEscape(value.(string))
	case bool:
		return strconv.FormatBool(value.(bool))
	case []string:
		return strings.Join(value.([]string), ",")
	case int8, int16, int32, int, int64, uint8, uint16, uint32, uint, uint64:
		return strconv.Itoa(value.(int))
	case float64:
		strV := strconv.FormatFloat(value.(float64), 'f', -1, 64)
		return strV
	case float32:
		ft := value.(float32)
		strV := strconv.FormatFloat(float64(ft), 'f', -1, 64)
		return strV
	default:
		zlog.Errorf(defs.UPDATE_MODULE_PARAMETERS, "Unsupported data type", "type: %T, param: %s", value, value)
	}
	return ""
}
