package new_socket

import (
	"bufio"
	"bytes"
	"encoding/binary"
	"fmt"
	"jrasp-daemon/defs"
	"jrasp-daemon/socket"
	"jrasp-daemon/zlog"
	"net"
	"time"
)

const INFO byte = 0x01
const UNINSTALL byte = 0x02
const FLUSH byte = 0x03
const ERROR byte = 0x04
const UPDATE byte = 0x05
const UNLOAD byte = 0x06
const ACTIVE byte = 0x07
const FROZEN byte = 0x08
const CONFIG byte = 0x09
const SEARCH_SERVER byte = 0x10
const UPDATE_SERVER byte = 0x11

/**
 * daemon 与agent 通信的socket
 * daemon 端作为 server端，java agent 作为 client 端
 */

var AgentMessageChan = make(chan string, 2000)

type DaemonSocket struct {
	Port   int
	Client map[string]ConnInfo // agent 注册的连接
}

type ConnInfo struct {
	net.Conn
	isUpdateConfig bool
}

func NewServerSocket(port int) *DaemonSocket {
	return &DaemonSocket{
		Port:   port,
		Client: make(map[string]ConnInfo, 10), //
	}
}

func (d *DaemonSocket) Start() {
	listener, err := net.Listen("tcp", fmt.Sprintf(":%d", d.Port))
	if err != nil {
		fmt.Println("Error listening:", err.Error())
		return
	}
	defer listener.Close()

	for {
		conn, err := listener.Accept()
		if err != nil {
			fmt.Println("Error accepting:", err.Error())
			continue
		}
		// 处理连接
		go d.handleAgentConnection(conn)
	}
}

// 日志处理
func (d *DaemonSocket) handleAgentConnection(conn net.Conn) {
	if conn != nil {
		addr := conn.RemoteAddr()
		if addr != nil {
			address := addr.String()
			conInfo := &ConnInfo{
				Conn:           conn,
				isUpdateConfig: false,
			}
			d.Client[address] = *conInfo
		}
	}

	scanner := bufio.NewScanner(conn)
	// 分割
	scanner.Split(func(data []byte, atEOF bool) (advance int, token []byte, err error) {
		if !atEOF && string(data[0:3]) == string(socket.MagicBytes[:]) {
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

	for scanner.Scan() {
		scannedPack := new(socket.Package)
		err := scannedPack.Unpack(bytes.NewReader(scanner.Bytes()))
		if err != nil {
			return
		}
		// 处理java agent 产生的日志
		handleAgentMessage(scannedPack)
	}
}

func handleAgentMessage(p *socket.Package) {
	// java agent 的日志写入缓存队列，准备发往server
	fmt.Println("回传日志:" + string(p.Body))
	AgentMessageChan <- string(p.Body)
}

// SendExit  agent退出卸载
func (d *DaemonSocket) SendExit() {
	d.SendGroup("", UNINSTALL)
}

// SendParameters 更新模块参数
func (d *DaemonSocket) SendParameters(message string) {
	d.SendGroup(message, UPDATE)
}

// SendFlushCommand 刷新模块jar包
func (d *DaemonSocket) SendFlushCommand(isForceFlush string) {
	d.SendGroup(isForceFlush, FLUSH)
}

// UpdateAgentConfig 更新agent全局配置
func (d *DaemonSocket) UpdateAgentConfig(message string) {
	d.SendGroup(message, CONFIG)
}

// SendGroup 广播消息
func (d *DaemonSocket) SendGroup(message string, t byte) {
	for _, v := range d.Client {
		d.SendClientMessge(v, message, t)
	}
}

// SendMessge2Conn 往指定连接发送消息
func (d *DaemonSocket) SendClientMessge(conn net.Conn, message string, t byte) {
	pack := &Package{
		Magic:     MagicBytes,
		Version:   PROTOCOL_VERSION,
		Type:      t,
		BodySize:  int32(len(message)),
		TimeStamp: time.Now().Unix(),
		Signature: EmptySignature,
		Body:      []byte((message)),
	}

	pack.Pack(conn)
	scanner := bufio.NewScanner(conn)
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

	for scanner.Scan() {
		scannedPack := new(Package)
		scannedPack.Unpack(bytes.NewReader(scanner.Bytes()))
		d.Handler(scannedPack)
	}
}

// Handler 处理返回的消息
func (this *DaemonSocket) Handler(p *Package) {
	switch p.Type {
	case ERROR:
		zlog.Errorf(defs.UPDATE_MODULE_PARAMETERS, "update parameters", "result:%s", string(p.Body))
	default:
		zlog.Debugf(defs.UPDATE_MODULE_PARAMETERS, "update parameters", "result:%s", string(p.Body))
	}
}
