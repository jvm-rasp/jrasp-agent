package com.jrasp.api.listener.ext;

import com.jrasp.api.filter.Filter;

public interface EventWatchCondition {
    Filter[] getOrFilterArray();
}
