package monitor

import (
	"os"
	"syscall"
)

func FDLimit() (uint64, error) {
	var rlimit syscall.Rlimit
	if err := syscall.Getrlimit(syscall.RLIMIT_NOFILE, &rlimit); err != nil {
		return 0, err
	}
	return rlimit.Cur, nil
}

func FDUsage() (uint64, error) {
	return countFiles("/proc/self/fd")
}

// countFiles reads the directory named by dirname and returns the count.
func countFiles(dirname string) (uint64, error) {
	f, err := os.Open(dirname)
	if err != nil {
		return 0, err
	}
	list, err := f.Readdirnames(-1)
	f.Close()
	if err != nil {
		return 0, err
	}
	return uint64(len(list)), nil
}
