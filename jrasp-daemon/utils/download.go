package utils

import (
	"crypto/tls"
	"fmt"
	"io/ioutil"
	"net/http"
	"os"
	"strings"
	"time"
)

// DownLoadFile 不依赖oss
func DownLoadFile(url string, fileName string) error {
	var transport http.Transport
	if strings.HasPrefix(url, "https://") {
		transport = http.Transport{TLSClientConfig: &tls.Config{InsecureSkipVerify: true}}
	}
	client := &http.Client{
		Transport: &transport,
		Timeout:   time.Second * 60,
	}
	response, err := client.Get(url)
	if err != nil {
		return fmt.Errorf("download file error:%v", err)
	}
	if response.StatusCode == 200 {
		body, err := ioutil.ReadAll(response.Body)
		if err != nil {
			return fmt.Errorf("read byte stream error:%v", err)
		}
		defer func() {
			rerr := response.Body.Close()
			if rerr != nil {
				fmt.Printf("close response body error: %v \n", rerr)
			}
		}()
		err = ioutil.WriteFile(fileName, body, os.ModePerm)
		if err != nil {
			return fmt.Errorf("write byte to file error:%v", err)
		}
		return nil
	} else {
		return fmt.Errorf("download file[%s] error:%d", url, response.StatusCode)
	}
}
