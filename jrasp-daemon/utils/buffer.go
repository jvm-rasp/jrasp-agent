package utils

import "io"

const defaultBufSize = 4096
const maxPacketSize = 65535

type Buffer struct {
	buf    []byte
	rd     io.Reader
	idx    int
	length int
}

func NewBuffer(rd io.Reader) *Buffer {
	var b [defaultBufSize]byte
	return &Buffer{
		buf: b[:],
		rd:  rd,
	}
}

func (b *Buffer) fill(need int) error {
	if b.length > 0 && b.idx > 0 {
		copy(b.buf[0:b.length], b.buf[b.idx:])
	}

	if need > len(b.buf) {
		newBuf := make([]byte, ((need/defaultBufSize)+1)*defaultBufSize)
		copy(newBuf, b.buf)
		b.buf = newBuf
	}

	b.idx = 0

	for {
		n, err := b.rd.Read(b.buf[b.length:])
		b.length += n

		if err == nil {
			if b.length < need {
				continue
			}
			return nil
		}
		if b.length >= need && err == io.EOF {
			return nil
		}
		return err
	}
}

func (b *Buffer) ReadNext(need int) ([]byte, error) {
	if b.length < need {
		if err := b.fill(need); err != nil {
			return nil, err
		}
	}

	offset := b.idx
	b.idx += need
	b.length -= need
	return b.buf[offset:b.idx], nil
}

func (b *Buffer) takeBuffer(length int) []byte {
	if b.length > 0 {
		return nil
	}

	if length <= defaultBufSize || length <= cap(b.buf) {
		return b.buf[:length]
	}

	if length < maxPacketSize {
		b.buf = make([]byte, length)
		return b.buf
	}
	return make([]byte, length)
}

func (b *Buffer) takeSmallBuffer(length int) []byte {
	if b.length == 0 {
		return b.buf[:length]
	}
	return nil
}

func (b *Buffer) takeCompleteBuffer() []byte {
	if b.length == 0 {
		return b.buf
	}
	return nil
}
