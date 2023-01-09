package java_process

import (
	"io/ioutil"
	"log"
	"os"
	"testing"
)

func Test_splitContent(t *testing.T) {
	filePath := ".jrasp.token"
	defer os.Remove(filePath)
	ioutil.WriteFile(filePath, []byte("jrasp;0.0.0.0;57058"), 0700)
	ip, port, err := splitContent(filePath)
	if err != nil {
		log.Fatalf("read token file error,file:%s", filePath)
	}
	if "57058" != port {
		log.Fatalf("read token file error,export port:57058,actual:%s", port)
	}
	if "0.0.0.0" != ip {
		log.Fatalf("read token file error,export port:57058,actual:%s", port)
	}
}
