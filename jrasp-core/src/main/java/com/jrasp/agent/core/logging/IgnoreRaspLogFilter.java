package com.jrasp.agent.core.logging;

import java.util.logging.Filter;
import java.util.logging.LogRecord;

/**
 * @author jrasp
 */
public class IgnoreRaspLogFilter implements Filter {

    // com.jrasp 不输出
    private static final String RASP_CLASS_NAME = "com.jrasp.";

    /**
     * Check if a given log record should be published.
     *
     * @param record a LogRecord
     * @return true if the log record should be published.
     */
    @Override
    public boolean isLoggable(LogRecord record) {
        if (record != null) {
            String sourceClassName = record.getSourceClassName();
            if (!"".equals(sourceClassName) && sourceClassName.startsWith(RASP_CLASS_NAME)) {
                return false;
            }
        }
        return true;
    }
}
