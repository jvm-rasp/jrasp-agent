package com.jrasp.agent.api.matcher;

import java.util.Set;

/**
 * @author jrasp
 */
public interface ModuleEventWatcher {

    void watch(Set<ClassMatcher> matchers);

    void delete(Set<ClassMatcher> matchers);
}
