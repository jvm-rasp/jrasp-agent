package com.jrasp.agent.module.sql.algorithm.impl.checker;

public enum PolicyAction {

    IGNORE, LOG, BLOCK;

    public boolean isIgnore() {
        return equals(IGNORE);
    }

    public boolean isBlock() {
        return equals(BLOCK);
    }

    public boolean isLog() {
        return equals(LOG);
    }

    public static PolicyAction parseAction(String action) {
        action = action.toLowerCase();
        if (action.equals("log")) {
            return LOG;
        }
        if (action.equals("block")) {
            return BLOCK;
        }
        return IGNORE;
    }
}
