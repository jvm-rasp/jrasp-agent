package utils

import (
	"crypto/md5"
	"encoding/hex"
	"io"
	"os"
	"fmt"
	"bufio"
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

func CopyFile(srcFile, dstFile string, perm os.FileMode) error {
	srcStat, err := os.Stat(srcFile)
	if err != nil {
		return err
	}

	if !srcStat.Mode().IsRegular() {
		return fmt.Errorf("src file is not a regular file")
	}

	srcf, err := os.Open(srcFile)
	if err != nil {
		return err
	}
	defer srcf.Close()
	
	dstf, err := os.OpenFile(dstFile, os.O_RDWR|os.O_CREATE|os.O_TRUNC, perm)
	if err != nil {
		return err
	}
	defer dstf.Close()
	_, err = io.Copy(dstf, srcf)
	return err
}

func CreateDir(path string) {
	if _, err := os.Stat(path); os.IsNotExist(err) {
		os.MkdirAll(path, os.ModePerm)
	}
}

func ReadLines(path string, count int) ([]string, error) {
	f, err := os.Open(path)
	if err != nil {
		return nil, err
	}
	defer f.Close()

	var lines []string
	r := bufio.NewReader(f)
	i := 0
	for {
		if count != 0 && i == count {
			break
		}
		// ReadLine is a low-level line-reading primitive.
		// Most callers should use ReadBytes('\n') or ReadString('\n') instead or use a Scanner.
		bytes, _, err := r.ReadLine()
		if err == io.EOF {
			break
		}
		if err != nil {
			return lines, err
		}
		lines = append(lines, string(bytes))
		i++
	}
	return lines, nil
}