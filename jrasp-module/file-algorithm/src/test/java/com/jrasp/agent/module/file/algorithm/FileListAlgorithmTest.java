package com.jrasp.agent.module.file.algorithm;

import com.jrasp.agent.module.file.algorithm.impl.FileListAlgorithm;
import org.junit.Test;

import java.util.List;

public class FileListAlgorithmTest {

    @Test
    public void testGetToken1() {
        String parameter = "1.txt";
        String path = "/usr/local/1.txt";
        List<String> tokens = FileListAlgorithm.getTokens(path);
        String include = FileListAlgorithm.include(parameter, tokens);
        assert include != null;
    }

    @Test
    public void testGetToken2() {
        String parameter = "cmd.vbs";
        String path = "C:\\windows\\cacc\\cmd.vbs";
        List<String> tokens = FileListAlgorithm.getTokens(path);
        String include = FileListAlgorithm.include(parameter, tokens);
        assert include != null;
    }

}
