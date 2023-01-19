# jrasp-attach

## Project introduction [中文说明](README_ch.md)
jrasp-attach is a gadget that helps agents attach into the JVM.

Only use in `test` or `debug`.

## HOW TO USE

Go to the JRASP installation directory, execute `./attach -h`

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

###1. attach jrasp agent to jvm
```java
$ ./attach -p 6841
2022/12/10 14:32:34 attach java process,pid: 6841
2022/12/10 14:32:38 jvm create uds socket file success
2022/12/10 14:32:38 command socket init success: [0.0.0.0:50523]
2022/12/10 14:32:38 attach jvm success
```

###2. list hook class of jrasp agent
```java
$ ./attach -p 6841 -l
2022/12/10 14:35:02 attach java process,pid: 6841
2022/12/10 14:35:02 jvm create uds socket file success
2022/12/10 14:35:02 command socket init success: [0.0.0.0:50523]
2022/12/10 14:35:02 list transform class:
java/lang/UNIXProcess#forkAndExec(I[B[B[BI[BI[B[IZ)I,true
```
`true` indicates that the bytecode is modified successfully

###3. update module data

```java
$ ./attach -p 6841 -d rce-hook:disable=true
2022/12/10 14:42:37 attach java process,pid: 6841
2022/12/10 14:42:37 jvm create uds socket file success
2022/12/10 14:42:37 command socket init success: [0.0.0.0:50905]
2022/12/10 14:42:37 update module data,rce-hook:disable=true
2022/12/10 14:42:37 update parameters result:true
```
RCE module as an example, update the disable parameter to true

###4. stop jrasp agent
```java
$ ./attach -p 6841 -s
2022/12/10 14:36:48 attach java process,pid: 6841
2022/12/10 14:36:48 jvm create uds socket file success
2022/12/10 14:36:48 command socket init success: [0.0.0.0:50523]
2022/12/10 14:36:48 stop agent
2022/12/10 14:36:53 result:success
```
