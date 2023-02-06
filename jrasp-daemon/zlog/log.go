package zlog

import (
	"fmt"
	"io"
	"jrasp-daemon/defs"
	"os"
	"path/filepath"
	"time"

	"go.uber.org/zap"
	"go.uber.org/zap/zapcore"
	"gopkg.in/natefinch/lumberjack.v2"
)

var defaultLogger *Log

const logFileName string = "jrasp-daemon.log"

func InitLogger(logger *Log) {
	defaultLogger = logger
}

type Log struct {
	provider *zap.Logger // 只能输出结构化日志
	pid      int         // jrasp-daemon pid
	ip       string      // ip
	hostName string      // 主机名称
	level    int         // 日志级别
}

// InitLog main中调用
func InitLog(logLevel int, logPath string, hostName, ip string) {
	logger := NewLog(logPath, hostName, ip, logLevel)
	InitLogger(logger)
}

// NewLog 初始化日志 logger
func NewLog(logPath, hostName, ip string, logLevel int) *Log {
	// 日志配置
	config := zapcore.EncoderConfig{
		MessageKey:   "msg",                       //结构化（json）输出：msg的key
		LevelKey:     "level",                     //结构化（json）输出：日志级别的key（INFO，WARN，ERROR等）
		TimeKey:      "ts",                        //结构化（json）输出：时间的key（INFO，WARN，ERROR等）
		CallerKey:    "caller",                    //结构化（json）输出：打印日志的文件对应的Key
		EncodeLevel:  zapcore.CapitalLevelEncoder, //将日志级别转换成大写（INFO，WARN，ERROR等）
		EncodeCaller: zapcore.ShortCallerEncoder,  //采用短文件路径编码输出（test/main.go:14 ）
		EncodeTime: func(t time.Time, enc zapcore.PrimitiveArrayEncoder) {
			enc.AppendString(t.Format(defs.DATE_FORMAT))
		}, //输出的时间格式
		EncodeDuration: func(d time.Duration, enc zapcore.PrimitiveArrayEncoder) {
			enc.AppendInt64(int64(d) / 1000000)
		},
	}

	// 获取io.Writer的实现
	fileWriter := getWriter(filepath.Join(logPath, logFileName))

	// 实现多个输出
	core := zapcore.NewTee(
		// 日志写入文件
		zapcore.NewCore(zapcore.NewJSONEncoder(config), zapcore.AddSync(fileWriter), zapcore.Level(logLevel)),
		// 日志输出到控制台
		zapcore.NewCore(zapcore.NewJSONEncoder(config), zapcore.NewMultiWriteSyncer(zapcore.AddSync(os.Stdout)), zapcore.Level(logLevel)),
	)
	logger := zap.New(core, zap.AddCaller(), zap.AddCallerSkip(1))

	return &Log{provider: logger, ip: ip, hostName: hostName, pid: os.Getpid(), level: logLevel}
}

// getWriter
func getWriter(filename string) io.Writer {
	return &lumberjack.Logger{
		Filename:   filename,
		MaxSize:    50,    //最大M数，超过则切割
		MaxBackups: 5,     //最大文件保留数，超过就删除最老的日志文件
		MaxAge:     7,     //保存7天
		Compress:   false, //是否压缩
	}
}

const (
	DebugLevel int = iota - 1
	InfoLevel
	WarnLevel
	ErrorLevel
	DPanicLevel
	PanicLevel
	FatalLevel
)

const (
	LOG_ID_KEY    = "logId"
	IP_KEY        = "ip"
	HOST_NAME_KEY = "hostName"
	PID_KEY       = "pid"
	DETAIL_KEY    = "detail"
)

func Debugf(logId int, msg string, format string, v ...interface{}) {
	if defaultLogger == nil {
		return
	}
	if defaultLogger.level <= DebugLevel {
		defaultLogger.provider.Debug(
			msg,
			zap.Int(LOG_ID_KEY, logId),
			zap.String(IP_KEY, defaultLogger.ip),
			zap.String(HOST_NAME_KEY, defaultLogger.hostName),
			zap.Int(PID_KEY, defaultLogger.pid),
			zap.String(DETAIL_KEY, fmt.Sprintf(format, v...)))
	}
}

func Infof(logId int, msg string, format string, v ...interface{}) {
	if defaultLogger == nil {
		fmt.Printf(format+"\n", v...)
		return
	}
	if defaultLogger.level <= InfoLevel {
		defaultLogger.provider.Info(
			msg,
			zap.Int(LOG_ID_KEY, logId),
			zap.String(IP_KEY, defaultLogger.ip),
			zap.String(HOST_NAME_KEY, defaultLogger.hostName),
			zap.Int(PID_KEY, defaultLogger.pid),
			zap.String(DETAIL_KEY, fmt.Sprintf(format, v...)))
	}
}

func Warnf(logId int, msg string, format string, v ...interface{}) {
	if defaultLogger == nil {
		fmt.Printf(format+"\n", v...)
		return
	}
	if defaultLogger.level <= WarnLevel {
		defaultLogger.provider.Warn(
			msg,
			zap.Int(LOG_ID_KEY, logId),
			zap.String(IP_KEY, defaultLogger.ip),
			zap.String(HOST_NAME_KEY, defaultLogger.hostName),
			zap.Int(PID_KEY, defaultLogger.pid),
			zap.String(DETAIL_KEY, fmt.Sprintf(format, v...)))
	}
}

func Errorf(logId int, msg string, format string, v ...interface{}) {
	if defaultLogger == nil {
		fmt.Printf(format+"\n", v...)
		return
	}
	if defaultLogger.level <= ErrorLevel {
		defaultLogger.provider.Error(
			msg,
			zap.Int(LOG_ID_KEY, logId),
			zap.String(IP_KEY, defaultLogger.ip),
			zap.String(HOST_NAME_KEY, defaultLogger.hostName),
			zap.Int(PID_KEY, defaultLogger.pid),
			zap.String(DETAIL_KEY, fmt.Sprintf(format, v...)))
	}
}

func Fatalf(logId int, msg string, format string, v ...interface{}) {
	if defaultLogger == nil {
		fmt.Printf(format+"\n", v...)
		return
	}
	if defaultLogger.level <= FatalLevel {
		defaultLogger.provider.Fatal(
			msg,
			zap.Int(LOG_ID_KEY, logId),
			zap.String(IP_KEY, defaultLogger.ip),
			zap.String(HOST_NAME_KEY, defaultLogger.hostName),
			zap.Int(PID_KEY, defaultLogger.pid),
			zap.String(DETAIL_KEY, fmt.Sprintf(format, v...)))
	}
}
