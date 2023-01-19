package com.jrasp.agent.launcher110;

import org.junit.Test;

import java.util.Map;

public class AgentLauncherTest {

    @Test
    public void toFeatureMap() {
        String key = "url";
        String value = "http://localhost:8080/index?requestId=1000";
        final String url = key + "=" + value + ";";
        Map<String, String> stringStringMap = AgentLauncher.toFeatureMap(url);
        assert stringStringMap.size() == 1 && value.equals(stringStringMap.get(key));
    }
}
