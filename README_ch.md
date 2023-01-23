# jrasp-agent

![Build Status](https://img.shields.io/badge/Build-passing-brightgreen)
![Version](https://img.shields.io/badge/Version-1.1.1-informational)
![Go Guild Version](https://img.shields.io/badge/Go-v1.16+-blue)
![Maven Version](https://img.shields.io/badge/Maven-v3.25-blue)
![Java Build Version](https://img.shields.io/badge/Java-v1.6+-blue)
![License](https://img.shields.io/badge/License-LGPL3.0-informational)
![install](https://img.shields.io/badge/install-5000%2B-yellowgreen)
![platform](https://img.shields.io/badge/platform-linux%7CmacOS%7Cwindows-success)

## 01 简介

Java Runtime Application Self-Protection 意思是Java应用自我保护系统，简称`jrasp`。

jrasp-agent 是 jrasp 项目最核心的部分。

jrasp-agent 基于Java Agent技术对Java字节码进行修改，增加安全检测逻辑，对漏洞攻击实时检测与阻断。

## 02 特性

### 功能特性

- 安全插件可定制 
- 检测逻辑低延时
- 插件热更新
- Java进程识别与自动注入
- 支持命令执行等的native方法hook，彻底防止绕过；
- 兼容 windows、mac 和 linux
- 体积小，核心jar包600KB

### 性能
- cpu增加5%  (正常请求下测试)
- 内存占用200MB以下

### 自身安全性

- 插件、守护进程HASH校验
- Agent与Daemon socket自定义通信协议与RSA非对称加密；
- 核心功能由自定义类加载器加载，与业务类隔离，提高从JVM内部攻击RASP的难度；
- 反射加固：RASP核心方法（如卸载、降级等）反射加固，防止被恶意反射；
- 不使用第三方框架，如servlet、json、sl4j2、apache-common等

### 安全模块

jrasp-agent 的安全模块 
目前支持的模块有：
- 1.命令执行模块 （native）
- 2.反序列化模块（jdk反序列化、fastjson、yaml、stream）
- 3.http模块（springboot、tomcat、jetty、undertown、spark）（IP黑名单、URL黑名单、扫描器识别）
- 4.xxe模块 （dom4j、jdom、jdk）
- 5.文件访问模块（io、nio）
- 6.表达式注入模块（spel、ognl）
- 7.sql注入（mysql）
- 8.JNDI注入
开发中的进行中的模块：
- 9.SSRF  (进行中)
- 10.danger protocol  (进行中)
- 11.DNS查询  (进行中)
- 12.Memory  (进行中)
- 13.类加载器 (进行中)
- 14.attach (进行中)

### 支持的jdk版本
+ jdk6+
（jdk17以上需要修改jvm参数）

## 03 linux (centos) 快速使用（无需管理端）

- **1 快速安装**

```java
curl https://jrasp-download.oss-cn-shanghai.aliyuncs.com/jrasp-install.sh|bash
```

需要说明的是：上面的安装脚本会将jrasp安装在`/usr/local/` 目录下 

- **2.1 启动方式1 (debug or test)**
  
使用`attach`工具注入Java进程。进入到 jrasp 的安装目录下的bin目录下
```shell
cd ./jrasp/bin
```
对pid为46575的Java进程注入
```java
./attach -p 46575
```

注入后，日志如下：
```shell
2022/09/29 18:04:28 attach java process,pid: 46575
2022/09/29 18:04:28 jvm create uds socket file success
2022/09/29 18:04:28 command socket init success: [0.0.0.0:51370]
2022/09/29 18:04:28 attach jvm success
```
agent 初始化日志在`jrasp/logs`目录下的 jrasp-agent-0.log

- **2.2 启动方式2 (启动守护进程，适用于线上生产环境)**

启动守护进程jrasp-daemon， 守护进程会自动发现Java进程、注入和更新配置等。
进入到 jrasp 的安装目录下的bin目录下，执行：
```java
./jrasp-daemon
```
成功启动日志如下：
```
➜  ./jrasp-daemon 
       _   _____                _____   _____  
      | | |  __ \      /\      / ____| |  __ \ 
      | | | |__) |    /  \    | (___   | |__) |
  _   | | |  _  /    / /\ \    \___ \  |  ___/ 
 | |__| | | | \ \   / ____ \   ____) | | |   
  \____/  |_|  \_\ /_/    \_\ |_____/  |_|
:: JVM RASP ::        (v1.1.1.RELEASE) https://www.jrasp.com
{"level":"INFO","ts":"2023-01-08 22:30:21.150","caller":"jrasp-daemon/main.go:55","msg":"daemon startup","logId":1000,"ip":"192.168.8.145","hostName":"MacBook-Pro","pid":20333,"detail":"{\"agentMode\":\"dynamic\"}"}
{"level":"INFO","ts":"2023-01-08 22:30:21.150","caller":"jrasp-daemon/main.go:57","msg":"config id","logId":1030,"ip":"192.168.8.145","hostName":"MacBook-Pro","pid":20333,"detail":"{\"configId\":1}"}
```
需要注的是：线上环境 `jrasp-daemon` 配置进程自动拉起的`systemctl`

- **3 日志输出**

所有的日志均在 `jrasp/logs/` 目录下

+ jrasp-agent-0.log 记录java agent 的日志
+ jrasp-daemon.log 记录守护进程的日志
+ jrasp-attack-0.log 记录攻击日志

## 04 日志上报到管理端 （非必需，可以跳过）

jrasp-agent 产生的日志在本地磁盘上，可以使用日志收集工具如`filebeat`或者`logtail`传输给kafka

filebeat一键安装命令：
```java
curl https://jrasp-download.oss-cn-shanghai.aliyuncs.com/filebeat-install.sh|bash
```

需要注意的是：仅支持公有云(如阿里云、腾讯云和华为云)环境；日志传输到jrasp官方提供的kafka集群上。

## 05 开发/编译  （可以跳过，使用release包）

+ jdk 1.8 （必需）
+ golang 1.16+ （必需）
+ maven 3.2.5 （必需）

您可以在macos/linux/windows系统上编译该项目，编译的系统必须与运行系统一致（即windows上编译仅限于windows运行）。

进入到 jrsap-agent/bin 目录下, 执行对应环境脚本即可。需要说明的是 macos/windows 仅供开发测试。

+ linux
```shell
bash packages_linux.sh
```
+ macos
```shell
bash packages_darwin.sh
```
## 06 版本记录

[RELEASE](CHANGELOG.md)

## 07 技术交流群

加微信：sear2022

## 08 官网

 https://www.jrasp.com

## 09 说明

基于字节码修改的开源项目[jvm-sandbox](https://github.com/alibaba/jvm-sandbox) ，本项目中的部分hook类与检测算法参考[open-rasp](https://github.com/baidu/openrasp) 感谢优秀的开源项目。 

反哺开源项目：jrasp团队在借鉴开源项目的同时，发现jvm-sandbox/open-rasp等多个bug，并提交给项目社区，受到社区的好评。

## 10 使用者

如果您在使用该项目，请联系我们，添加在这里。

## 11 版权信息

GPL3.0 （您可以在自身的项目中学习与使用，商业化必须获得授权）
