package zlog

import (
	"bufio"
	"fmt"
	"github.com/gorilla/websocket"
	"io"
	"jrasp-daemon/defs"
	"os"
	"path/filepath"
	"time"

	"go.uber.org/zap"
	"go.uber.org/zap/zapcore"
	"gopkg.in/natefinch/lumberjack.v2"
)

// 同时具备写本地和远程功能

// InitLog main中调用
func InitLog(logLevel int, logPath string, hostName, ip string) {
	InitFileLog(logPath, hostName, ip, logLevel)
}

var raspLogger *RaspDaemonLog

const (
	logFileName string = "jrasp-daemon.log"

	DAEMON_TOPIC = "jrasp-daemon"
)

const (
	DebugLevel int = iota - 1
	InfoLevel
	WarnLevel
	ErrorLevel
	DPanicLevel
	PanicLevel
	FatalLevel
)

type RaspDaemonLog struct {
	provider *zap.Logger // 只能输出结构化日志
	pid      int         // jrasp-daemon pid
	ip       string      // ip
	hostName string      // 主机名称
	level    int         // 日志级别
	LogPath  string
	Conn     *websocket.Conn
}

func InitFileLog(logPath, hostName, ip string, logLevel int) {
	raspLogger = NewFileLog(logPath, hostName, ip, logLevel)
}

func InitSocketLog(logPath, hostName, ip string, logLevel int, conn *websocket.Conn) {
	raspLogger = NewSocketLog(logPath, hostName, ip, logLevel, conn)
	err := SyncFile(raspLogger)
	if err != nil {
		fmt.Println("InitSocketLog error: " + err.Error())
	}
}

// NewLog 初始化日志 logger
func NewFileLog(logPath, hostName, ip string, logLevel int) *RaspDaemonLog {
	config := getZapConfig()

	// 获取io.Writer的实现
	w := getFileWriter(filepath.Join(logPath, logFileName))

	// 实现多个输出
	core := zapcore.NewTee(
		// 日志写入文件
		zapcore.NewCore(zapcore.NewJSONEncoder(config), zapcore.AddSync(w), zapcore.Level(logLevel)),
		// 日志输出到控制台
		zapcore.NewCore(zapcore.NewJSONEncoder(config), zapcore.NewMultiWriteSyncer(zapcore.AddSync(os.Stdout)), zapcore.Level(logLevel)),
	)
	logger := zap.New(core, zap.AddCaller(), zap.AddCallerSkip(1))

	return &RaspDaemonLog{provider: logger, ip: ip, hostName: hostName, pid: os.Getpid(), level: logLevel, LogPath: logPath, Conn: nil}
}

// NewLog 初始化日志 logger
func NewSocketLog(logPath, hostName, ip string, logLevel int, conn *websocket.Conn) *RaspDaemonLog {
	config := getZapConfig()

	w := getSocketWriter(conn, logPath)

	// 实现多个输出
	core := zapcore.NewTee(
		zapcore.NewCore(zapcore.NewJSONEncoder(config), zapcore.AddSync(w), zapcore.Level(logLevel)),
		// 日志输出到控制台
		zapcore.NewCore(zapcore.NewJSONEncoder(config), zapcore.NewMultiWriteSyncer(zapcore.AddSync(os.Stdout)), zapcore.Level(logLevel)),
	)
	logger := zap.New(core, zap.AddCaller(), zap.AddCallerSkip(1))

	return &RaspDaemonLog{provider: logger, ip: ip, hostName: hostName, pid: os.Getpid(), level: logLevel, LogPath: logPath, Conn: conn}
}

func getZapConfig() zapcore.EncoderConfig {
	// 日志配置
	config := zapcore.EncoderConfig{
		MessageKey:   "msg",                       //结构化（json）输出：msg的key
		LevelKey:     "level",                     //结构化（json）输出：日志级别的key（INFO，WARN，ERROR等）
		TimeKey:      "ts",                        //结构化（json）输出：时间的key（INFO，WARN，ERROR等）
		CallerKey:    "caller",                    //结构化（json）输出：打印日志的文件对应的Key
		EncodeLevel:  zapcore.CapitalLevelEncoder, //将日志级别转换成大写（INFO，WARN，ERROR等）
		EncodeCaller: zapcore.ShortCallerEncoder,  //采用短文件路径编码输出（test/main.go:14 ）
		EncodeTime: func(t time.Time, enc zapcore.PrimitiveArrayEncoder) {
			location := time.FixedZone("CST", 8*3600)
			t = t.In(location)
			enc.AppendString(t.Format(defs.DATE_FORMAT))
		}, //输出的时间格式
		EncodeDuration: func(d time.Duration, enc zapcore.PrimitiveArrayEncoder) {
			enc.AppendInt64(int64(d) / 1000000)
		},
	}
	return config
}

// getWriter
func getFileWriter(filename string) io.Writer {
	return &lumberjack.Logger{
		Filename:   filename,
		MaxSize:    50,    //最大M数，超过则切割
		MaxBackups: 5,     //最大文件保留数，超过就删除最老的日志文件
		MaxAge:     7,     //保存7天
		Compress:   false, //是否压缩
	}
}

func getSocketWriter(conn *websocket.Conn, logPath string) io.Writer {
	return &SocketWriter{
		conn:    conn,
		LogPath: logPath,
	}
}

// conn 包装一层，实现 io 接口
type SocketWriter struct {
	conn    *websocket.Conn
	LogPath string
}

func (s *SocketWriter) Write(p []byte) (n int, err error) {
	err = s.conn.WriteMessage(websocket.BinaryMessage, p)
	if err != nil {
		// 切换为file writer
		InitFileLog(raspLogger.LogPath, raspLogger.hostName, raspLogger.ip, raspLogger.level)
	}
	return len(p), err
}

func (s *SocketWriter) Close() error {
	return s.conn.Close()
}

// 同步文件中的日志
func SyncFile(raspLogger *RaspDaemonLog) error {
	fileName := filepath.Join(raspLogger.LogPath, logFileName)
	file, err := os.Open(fileName)
	if err != nil {
		return err
	}
	defer file.Close()

	scanner := bufio.NewScanner(file)

	for scanner.Scan() {
		raspLogger.Conn.WriteMessage(websocket.BinaryMessage, scanner.Bytes())
	}

	// 同步完成之后，删除文件
	if err := os.Remove(fileName); err != nil {
		return err
	}
	return nil
}

const (
	LOG_ID_KEY    = "logId"
	IP_KEY        = "ip"
	HOST_NAME_KEY = "hostName"
	PID_KEY       = "pid"
	DETAIL_KEY    = "detail"
	TOPIC_KEY     = "topic"
)

func Debugf(logId int, msg string, format string, v ...interface{}) {
	if raspLogger != nil && raspLogger.level <= DebugLevel {
		raspLogger.provider.Debug(
			msg,
			zap.Int(LOG_ID_KEY, logId),
			zap.String(TOPIC_KEY, DAEMON_TOPIC),
			zap.String(IP_KEY, raspLogger.ip),
			zap.String(HOST_NAME_KEY, raspLogger.hostName),
			zap.Int(PID_KEY, raspLogger.pid),
			zap.String(DETAIL_KEY, fmt.Sprintf(format, v...)))
	}
}

func Infof(logId int, msg string, format string, v ...interface{}) {
	if raspLogger != nil && raspLogger.level <= InfoLevel {
		raspLogger.provider.Info(
			msg,
			zap.Int(LOG_ID_KEY, logId),
			zap.String(TOPIC_KEY, DAEMON_TOPIC),
			zap.String(IP_KEY, raspLogger.ip),
			zap.String(HOST_NAME_KEY, raspLogger.hostName),
			zap.Int(PID_KEY, raspLogger.pid),
			zap.String(DETAIL_KEY, fmt.Sprintf(format, v...)))
	}
}

func Warnf(logId int, msg string, format string, v ...interface{}) {
	if raspLogger != nil && raspLogger.level <= WarnLevel {
		raspLogger.provider.Warn(
			msg,
			zap.Int(LOG_ID_KEY, logId),
			zap.String(TOPIC_KEY, DAEMON_TOPIC),
			zap.String(IP_KEY, raspLogger.ip),
			zap.String(HOST_NAME_KEY, raspLogger.hostName),
			zap.Int(PID_KEY, raspLogger.pid),
			zap.String(DETAIL_KEY, fmt.Sprintf(format, v...)))
	}
}

func Errorf(logId int, msg string, format string, v ...interface{}) {
	if raspLogger != nil && raspLogger.level <= ErrorLevel {
		raspLogger.provider.Error(
			msg,
			zap.Int(LOG_ID_KEY, logId),
			zap.String(TOPIC_KEY, DAEMON_TOPIC),
			zap.String(IP_KEY, raspLogger.ip),
			zap.String(HOST_NAME_KEY, raspLogger.hostName),
			zap.Int(PID_KEY, raspLogger.pid),
			zap.String(DETAIL_KEY, fmt.Sprintf(format, v...)))
	}
}

func Fatalf(logId int, msg string, format string, v ...interface{}) {
	if raspLogger != nil && raspLogger.level <= FatalLevel {
		raspLogger.provider.Fatal(
			msg,
			zap.Int(LOG_ID_KEY, logId),
			zap.String(TOPIC_KEY, DAEMON_TOPIC),
			zap.String(IP_KEY, raspLogger.ip),
			zap.String(HOST_NAME_KEY, raspLogger.hostName),
			zap.Int(PID_KEY, raspLogger.pid),
			zap.String(DETAIL_KEY, fmt.Sprintf(format, v...)))
	}
}
