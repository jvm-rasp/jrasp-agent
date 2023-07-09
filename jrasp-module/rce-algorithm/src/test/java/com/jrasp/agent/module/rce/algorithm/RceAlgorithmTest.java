package com.jrasp.agent.module.rce.algorithm;

import org.junit.Test;

import java.util.List;

import static com.jrasp.agent.module.rce.algorithm.RceAlgorithm.getTokens;

public class RceAlgorithmTest {

    @Test
    public void testGetTokens1() {
        String cmds = "whoami";
        List<String> tokens = getTokens(cmds);
        String[] s = cmds.split(" ");
        assert tokens.size() == s.length;
    }

    @Test
    public void testGetTokens2(){
        String command = "touch /tmp/1.txt /tmp/2.txt /tmp/3.txt /tmp/4.txt /tmp/5.txt";
        List<String> tokens = getTokens(command);
        String[] s = command.split(" ");
        assert tokens.size() == s.length;
    }

    // 检测算法 1

    // 检测算法 2


    // 检测算法 3

    // 检测算法 4

    // 检测算法 5
}
