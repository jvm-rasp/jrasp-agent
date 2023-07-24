//go:build !linux

package monitor

import (
	"fmt"
	"runtime"
)

func FDLimit() (uint64, error) {
	return 0, fmt.Errorf("cannot get FDLimit on %s", runtime.GOOS)
}

func FDUsage() (uint64, error) {
	return 0, fmt.Errorf("cannot get FDUsage on %s", runtime.GOOS)
}
