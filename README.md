# jrasp-agent

## 项目简介

Java Runtime Application Self-Protection 意思是Java应用自我保护系统，简称`jrasp`。

jrasp-agent 是 jrasp 项目最核心的部分。

jrasp-agent 基于Java Agent技术对Java字节码进行修改，增加安全检测逻辑，对漏洞攻击实时检测与阻断。

## 功能特性

- 安全插件可定制
- 检测逻辑低延时
- 插件热更新

## 运行环境依赖
+ jdk6～11

## 编译环境
+ jdk8
+ mvn 3.2.5
## 快速安装

- **1.下载并安装**

  ```shell
  # 1.下载最新版本的jrasp 
  git clone https://github.com/jvm-rasp/jrasp-agent.git
  
  # 2.进入到bin目录下 
  cd jrasp-agent/bin
  
  # 3.编译打包
  ./jrasp-packages.sh
  
  # 4.打包后的安装包位置，复制安装包到指定位置
  jrasp-agent/target/jrasp-agent-1.0-bin.zip
  
  # 5.安装包解压
  unzip jrasp-agent-1.0-bin.zip
  ```
- **2.对Java进程开启防护**

  ```shell
  # 进入沙箱执行脚本
  cd jrasp-agent/bin

  # 注入JVM进程20855
  ./jrasp.sh -p 20855
  ```
  注入成功之后的提示
  ```
  {"code":200,"data":{"mode":"ATTACH","raspHome":"/Users/xxx/Desktop/jrasp-agent/bin/..","version":"1.0","username":"admin"}}
  点击链接获得技术支持: https://applink.feishu.cn/client/chat/chatter/add_by_link?link_token=e35l9ee2-a000-45ba-8863-75b5ca2cdbe6
  ```
- **3.卸载jrasp**
  ```shell
  ./jrasp.sh -p 20855 -s 
  ```
  卸载jrasp成功的提示
  ```shell
  {"code":200,"message":"shutdown success"}
  ```

## 技术交流群(如二维码过期，请加官方运营微信：sear244)
![image](https://github.com/jvm-rasp/assets/blob/master/tech-chat.png)

## 技术公众号
![image](https://github.com/jvm-rasp/assets/blob/master/gongzhonghao.jpeg)


## 贡献者


## 使用者


## 版权信息

 GPL3.0
