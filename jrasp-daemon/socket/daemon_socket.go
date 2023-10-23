package socket

import (
	"fmt"
	"jrasp-daemon/defs"
	"jrasp-daemon/zlog"
	"net"
)

// DaemonSocket daemon 与agent 通信的socket
// daemon 端作为 server端，java agent 作为 client 端
type DaemonSocket struct {
	Port int
}

func NewDaemonSocket(port int) *DaemonSocket {
	return &DaemonSocket{
		Port: port,
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
			continue
		}

		agentConn := &AgentConn{
			conn:              conn,
			IsRegister:        false,
			AgentCommandChan:  make(chan Package, 10),
			AgentResponseChan: make(chan AgentMessage, 10),
		}

		zlog.Infof(defs.AGENT_CONN_REGISTER, "a new conn register to daemon socket",
			"remote addr: %s", conn.RemoteAddr().String())

		go agentConn.ReadConn()
		go agentConn.WriteConn()
	}
}
