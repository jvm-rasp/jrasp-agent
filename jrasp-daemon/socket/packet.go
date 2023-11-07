package socket

import (
	"encoding/binary"
	"fmt"
	"io"
)

var MagicBytes = [3]byte{88, 77, 68}
var EmptySignature = make([]byte, 128)

const PROTOCOL_VERSION byte = 101

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

// Package 145
type Package struct {
	Type     byte   // 1 byte 包类型
	BodySize uint32 // 4 byte 数据部分长度,仅指 body 长度
	Body     []byte // 数据部分长度
}

// Pack 编码
func (p *Package) Pack(writer io.Writer) error {
	var err error
	err = binary.Write(writer, binary.BigEndian, &p.Type)
	err = binary.Write(writer, binary.BigEndian, &p.BodySize)
	err = binary.Write(writer, binary.BigEndian, &p.Body)
	return err
}

func (p *Package) Unpack(reader io.Reader) error {
	var err error
	err = binary.Read(reader, binary.BigEndian, &p.Type)
	err = binary.Read(reader, binary.BigEndian, &p.BodySize)
	p.Body = make([]byte, p.BodySize)
	err = binary.Read(reader, binary.BigEndian, &p.Body)
	return err
}

func (p *Package) String() string {
	return fmt.Sprintf("type:%d bodySize:%d body:%s", p.Type, p.BodySize, p.Body)
}
