package utils

import (
	"crypto/md5"
	"encoding/hex"
	"io"
	"os"
)

// PathExists reports whether the named file or directory exists.
func PathExists(path string) bool {
	if path == "" {
		return false
	}

	if _, err := os.Stat(path); err != nil {
		if os.IsNotExist(err) {
			return false
		}
	}
	return true
}

// GetFileMd5 linux  md5sum file、macos md5 file
func GetFileMd5(pluginPath string) (string, error) {
	file, err := os.Open(pluginPath)
	if err != nil {
		return "", err
	}
	defer func() {
		ferr := file.Close()
		if ferr != nil {
			err = ferr
		}
	}()
	// md5 方便在mac上机算
	h := md5.New()
	_, err = io.Copy(h, file)
	if err != nil {
		return "", err
	}
	return hex.EncodeToString(h.Sum(nil)), nil
}
