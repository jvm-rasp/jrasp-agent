package remote

import (
	"bytes"
	"encoding/json"
	"errors"
	"fmt"
	"github.com/gookit/goutil/fsutil"
	"github.com/spf13/viper"
	"gopkg.in/yaml.v3"
	"jrasp-daemon/defs"
	"jrasp-daemon/environ"
	"jrasp-daemon/socket"
	"jrasp-daemon/userconfig"
	"jrasp-daemon/zlog"
	"net"
	url2 "net/url"
	"os"
	"path"
	"path/filepath"
	"strings"
	"time"
)

type UDPClient struct {
	env         *environ.Environ
	cfg         *userconfig.Config
	conn        *net.UDPConn
	udpAddr     *net.UDPAddr
	err         error
	IsSearching bool
}

func NewUDPClient(cfg *userconfig.Config, env *environ.Environ) *UDPClient {
	udpAddr, err := net.ResolveUDPAddr("udp4", "0.0.0.0:8080")
	if err != nil {
		zlog.Errorf(defs.MDNS_SEARCH, "bind udp port error", "err: %v", err.Error())
		return nil
	}
	return &UDPClient{
		env:         env,
		cfg:         cfg,
		udpAddr:     udpAddr,
		IsSearching: false,
	}
}

func (c *UDPClient) MonitorConnectState() {
	for {
		if !c.env.IsConnectServer && c.IsSearching == false {
			go c.SearchServer()
		}
		time.Sleep(time.Second * 5)
	}
}

func (c *UDPClient) SearchServer() {
	zlog.Infof(defs.MDNS_SEARCH, "starting search server", "starting search server")
	c.IsSearching = true
	defer c.Close()
	go c.listenUDP()
	for {
		time.Sleep(time.Second * 5)
		if !c.env.IsConnectServer {
			zlog.Infof(defs.MDNS_SEARCH, "broadcast packet", "starting send search packet")
			dstAddr := net.UDPAddr{
				IP:   net.IPv4(255, 255, 255, 255),
				Port: 8080,
			}
			conn, err := net.DialUDP("udp", nil, &dstAddr)
			sendMessage := "where is jrasp-admin"
			pack := &socket.Package{
				Magic:     socket.MagicBytes,
				Version:   socket.PROTOCOL_VERSION,
				Type:      socket.SEARCH_SERVER,
				BodySize:  int32(len(sendMessage)),
				TimeStamp: time.Now().Unix(),
				Signature: socket.EmptySignature,
				Body:      []byte((sendMessage)),
			}
			sendBuf := bytes.NewBuffer(nil)
			err = pack.Pack(sendBuf)
			if err != nil {
				zlog.Errorf(defs.MDNS_SEARCH, "pack buf error", "err: %v", err.Error())
				continue
			}
			_, err = conn.Write(sendBuf.Bytes())
			if err != nil {
				zlog.Errorf(defs.MDNS_SEARCH, "send buf error", "err: %v", err.Error())
			}
			err = conn.Close()
			if err != nil {
				zlog.Errorf(defs.MDNS_SEARCH, "close connect error", "err: %v", err.Error())
			}
		} else {
			break
		}
	}
	zlog.Infof(defs.MDNS_SEARCH, "stop search server", "stop search server")
	c.IsSearching = false
}

func (c *UDPClient) listenUDP() {
	c.conn, c.err = net.ListenUDP("udp", c.udpAddr)
	if c.err != nil {
		if strings.Index(c.err.Error(), "bind: address already in use") > 0 {
			zlog.Infof(defs.MDNS_SEARCH, "udp port is in used", "wait for 10 seconds")
			time.Sleep(time.Second * 10)
			c.listenUDP()
		} else {
			zlog.Errorf(defs.MDNS_SEARCH, "listen udp port error", "err: %v", c.err.Error())
			return
		}
	}
	zlog.Infof(defs.MDNS_SEARCH, "bind udp port success", "starting wait for client message")
	for {
		if !c.env.IsConnectServer {
			buf := make([]byte, 2048)
			length, addr, err := c.conn.ReadFromUDP(buf[:]) // 读取数据，返回值依次为读取数据长度、远端地址、错误信息 // 读取操作会阻塞直至有数据可读取
			if err != nil {
				if strings.Index(err.Error(), "use of closed network connection") < 0 {
					zlog.Errorf(defs.MDNS_SEARCH, "read udp data error", "err: %v", err.Error())
				}
				continue
			}
			data := buf[:length]
			scannedPack := new(socket.Package)
			err = scannedPack.Unpack(bytes.NewReader(data))
			if err != nil {
				zlog.Errorf(defs.MDNS_SEARCH, "unpack udp data error", "err: %v", err.Error())
				continue
			}
			if scannedPack.Type == socket.UPDATE_SERVER {
				message := string(scannedPack.Body)
				zlog.Infof(defs.MDNS_SEARCH, "received udp data", "remote: %v, message: %v", addr, message)
				c.writeConfig([]string{message})
			}
		} else {
			break
		}
	}
}

func (c *UDPClient) Close() {
	err := c.conn.Close()
	if err != nil {
		zlog.Errorf(defs.MDNS_SEARCH, "close udp port error", "err: %v", err.Error())
	}
}

func (c *UDPClient) writeConfig(remoteHosts []string) {
	config, err := c.readConfig()
	if err != nil {
		zlog.Errorf(defs.MDNS_SEARCH, "Read Config File Failed", "err:%v", err)
		return
	}
	config["remoteHosts"] = remoteHosts
	data, err := json.Marshal(config)
	if err != nil {
		zlog.Errorf(defs.MDNS_SEARCH, "Marshal Config File Failed", "err:%v", err)
		return
	}
	err = os.WriteFile(filepath.Join(c.env.InstallDir, "config", "config.json"), data, 0600)
	if err != nil {
		zlog.Errorf(defs.MDNS_SEARCH, "Write Config File Failed", "err:%v", err)
		return
	}
	if !c.env.IsContainer {
		// 容器内守护进程无需更新filebeat
		err = c.writeFileBeatConfig(remoteHosts)
		if err != nil {
			zlog.Errorf(defs.MDNS_SEARCH, "Write FileBeat Config File Failed", "err:%v", err)
		}
	}
	zlog.Infof(defs.MDNS_SEARCH, "[ListenConfig]", "config update, jrasp-daemon will exit(0)...")
	os.Exit(0)
}

func (c *UDPClient) writeFileBeatConfig(remoteHosts []string) error {
	// 修改filebeat配置
	fileName := filepath.Join(c.env.InstallDir, "..", "filebeat", "filebeat.yml")
	if !fsutil.PathExist(fileName) {
		return errors.New(fmt.Sprintf("%v does not exists", fileName))
	}
	var filebeatConfig = viper.New()
	filebeatConfig.SetConfigFile(fileName)
	filebeatConfig.SetConfigType("yaml")
	if err := filebeatConfig.ReadInConfig(); err != nil {
		zlog.Errorf(defs.MDNS_SEARCH, "Read Filebeat Config File Failed", "err:%v", err)
		return err
	}
	for index, _ := range remoteHosts {
		url, _ := url2.Parse(remoteHosts[index])
		path := path.Join(url.Path, "base", "report")
		if url.Scheme == "wss" {
			remoteHosts[index] = fmt.Sprintf("%v://%v%v", "https", url.Host, path)
		}
		if url.Scheme == "ws" {
			remoteHosts[index] = fmt.Sprintf("%v://%v%v", "http", url.Host, path)
		}
	}
	filebeatConfig.Set("output.http.hosts", remoteHosts)
	setting := filebeatConfig.AllSettings()
	configData, err := yaml.Marshal(setting)
	if err != nil {
		zlog.Errorf(defs.MDNS_SEARCH, "Marshal Filebeat Config File Failed", "err:%v", err)
		return err
	}
	err = os.WriteFile(fileName, configData, 0755)
	if err != nil {
		zlog.Errorf(defs.MDNS_SEARCH, "Write Filebeat Config File Failed", "err:%v", err)
		return err
	}
	return nil
}

func (c *UDPClient) readConfig() (map[string]interface{}, error) {
	data, err := os.ReadFile(filepath.Join(c.env.InstallDir, "config", "config.json"))
	if err != nil {
		zlog.Errorf(defs.MDNS_SEARCH, "Read Config File Failed", "err:%v", err)
		return nil, err
	}
	var obj map[string]interface{}
	err = json.Unmarshal(data, &obj)
	if err != nil {
		zlog.Errorf(defs.MDNS_SEARCH, "Unmarshal Config File Failed", "err:%v", err)
		return nil, err
	}
	return obj, nil
}
