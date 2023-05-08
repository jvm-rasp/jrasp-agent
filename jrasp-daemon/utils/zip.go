package utils

import (
	"archive/zip"
	"errors"
	"io"
)

func ReadFileFromZip(zipName string, fileName string) ([]byte, error) {
	r, err := zip.OpenReader(zipName)
	if err != nil {
		return nil, err
	}
	defer r.Close()
	for _, f := range r.File {
		if f.FileInfo().IsDir() || f.Name != fileName {
			continue
		}
		reader, err := f.Open()
		if err != nil {
			return nil, err
		}
		data, err := io.ReadAll(reader)
		reader.Close()
		if err != nil {
			return nil, err
		}
		return data, nil
	}
	return nil, errors.New("not found file: " + fileName)
}

func ReadFileFromZipByPath(zipName string, filePath string) ([]byte, error) {
	r, err := zip.OpenReader(zipName)
	if err != nil {
		return nil, err
	}
	defer r.Close()
	reader, err := r.Open(filePath)
	if err != nil {
		return nil, err
	}
	data, err := io.ReadAll(reader)
	reader.Close()
	if err != nil {
		return nil, err
	}
	return data, nil
}
