package com.jrasp.agent.api;

import java.util.Map;

/**
 * @author jrasp
 */
public interface Module {

    boolean update(Map<String, String> configMaps);

}
