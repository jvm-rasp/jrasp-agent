package com.jrasp.agent.api;

import java.util.Map;

public interface Module {

    boolean update(Map<String, String> configMaps);

}
