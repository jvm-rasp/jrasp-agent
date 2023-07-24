package monitor

import (
	"jrasp-daemon/utils"
	"log"
	"testing"
)

func TestNewFileInfo(t *testing.T) {
	info := NewFileInfo(1000, 10000, 80, 10)
	log.Println("fd: " + utils.ToString(info))
	if info != nil && info.IsOverLimit {
		log.Fatalf("check daemon FD error")
	}

	info = NewFileInfo(8000, 10000, 80, 10)
	log.Println("fd: " + utils.ToString(info))
	if info != nil && !info.IsOverLimit {
		log.Fatalf("check daemon FD error")
	}
}
