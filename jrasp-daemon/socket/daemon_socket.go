package socket

import (
	"fmt"
	"jrasp-daemon/defs"
	"jrasp-daemon/zlog"
	"net"
)

/**
 * daemon 与agent 通信的socket
 * daemon 端作为 server端，java agent 作为 client 端
 */

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
			fmt.Println("Error accepting:", err.Error())
			continue
		}

		agentConn := &AgentConn{
			conn:             conn,
			IsRegister:       false,
			AgentCommandChan: make(chan Package, 10),
		}

		zlog.Infof(defs.AGENT_CONN_REGISTER, "a new conn register to daemon socket",
			"remote addr: %s", conn.RemoteAddr().String())

		go agentConn.Read()
		go agentConn.Write()
	}
}
