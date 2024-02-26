# 版本迭代

## 1.1.5  【2024-02-26】
> 1.1.5 主要以修复bug为主
### Bugfix
+ [agent] agent日志中增加ip
+ [agent] 修复jdk兼容性问题
+ [module] jndi bug 修复
+ [module] 内存马bug修复
+ [module] 默认启用反序列化
+ [module] spel开关类型不匹配bug修复
+ [daemon] 解决daemon日志写入/var/log/messages问题

## 1.1.4  【2023-10-26】
### Enhancement
+ [module] 增加自定义的响应头X-Protected-By：JRASP
+ [module] 增加java.io.file#createNewFile的hook
+ [module] 增加http response的hook和检测模块
### Bugfix
+ [module] 修复xxe模块运行类转换失败问题
+ [agent] 修复日志路径初始化为空的bug
+ [module] 修复请求中contentType判断bug
+ [module] 修复xml反序列化漏报问题
+ [daemon] 修复启动脚本service.sh进程错误识别问题
+ [daemon] 解决进程延迟上报bug

## 1.1.3  【2023-09-09】

> 1.1.3 主要以修复bug为主，新特性暂缓合入

### Enhancement
+ [module] 重构JNDI检测模块
+ [daemon] 新增jrasp-daemon打开文件数量监控
+ [daemon] 新增jdk反序列化黑名单

### Bugfix
+ [module] 修复http-hook类型转换失败的bug
+ [module] 扫描器特征识别优化，防止误报
+ [daemon] 修复json反序列化检测异常
+ [module] 获取http请求参数置后
+ [daemon] PathExists优化解决cpu飙高问题
+ [daemon] 解决进程延迟上报bug

## 1.1.2  【2023-07-26】

> 1.1.2 主要以修复bug为主，新特性暂缓合入

### Enhancement
+ [agent] 增加jvm性能监控
+ [module] 新增shiro检测模块

### Bugfix
+ [module] xercesImpl 2.6.2 版本没有实现setFeature方法,调用会报错的问题
+ [module] 修复xml反序列化类型转换错误问题
+ [daemon] 修复docker主机名称获取bug
+ [module] 修复sql检测算法cpu耗时高的问题
+ [module] 修复命令token分割失败问题
+ [module] 修复xml反序列化类参数读取错误
### TODO
+ [daemon] 支持容器&进程运行时注入
+ [agent] jrasp agent内置 filebeat
+ [module] 模块参数根据注解自动生成

## 1.1.1 正式版 【2023-05】
### Enhancement
+ [module] 新增内存马检测模块
+ [module] 模块增加编译时间便于区分版本
+ [daemon] deamon与server通信支持https
### Bugfix
+ [agent] jrasp日志与tomcat日志完全隔离
### TODO
+ [daemon] 支持安装目录lib下jar更新

## 1.1.1 【2023-01】
### Enhancement
+ [module] SQL检测增加LRU缓存，相同sql仅检测一次
+ [module] 检测模块增加自定义html
+ [module] 插件jar包支持加密与运行时类加载器解密
+ [工程] 将模块加密流程增加到maven plugin中
+ [工程] 支持 linux aarch64架构
### Bugfix
+ [attach & build] 解决windows打包脚本兼容性问题，增加windows系统编译自动打包功能；
+ [module] 解决方法参数涉及第三方类时触发类的依赖加载bug
+ [agent] 修复字符串参数转map参数丢失bug
+ [agent] 修复全局配置非单例的bug @羽音
### TODO
+ [部署方案] 小规模服务部署
+ [module] 支持sqlserver数据库
+ [agent] 去掉模块复制到run目录的功能
+ [工程] 提供方便测试的jrasp-vulns 工程
+ [daemon] 支持安装目录lib下jar更新

## 1.1.0【2022-10】
### Enhancement
+ [attach] 新增jrasp-attach工程(Golang)，支持手动注入、查看hook类、更新模块参数和卸载RASP
+ [agent] agent依赖的bridge打包时指定，防止加载错误依赖
+ [agent] 去掉logback/sl4j，使用原生jul ，减少不安全的依赖
+ [agent] 去掉内置jetty，使用原生socket
+ [agent] 全局唯一 classtransformer 降低类加载时的锁占用问题
+ [agent] 优化类匹配机制，全局唯一transform实例，减少stw时间
+ [agent] jvmsandbox的类匹配机制存在严重性能问题，优化类匹配机制
+ [agent] 去掉java-agent的json日志格式，并修改filebeat的日志分割grok表达式
+ [module] 上下文对象优化为context对象
+ [module] module统一参数更新接口
+ [project] 将jrasp-agent、jrasp-module、jrasp-attach和jrasp-daemon等工程合并，统一编译打包
+ [project] 全面兼容 windows、linux、mac
### BugFix
+ [agent] jar包文件名称增加版本号，解决jar包文件句柄清除问题
+ [module] 替换 @Resource 注解，解决与javax包类的冲突
+ [agent] 解决jvm-sandbox抛出异常时的内存泄漏 bug （jvm-sandbox 已经合入补丁）
+ [jetty module] 解决jetty http input.read方法重复hook问题 （在openrasp上已经复现该问题）
+ [xxe module] 解决dom4j方法重复hook问题 （在openrasp官方已经确认该问题）

### TODO
+ [agent] 使用InheritableThreadLocal代替ThreadLocal防止线程注入 (存在内存泄漏，暂缓)

## 1.0.8 【2022-08】（内部测试版本）
### Enhancement
+ [module] 增加多个安全模块
+ [daemon] 进程扫描优化
+ [daemon] 防止启动多个守护进程

## 1.0.7 【2022-07】（用户使用的稳定版本）
### Enhancement
+ [daemon] 上报配置更新时间
+ [daemon] daemon启动上报nacos初始化的状态和注册的服务ip
+ [daemon] 发现无法连接nacos时，自动重启，24小时检测一次

### BugFix
+ [daemon] 修复软刷新panic
+ [daemon] 删除获取依赖的功能，由安全插件自行上报

## 1.0.6 【2022-06】
### BugFix
+ [daemon] 使用 os.RemoveAll 删除Java进程文件夹

## 1.0.5 【2022-05】
+ [daemon]插件以配置文件为准，配置文件中没有的，删除磁盘上的
+ [daemon]注入后增加软刷新功能和参数更新功能

## 1.0.4 【2022-04】 （开源版本）
+ [agent] 增加native方法hook
+ [daemon] 支持对多个Java进程注入，每个Java进程独立的数据目录
