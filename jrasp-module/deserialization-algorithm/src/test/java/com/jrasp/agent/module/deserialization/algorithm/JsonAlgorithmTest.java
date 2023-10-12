package com.jrasp.agent.module.deserialization.algorithm;

import com.jrasp.agent.api.ProcessControlException;
import com.jrasp.agent.api.log.RaspLog;
import com.jrasp.agent.api.request.AttackInfo;
import com.jrasp.agent.api.request.Context;
import com.jrasp.agent.module.deserialization.algorithm.impl.JsonAlgorithm;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class JsonAlgorithmTest {

    @Test
    public void test() {
        Map<String, String> configMaps = new HashMap();
        configMaps.put("json_black_list_action", "1");
        JsonAlgorithm jsonAlgorithm = new JsonAlgorithm(new RaspLogImpl(), null, configMaps, "deserialization-algorithm-1.1.2-2023-07-11T16:49:07Z");
        Context context = new Context();
        try {
            jsonAlgorithm.check(context, "com.sun.org.apache.xalan.internal.xsltc.trax.TemplatesImpl");
        } catch (Exception e) {
            // 抛出阻断异常
            assert e instanceof ProcessControlException;
        }
    }

    public static class RaspLogImpl implements RaspLog {

        @Override
        public void attack(AttackInfo attackInfo) {

        }

        @Override
        public void info(String message) {

        }

        @Override
        public void warning(String message) {

        }

        @Override
        public void warning(String message, Throwable t) {

        }

        @Override
        public void error(String message) {

        }

        @Override
        public void error(String message, Throwable t) {

        }

        @Override
        public void info(int logId, String message) {

        }

        @Override
        public void warning(int logId, String message) {

        }

        @Override
        public void error(int logId, String message) {

        }

        @Override
        public void warning(int logId, String message, Throwable t) {

        }

        @Override
        public void error(int logId, String message, Throwable t) {

        }
    }
}
