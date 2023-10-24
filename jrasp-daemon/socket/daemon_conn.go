package socket

import (
	"bufio"
	"bytes"
	"context"
	"encoding/binary"
	"encoding/json"
	"fmt"
	"jrasp-daemon/defs"
	"jrasp-daemon/zlog"
	"net"
	"strings"
	"sync"
	"time"
)

// 接收所有的 agent 的消息
var AgentMessageChan = make(chan string, 2000)

// 管理连接的conn
var SocketConns sync.Map

type AgentConn struct {
	processId         string
	conn              net.Conn
	IsRegister        bool              // 与Java进程匹配成功
	AgentCommandChan  chan Package      // 发给agent的命令
	AgentResponseChan chan AgentMessage // 接受agent的返回
	ctx               context.Context
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
						zlog.Infof(defs.COMMAND_RESPONSE, "command exec success", "code: %d, command: %d, response: %s",
							code, commandType, message)
					} else {
						zlog.Errorf(defs.COMMAND_RESPONSE, "command exec error", "code: %d, command: %d, response: %s",
							code, commandType, message)
					}
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
	scanner := initScanner(a.conn)
	// 读取消息
	for scanner.Scan() {
		// 消息体
		p := new(Package)
		err := p.Unpack(bytes.NewReader(scanner.Bytes()))
		if err != nil {
			// _ = a.conn.Close()
			return
		}

		// 日志
		var agentMessage AgentMessage
		err = json.Unmarshal(p.Body, &agentMessage)
		if err != nil {
			fmt.Printf("json: %s\n", string(p.Body))
			return
		}

		// 命令返回的消息，写入单独的高优先通道
		if p.Type == COMMAND_RESPONSE {
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

		// 写入日志传输通道
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
	a.AgentCommandChan <- *pkg
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
