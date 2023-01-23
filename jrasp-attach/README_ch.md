# jrasp-attach

## 简介
jrasp-attach 是一个将jrasp-agent注入到目标JVM的小工具，适合于单机环境的测试、故障排查等场景。

## 使用
在执行编译打包之后，进入到 jrasp-agent的安装目录bin目录下执行 `./attach -h` 获取帮助文档：

```shell
$ ./attach -h
Usage of ./attach:
  -c string
        usage for update global config. example: ./attach -p <pid> -c k=v
  -d string
        usage for update module data. example: ./attach -p <pid> -d rce-hook:k1=v1;k2=v2;k3=v31,v32,v33
  -l    usage for list transform class. example: ./attach -p <pid> -l
  -p int
        usage for attach java pid. example: ./attach -p <pid> (default -1)
  -s    usage for stop agent. example: ./attach -p <pid> -s
  -u string
        usage for unload module. example: ./attach -p <pid> -u rce-hook
  -v    usage for attach version. example: ./attach -v
```

###1. 对Java进程发起注入（process）
```java
$ ./attach -p 6841
2022/12/10 14:32:34 attach java process,pid: 6841
2022/12/10 14:32:38 jvm create uds socket file success
2022/12/10 14:32:38 command socket init success: [0.0.0.0:50523]
2022/12/10 14:32:38 attach jvm success
```

###2. 获取已经hook的类（list）
```java
$ ./attach -p 6841 -l
2022/12/10 14:35:02 attach java process,pid: 6841
2022/12/10 14:35:02 jvm create uds socket file success
2022/12/10 14:35:02 command socket init success: [0.0.0.0:50523]
2022/12/10 14:35:02 list transform class:
java/lang/UNIXProcess#forkAndExec(I[B[B[BI[BI[B[IZ)I,true
```
`true`：表示该方法字节码修改成功

`false`：表示已经该类不存在或者未被加载（java类的懒加载）

###3. 更新模块参数 (data)
这里以rce-hook模块为例子，更新模块的`disable`参数值为true
```java
$ ./attach -p 6841 -d rce-hook:disable=true
2022/12/10 14:42:37 attach java process,pid: 6841
2022/12/10 14:42:37 jvm create uds socket file success
2022/12/10 14:42:37 command socket init success: [0.0.0.0:50905]
2022/12/10 14:42:37 update module data,rce-hook:disable=true
2022/12/10 14:42:37 update parameters result:true
```

###4. 卸载jrasp-agent（stop）
```java
$ ./attach -p 6841 -s
2022/12/10 14:36:48 attach java process,pid: 6841
2022/12/10 14:36:48 jvm create uds socket file success
2022/12/10 14:36:48 command socket init success: [0.0.0.0:50523]
2022/12/10 14:36:48 stop agent
2022/12/10 14:36:53 result:success
```
