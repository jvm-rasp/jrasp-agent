# jrasp-agent

![Build Status](https://img.shields.io/badge/Build-passing-brightgreen)
![Version](https://img.shields.io/badge/Version-1.1.5-informational)
![Go Guild Version](https://img.shields.io/badge/Go-v1.19.6+-blue)
![Maven Version](https://img.shields.io/badge/Maven-v3.8.0-blue)
![Java Build Version](https://img.shields.io/badge/Java-v1.6+-blue)
![License](https://img.shields.io/badge/License-LGPL3.0-informational)
![install](https://img.shields.io/badge/install-20000%2B-yellowgreen)
![platform](https://img.shields.io/badge/platform-linux%7CmacOS%7Cwindows-success)

## 01 Project introduction [中文说明](README_ch.md)

_Java Runtime Application Self Protection_ means Java application self-protection system, which is called 'jrasp' for short.

jrasp-agent is the core part of jrasp project.

jrasp-agent based on Java Agent technology, modifies Java bytecode, adds security detection logic, detects and blocks vulnerability attacks in real time.

## 02 Characteristics

### Functional characteristics

- Security plug-in can be customized
- Detection logic low delay
- Plug in Hot Update
- Java Process Identification and Automatic Injection
- Support native method hooks such as command execution to completely prevent bypassing;
- Compatible with Windows, Mac and Linux
- Small size, core jar package 600KB

### Performance
- Increase CPU by 5% (test under normal request)
- Memory consumption below 200MB

### Self security

- Plug in and daemon HASH verification
- Agent and Daemon socket customized communication protocol and RSA asymmetric encryption;
- The core functions are loaded by custom class loaders and isolated from business classes, which improves the difficulty of attacking RASP from within the JVM;
- Reflection reinforcement: RASP core methods (such as unloading, degradation, etc.) reflect reinforcement to prevent malicious reflection;
- Do not use third-party frameworks, such as servlet, json, sl4j2, apache common, etc

### Security module

Security module of jrasp agent
The currently supported modules are:
- Command execution module (native)
- Deserialization module (jdk deserialization/fastjson/yaml/stream)
- HTTP module (springboot/tomcat/jetty/underwown/spark) (IP blacklist/URL blacklist/scanner identification)
- xxe module (dom4j/jdom/jdk)
- File access module (io/nio)
- Expression injection module (spel/ognl)
- SQL injection (mysql)
- JNDI injection
- SSRF
- shiro

under development:

- danger protocol
- DNS query 
- Memory 
- Class loader 
- attach

### Supported jdk versions

+ jdk6+

jdk11+ `--add-opens=java.base/java.lang=ALL-UNNAMED`

## 03 Install (centos)

[how to install](https://www.jrasp.com/guide/install/v1.1.2/jrasp-agent.html)

## 04 develop & Compilation (can be skipped, use the release package)

+ jdk 1.8
+ golang 1.19
+ maven 3.8.5

Enter the jrsap-agent/bin directory and execute the corresponding environment script.

It should be noted that macOs/windows is only for development and testing.


## 05 Version record

[RELEASE](CHANGELOG.md)

## 06 Wechart

wx：hycsxs

## 07 Official website

https://www.jrasp.com

## 08 Explanation

+ Based on the open source project [jvm-sandbox](https://github.com/alibaba/jvm-sandbox)

+ the hook class/method part from [open-rasp](https://github.com/baidu/openrasp)
## 09 Users

If you are using it, please contact us and add it here.

## 10 Copyright Information

GPL3.0（You can learn and use in your own projects, other actions must be authorized)
