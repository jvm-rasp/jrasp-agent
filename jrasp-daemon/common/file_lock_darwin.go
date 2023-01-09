package common

import (
	"fmt"
	"jrasp-daemon/defs"
	"jrasp-daemon/zlog"
	"os"
	"syscall"
)

// Lock 加锁
func (p *PidFile) Lock() error {
	// create pid file
	f, err := os.OpenFile(p.dir, os.O_WRONLY|os.O_CREATE, os.FileMode(defs.FILE_MODE_ONLY_ROOT))
	if err != nil {
		zlog.Errorf(defs.CREATE_PID_FILE, "create daemon pid file", "open or create pid file err:%v", err)
		os.Exit(defs.EXIT_CODE_2)
	}

	// try lock pid file
	err = syscall.Flock(int(f.Fd()), syscall.LOCK_EX|syscall.LOCK_NB)
	if err != nil {
		zlog.Errorf(defs.CREATE_PID_FILE, "cannot flock pid file", "err:%v", err)
		os.Exit(defs.EXIT_CODE_2)
	}

	// update pid fie content
	err = os.Truncate(p.dir, 0)
	if err != nil {
		zlog.Errorf(defs.CLEAN_PID_FILE, "clean pid file content", "err:%v", err)
		os.Exit(defs.EXIT_CODE_2)
	}

	_, err = fmt.Fprintf(f, "%d", os.Getpid())
	if err != nil {
		zlog.Errorf(defs.WRITE_PID_FILE, "update pid file content", "write pid[%d] to file error: %v", os.Getpid(), err)
		os.Exit(defs.EXIT_CODE_2)
	}
	p.f = f
	return nil
}

// Unlock 在此文件描述符关闭时，锁会自动释放。而当进程终止时，所有的文件描述符均会被关闭。所以很多时候就不用考虑类似原子锁解锁的事情
func (p *PidFile) Unlock() error {
	defer p.f.Close()
	return syscall.Flock(int(p.f.Fd()), syscall.LOCK_UN)
}