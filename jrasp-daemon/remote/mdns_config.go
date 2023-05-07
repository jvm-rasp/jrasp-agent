package remote

import (
	"encoding/json"
	"github.com/gookit/goutil/arrutil"
	"github.com/hashicorp/mdns"
	"jrasp-daemon/defs"
	"jrasp-daemon/environ"
	"jrasp-daemon/userconfig"
	"jrasp-daemon/zlog"
	"os"
	"path/filepath"
	"time"
)

type MDNSClient struct {
	entriesCh chan *mdns.ServiceEntry
	env       *environ.Environ
	cfg       *userconfig.Config
}

func NewMDNSClient(cfg *userconfig.Config, env *environ.Environ) *MDNSClient {
	return &MDNSClient{
		env: env,
		cfg: cfg,
	}
}

func (c *MDNSClient) SearchServer() {
	iface, err := environ.GetDefaultIface()
	if err != nil {
		zlog.Errorf(defs.MDNS_SEARCH, "get iface error", "err: %v", err.Error())
		return
	}
	// Make a channel for results and start listening
	c.entriesCh = make(chan *mdns.ServiceEntry, 4)
	go c.listen()
	// Start the lookup
	params := mdns.DefaultParams("jrasp")
	params.Domain = ""
	params.Entries = c.entriesCh
	params.DisableIPv6 = true
	params.Interface = iface
	for {
		if !c.env.IsConnectServer {
			err = mdns.Query(params)
			if err != nil {
				zlog.Errorf(defs.MDNS_SEARCH, "query error", "err: %v", err.Error())
			}
		}
		time.Sleep(time.Second * 5)
	}

}

func (c *MDNSClient) listen() {
	for entry := range c.entriesCh {
		if entry.Name == "admin.jrasp.local." {
			zlog.Infof(defs.MDNS_SEARCH, "Got new entry", "service=%v, ip: %v, port: %v", entry.Info, entry.AddrV4, entry.Port)
			for _, item := range entry.InfoFields {
				if !arrutil.Contains(c.cfg.RemoteHosts, item) {
					c.writeConfig(entry.InfoFields)
				}
			}
		}
	}
}

func (c *MDNSClient) Close() {
	close(c.entriesCh)
}

func (c *MDNSClient) writeConfig(remoteHosts []string) {
	config, err := c.readConfig()
	if err != nil {
		return
	}
	config["remoteHosts"] = remoteHosts
	data, err := json.Marshal(config)
	if err != nil {
		zlog.Errorf(defs.MDNS_SEARCH, "Marshal Config File Failed", "err:%v", err)
	}
	err = os.WriteFile(filepath.Join(c.env.InstallDir, "config", "config.json"), data, 0600)
	if err != nil {
		zlog.Errorf(defs.MDNS_SEARCH, "Write Config File Failed", "err:%v", err)
	}
	zlog.Infof(defs.MDNS_SEARCH, "[ListenConfig]", "config update, jrasp-daemon will exit(0)...")
	os.Exit(0)
}

func (c *MDNSClient) readConfig() (map[string]interface{}, error) {
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
