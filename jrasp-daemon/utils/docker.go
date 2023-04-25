package utils

import (
	"bytes"
	"context"
	"encoding/base64"
	"encoding/json"
	"github.com/docker/docker/api/types"
	"github.com/docker/docker/api/types/container"
	"github.com/docker/docker/client"
	"github.com/docker/docker/pkg/archive"
	"github.com/docker/docker/pkg/system"
	"github.com/docker/go-connections/nat"
	"io"
	"os"
	"path/filepath"
	"time"
)

type Docker struct {
	client *client.Client
	ctx    context.Context
}

func NewDocker() (*Docker, error) {
	ctx := context.Background()
	cli, err := client.NewClientWithOpts(client.FromEnv, client.WithAPIVersionNegotiation())
	if err != nil {
		return nil, err
	}
	return &Docker{
		client: cli,
		ctx:    ctx,
	}, nil
}

func (d Docker) Pull(image, user, password string) (string, error) {
	authConfig := types.AuthConfig{
		Username: user,
		Password: password,
	}
	encodedJSON, err := json.Marshal(authConfig)
	if err != nil {
		return "", err
	}

	authStr := base64.URLEncoding.EncodeToString(encodedJSON)
	reader, err := d.client.ImagePull(d.ctx, image, types.ImagePullOptions{RegistryAuth: authStr})
	if err != nil {
		return "", err
	}
	buf := new(bytes.Buffer)
	_, _ = buf.ReadFrom(reader)
	return buf.String(), nil
}

func (d Docker) Import(file string, image string) (string, error) {
	f, err := os.Open(file)
	defer f.Close()
	if err != nil {
		return "", err
	}

	reader, err := d.client.ImageImport(d.ctx, types.ImageImportSource{Source: f, SourceName: "-"}, image, types.ImageImportOptions{})
	if err != nil {
		return "", err
	}
	buf := new(bytes.Buffer)
	_, _ = buf.ReadFrom(reader)
	return buf.String(), nil
}

func (d Docker) Run(name string, image string, cmd []string, volumes map[string]struct{}, ports nat.PortSet) error {
	resp, err := d.client.ContainerCreate(d.ctx, &container.Config{Image: image, Volumes: volumes, ExposedPorts: ports, Cmd: cmd}, nil, nil, nil, name)
	if err != nil {
		return err
	}
	if err = d.client.ContainerStart(d.ctx, resp.ID, types.ContainerStartOptions{}); err != nil {
		return err
	}
	return nil
}

func (d Docker) Copy(file string, dest string, container string) error {
	srcPath := file
	dstPath := dest
	// Prepare destination copy info by stat-ing the container path.
	dstInfo := archive.CopyInfo{Path: dstPath}
	dstStat, err := d.client.ContainerStatPath(d.ctx, container, dstPath)

	// If the destination is a symbolic link, we should evaluate it.
	if err == nil && dstStat.Mode&os.ModeSymlink != 0 {
		linkTarget := dstStat.LinkTarget
		if !system.IsAbs(linkTarget) {
			// Join with the parent directory.
			dstParent, _ := archive.SplitPathDirEntry(dstPath)
			linkTarget = filepath.Join(dstParent, linkTarget)
		}

		dstInfo.Path = linkTarget
		dstStat, err = d.client.ContainerStatPath(d.ctx, container, linkTarget)
	}

	if err == nil {
		dstInfo.Exists, dstInfo.IsDir = true, dstStat.Mode.IsDir()
	}

	var (
		content         io.Reader
		resolvedDstPath string
	)

	// Prepare source copy info.
	srcInfo, err := archive.CopyInfoSourcePath(srcPath, true)
	if err != nil {
		return err
	}

	srcArchive, err := archive.TarResource(srcInfo)
	if err != nil {
		return err
	}
	defer srcArchive.Close()

	dstDir, preparedArchive, err := archive.PrepareArchiveCopy(srcArchive, srcInfo, dstInfo)
	if err != nil {
		return err
	}
	defer preparedArchive.Close()

	resolvedDstPath = dstDir
	content = preparedArchive

	options := types.CopyToContainerOptions{
		AllowOverwriteDirWithFile: true,
	}
	return d.client.CopyToContainer(d.ctx, container, resolvedDstPath, content, options)
}

func (d Docker) Start(container string) error {
	err := d.client.ContainerStart(d.ctx, container, types.ContainerStartOptions{})
	return err
}

func (d Docker) Stop(containerID string) error {
	timeout := int(time.Second * 5)
	options := container.StopOptions{
		Timeout: &timeout,
	}
	err := d.client.ContainerStop(d.ctx, containerID, options)
	return err
}

func (d Docker) Rm(container string, force bool) error {
	err := d.client.ContainerRemove(d.ctx, container, types.ContainerRemoveOptions{Force: force})
	return err
}

func (d Docker) Push(image string, user string, password string) (string, error) {
	authConfig := types.AuthConfig{
		Username: user,
		Password: password,
	}
	encodedJSON, err := json.Marshal(authConfig)
	if err != nil {
		return "", err
	}

	authStr := base64.URLEncoding.EncodeToString(encodedJSON)
	reader, err := d.client.ImagePush(d.ctx, image, types.ImagePushOptions{RegistryAuth: authStr})
	if err != nil {
		return "", err
	}
	buf := new(bytes.Buffer)
	buf.ReadFrom(reader)
	return buf.String(), err
}

func (d Docker) Exec(container string, workingDir string, cmd []string, env []string) (string, error) {
	id, err := d.client.ContainerExecCreate(d.ctx, container, types.ExecConfig{Tty: true, WorkingDir: workingDir, Cmd: cmd, Env: env, AttachStderr: true, AttachStdout: true})
	if err != nil {
		return "", err
	}
	resp, err := d.client.ContainerExecAttach(d.ctx, id.ID, types.ExecStartCheck{})
	if err != nil {
		return "", err
	}
	buf := new(bytes.Buffer)
	_, err = buf.ReadFrom(resp.Reader)
	if err != nil {
		return "", err
	}
	return buf.String(), err
}

func (d Docker) Restart(container string) error {
	_ = d.Stop(container)
	return d.Start(container)
}

func (d Docker) IsRun(container string) bool {
	stat, err := d.client.ContainerInspect(d.ctx, container)
	if err != nil {
		return false
	}
	if !stat.State.Running {
		return false
	}
	return true
}

func (d Docker) List() ([]types.Container, error) {
	return d.client.ContainerList(d.ctx, types.ContainerListOptions{})
}

func (d Docker) Close() error {
	return d.client.Close()
}
