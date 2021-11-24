package com.jrasp.api;

import java.net.InetSocketAddress;

/**
 * 沙箱配置信息
 */
public interface ConfigInfo {

    /**
     * 获取沙箱的命名空间
     *
     * @return 沙箱的命名空间
     */
    String getNamespace();

    /**
     * 获取沙箱的加载模式
     *
     * @return 沙箱加载模式
     */
    Information.Mode getMode();

    /**
     * 判断沙箱是否启用了unsafe
     * <p>unsafe功能启用之后，沙箱将能修改被BootstrapClassLoader所加载的类</p>
     *
     * @return true:功能启用;false:功能未启用
     */
    boolean isEnableUnsafe();

    /**
     * 获取沙箱的HOME目录(沙箱主程序目录)
     *
     * @return 沙箱HOME目录
     */
    String getRaspHome();

    /**
     * 获取沙箱的系统模块目录地址
     *
     * @return 系统模块目录地址
     */
    String getModuleLibPath();

    /**
     * 获取沙箱的系统模块目录地址
     * <p>沙箱将会从该模块目录中寻找并加载所有的模块</p>
     *
     * @return 系统模块目录地址
     */
    String getSystemModuleLibPath();

    /**
     * 获取沙箱内部服务提供库目录
     *
     * @return 沙箱内部服务提供库目录
     */
    String getSystemProviderLibPath();

    /**
     * 获取沙箱的用户模块目录地址
     * <p>沙箱将会优先从系统模块地址{@link #getModuleLibPath()}加载模块，然后再从用户模块目录地址加载模块</p>
     *
     * @return 用户模块目录地址
     */
    String getUserModuleLibPath();

    /**
     * 获取沙箱HTTP服务侦听地址
     * 如果服务器未能完成端口的绑定，则返回("0.0.0.0:0")
     *
     * @return 沙箱HTTP服务侦听地址
     */
    InetSocketAddress getServerAddress();

    /**
     * 获取沙箱HTTP服务返回编码
     *
     * @return 沙箱HTTP服务返回编码
     * @since 1.2.2
     */
    String getServerCharset();

    /**
     * 获取沙箱版本号
     *
     * @return 沙箱版本号
     */
    String getVersion();


    /**
     *  获取加密密钥
     * @return
     */
    String getEncryptionkey();

    /**
     *  获取登陆用户名
     * @return
     */
    String getUsername();

    /**
     *  获取登陆密码
     * @return
     */
    String getPassword();

    /**
     *  设置加密密钥
     */
    void setEncryptionkey(String encryptionkey);

    /**
     *  设置登陆用户名
     */
    void setUsername(String username);

    /**
     *  设置登陆密码
     */
    void setPassword(String password);

}
