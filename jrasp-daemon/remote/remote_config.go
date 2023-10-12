package remote

import (
	"crypto/tls"
	"fmt"
	"io/ioutil"
	"jrasp-daemon/defs"
	"jrasp-daemon/environ"
	"jrasp-daemon/new_socket"
	"jrasp-daemon/userconfig"
	"jrasp-daemon/zlog"
	"math/rand"
	"os"
	"path/filepath"
	"strings"
	"time"

	"github.com/gorilla/websocket"
)

const WS_URL_FORMAT = "%s/ws/%s"

func WatchRemoteConfig(cfg *userconfig.Config, env *environ.Environ) {
	for {
		// 随机选取一个服务端
		index := rand.Intn(len(cfg.RemoteHosts))
		url := fmt.Sprintf(WS_URL_FORMAT, cfg.RemoteHosts[index], env.HostName)
		var tlsConfig tls.Config
		if strings.HasPrefix(url, "wss://") {
			tlsConfig = tls.Config{InsecureSkipVerify: true}
		}
		dialer := &websocket.Dialer{
			TLSClientConfig: &tlsConfig,
		}
		conn, _, err := dialer.Dial(url, nil)
		if err != nil {
			zlog.Infof(defs.NACOS_LISTEN_CONFIG, "[ListenConfig]", "create conn failed to remote: %s, error: %v", url, err)
			env.IsConnectServer = false
		} else {
			zlog.InitSocketLog(cfg.LogPath, env.HostName, env.Ip, cfg.LogLevel, conn)
			defer func() { _ = conn.Close() }()

			zlog.Infof(defs.NACOS_LISTEN_CONFIG, "[ListenConfig]", "create conn success to remote: %s, init socket file success.", url)
			env.IsConnectServer = true

			//go heartbeat(conn)

			go reportAgentLogToAdmin(conn)

			for {
				messageType, message, err := conn.ReadMessage()
				if err != nil {
					zlog.Infof(defs.NACOS_LISTEN_CONFIG, "[ListenConfig]", "read remote config error:%v", err)
					break
				}
				if messageType == websocket.BinaryMessage {
					zlog.Infof(defs.NACOS_LISTEN_CONFIG, "[ListenConfig]", "read remote config success")
					writeConfigToFile(env.InstallDir, message)
				}
			}
		}
		time.Sleep(time.Second * time.Duration(cfg.ConnectTime))
	}

}

func heartbeat(conn *websocket.Conn) {
	location := time.FixedZone("CST", 8*3600)
	for {
		t := time.Now().In(location)
		now := t.Format(defs.DATE_FORMAT)
		conn.WriteMessage(websocket.TextMessage, []byte(now))
		time.Sleep(time.Second * 180)
	}
}

func writeConfigToFile(installDir string, data []byte) {
	err := ioutil.WriteFile(filepath.Join(installDir, "config", "config.json"), data, 0600)
	if err != nil {
		zlog.Errorf(defs.NACOS_LISTEN_CONFIG, "[ListenConfig]", "write file to config.json,err:%v", err)
	}
	zlog.Infof(defs.NACOS_LISTEN_CONFIG, "[ListenConfig]", "config update,jrasp-daemon will exit(0)...")
	os.Exit(0)
}

// 发往server
func reportAgentLogToAdmin(conn *websocket.Conn) {
	for {
		select {
		case msg := <-new_socket.AgentMessageChan:
			conn.WriteMessage(websocket.BinaryMessage, []byte(msg))
		}
		//限速，避免将server打挂
		time.Sleep(time.Millisecond * 100)
	}
}
