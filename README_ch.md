# jrasp-agent

![Build Status](https://img.shields.io/badge/Build-passing-brightgreen)
![Version](https://img.shields.io/badge/Version-1.1.5-informational)
![Go Guild Version](https://img.shields.io/badge/Go-v1.19.6+-blue)
![Maven Version](https://img.shields.io/badge/Maven-v3.8.0-blue)
![Java Build Version](https://img.shields.io/badge/Java-v1.6+-blue)
![License](https://img.shields.io/badge/License-LGPL3.0-informational)
![install](https://img.shields.io/badge/install-20000%2B-yellowgreen)
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
- 命令执行模块 （native）
- 反序列化模块（jdk反序列化、fastjson、yaml、stream）
- http模块（springboot、tomcat、jetty、undertown、spark）（IP黑名单、URL黑名单、扫描器识别）
- xxe模块 （dom4j、jdom、jdk）
- 文件访问模块（io、nio）
- 表达式注入模块（spel、ognl）
- sql注入（mysql）
- JNDI注入
- SSRF
- shiro

开发中的进行中的模块：
- danger protocol  (进行中)
- DNS查询  (进行中)
- Memory  (进行中)
- 类加载器 (进行中)
- attach (进行中)

### 支持的jdk版本
+ jdk6+
jdk11+增加jvm参数 --add-opens=java.base/java.lang=ALL-UNNAMED

## 03 快速使用

参考官网安装文档：[最新版本安装](https://www.jrasp.com/guide/install/v1.1.2/jrasp-agent.html)

## 04 开发/编译  （可以跳过，使用release包）

+ jdk 1.8 
+ golang 1.19
+ maven 3.8.5 

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
## 05 版本记录

[RELEASE](CHANGELOG.md)

## 06 技术交流群

加微信：hycsxs

## 07 官网

 https://www.jrasp.com

## 08 说明

基于字节码修改的开源项目[jvm-sandbox](https://github.com/alibaba/jvm-sandbox) ，本项目中的部分hook类与检测算法参考[open-rasp](https://github.com/baidu/openrasp) 感谢优秀的开源项目。 

反哺开源项目：jrasp团队在借鉴开源项目的同时，发现jvm-sandbox/open-rasp等多个bug，并提交给项目社区，受到社区的好评。

## 09 使用者

如果您在使用该项目，请联系我们，添加在这里。

目前有12家公司使用，agent安装量超过2w，请放心使用。

## 10 版权信息

GPL3.0 （您可以在自身的项目中学习与使用，其它行为必须获得授权）
