package socket

import (
	"bufio"
	"bytes"
	"encoding/binary"
	"encoding/json"
	"jrasp-daemon/defs"
	"jrasp-daemon/zlog"
	"net"
	"sync"
	"time"
)

const (
	// --------------------- agent ---------------------
	AGENT_CONFIG    byte = 0x01 // 更新agent参数
	AGENT_UNINSTALL byte = 0x02 // 卸载agent
	AGENT_INFO      byte = 0x03 // 获取agent信息
	AGENT_LOG       byte = 0x04 // agent日志

	// --------------------- module ---------------------
	MODULE_UNINSTALL byte = 0x20 // 卸载module
	MODULE_CONFIG    byte = 0x21 //  更新module参数
	MODULE_FLUSH     byte = 0x22 // 刷新module
	MODULE_ACTIVE    byte = 0x23 // 激活模块
	MODULE_FROZEN    byte = 0x24 // 冻结模块

	// --------------------- other ---------------------
	COMMAND_RESPONSE byte = 0x30 // 命令的返回
)

// 接收所有的 agent 的消息
var AgentMessageChan = make(chan string, 2000)

// 管理连接的conn
var SocketConns sync.Map

type AgentConn struct {
	processId  string
	conn       net.Conn
	IsRegister bool // 与Java进程匹配成功
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

// 接收消息
func (a *AgentConn) Write() {
	for {
		select {}
	}
}

// 接收消息
func (a *AgentConn) Read() {
	scanner := initScanner(a.conn)
	// 读取消息
	for scanner.Scan() {
		p := new(Package)
		err := p.Unpack(bytes.NewReader(scanner.Bytes()))
		if err != nil {
			_ = a.conn.Close()
			return
		}
		// 处理java agent 产生的日志
		if !a.IsRegister {
			a.registerConn(p)
		}
		AgentMessageChan <- string(p.Body)
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
func (a *AgentConn) UpdateModuleConfig(message string) {
	a.SendAgentMessge(MODULE_CONFIG, message)
}

// SendFlushCommand 刷新模块jar包
func (a *AgentConn) SendFlushCommand(isForceFlush string) {
	a.SendAgentMessge(MODULE_FLUSH, isForceFlush)
}

// UpdateAgentConfig 更新agent全局配置
func (d *AgentConn) UpdateAgentConfig(message string) {
	d.SendAgentMessge(AGENT_CONFIG, message)
}

// SendMessge2Conn 往指定连接发送消息
func (a *AgentConn) SendAgentMessge(t byte, message string) {
	pkg := &Package{
		Magic:     MagicBytes,
		Version:   PROTOCOL_VERSION,
		Type:      t,
		BodySize:  int32(len(message)),
		TimeStamp: time.Now().Unix(),
		Signature: EmptySignature,
		Body:      []byte((message)),
	}
	pkg.Pack(a.conn)
}

func initScanner(conn net.Conn) *bufio.Scanner {
	scanner := bufio.NewScanner(conn)
	// 分割
	scanner.Split(func(data []byte, atEOF bool) (advance int, token []byte, err error) {
		if !atEOF && string(data[0:3]) == string(MagicBytes[:]) {
			if len(data) > 5 {
				length := int32(0)
				binary.Read(bytes.NewReader(data[5:9]), binary.BigEndian, &length)
				if int(length)+145 <= len(data) {
					return 145 + int(length), data[:145+int(length)], nil
				}
			}
		}
		return
	})
	return scanner
}
