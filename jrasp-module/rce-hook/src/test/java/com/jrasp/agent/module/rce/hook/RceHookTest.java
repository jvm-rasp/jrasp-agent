package com.jrasp.agent.module.rce.hook;

import org.junit.Test;

public class RceHookTest {

    // 测试无参数命令
    @Test
    public void testUnixCommand1() {
        String cmdOrigin = "whoami";
        // command、args 为上面的命令在执行时native forkAndExec方法的参数
        byte[] command = new byte[]{119, 104, 111, 97, 109, 105, 0}; // 末尾为0
        byte[] args = new byte[0];

        String cmd = RceHook.unixCommand(command, args);

        assert cmdOrigin.equals(cmd);
    }

    // 测试多参数命令
    @Test
    public void testUnixCommand2() {
        String cmdOrigin = "touch /tmp/1.txt /tmp/2.txt /tmp/3.txt /tmp/4.txt /tmp/5.txt";
        // command、args 为上面的命令在执行时forkAndExec方法的参数
        byte[] command = new byte[]{116, 111, 117, 99, 104, 0};  // 末尾为0
        byte[] args = new byte[]{
                47, 116, 109, 112, 47, 49, 46, 116, 120, 116, 0, // /tmp/1.txt
                47, 116, 109, 112, 47, 50, 46, 116, 120, 116, 0, // /tmp/2.txt
                47, 116, 109, 112, 47, 51, 46, 116, 120, 116, 0, // /tmp/3.txt
                47, 116, 109, 112, 47, 52, 46, 116, 120, 116, 0, // /tmp/4.txt
                47, 116, 109, 112, 47, 53, 46, 116, 120, 116, 0  // /tmp/5.txt
        };
        String cmd = RceHook.unixCommand(command, args);

        assert cmdOrigin.equals(cmd);
    }

    // TODO windows上命令执行

}
