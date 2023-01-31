package socket

import (
	"bufio"
	"bytes"
	"encoding/binary"
	"fmt"
	"jrasp-daemon/defs"
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

type SocketClient struct {
	Ip   string
	Port string
}

func NewSocketClient(ip, port string) *SocketClient {
	return &SocketClient{
		Port: port,
		Ip:   ip,
	}
}

// Send message json string
func (this *SocketClient) Send(message string, t byte) {
	conn, err := net.DialTimeout("tcp", fmt.Sprintf("%s:%s", this.Ip, this.Port), 10*time.Second)
	if err != nil {
		return
	}
	pack := &Package{
		Magic:     magicBytes,
		Version:   PROTOCOL_VERSION,
		Type:      t,
		BodySize:  int32(len(message)),
		TimeStamp: time.Now().Unix(),
		Signature: emptySignature,
		Body:      []byte((message)),
	}

	pack.Pack(conn)
	scanner := bufio.NewScanner(conn)
	scanner.Split(func(data []byte, atEOF bool) (advance int, token []byte, err error) {
		if !atEOF && string(data[0:3]) == string(magicBytes[:]) {
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

	// server只收到一个
	for scanner.Scan() {
		scannedPack := new(Package)
		scannedPack.Unpack(bytes.NewReader(scanner.Bytes()))
		this.Handler(scannedPack)
	}
}

// TODO
func (this *SocketClient) Handler(p *Package) {
	switch p.Type {
	case ERROR:
		zlog.Errorf(defs.UPDATE_MODULE_PARAMETERS, "update parameters", "result:%s", string(p.Body))
	default:
		zlog.Debugf(defs.UPDATE_MODULE_PARAMETERS, "update parameters", "result:%s", string(p.Body))
	}
}

func (this *SocketClient) SendExit() {
	this.Send("", UNINSTALL)
}

func (this *SocketClient) SendParameters(message string) {
	this.Send(message, UPDATE)
}

func (this *SocketClient) SendFlush(isForceFlush string) {
	this.Send(isForceFlush, FLUSH)
}

func (this *SocketClient) UpdateAgentConfig(message string) {
	this.Send(message, CONFIG)
}
