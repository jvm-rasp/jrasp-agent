package new_socket

import (
	"bufio"
	"bytes"
	"encoding/binary"
	"fmt"
	"jrasp-daemon/socket"
	"net"
)

/**
 * daemon 与agent 通信的socket
 * daemon 端作为 server端，java agent 作为 client 端
 */

var AgentMessageChan = make(chan string, 2000)

type DaemonSocket struct {
	Port   int
	Client map[string]net.Conn // agent 注册的连接
}

func NewServerSocket(port int) *DaemonSocket {
	return &DaemonSocket{
		Port:   port,
		Client: make(map[string]net.Conn, 10),
	}
}

func (s *DaemonSocket) Start() {
	listener, err := net.Listen("tcp", fmt.Sprintf(":%d", s.Port))
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
		go s.handleAgentConnection(conn)
	}
}

// 日志处理
func (s *DaemonSocket) handleAgentConnection(conn net.Conn) {
	if conn != nil {
		addr := conn.RemoteAddr()
		if addr != nil {
			addr.Network()
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
		HandlerAgentMessage(scannedPack)
	}
}

func HandlerAgentMessage(p *socket.Package) {
	// java agent 的日志写入缓存队列，准备发往server
	fmt.Println("回传日志:" + string(p.Body))
	AgentMessageChan <- string(p.Body)
}
