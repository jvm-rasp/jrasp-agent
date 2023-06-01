package com.jrasp.agent.module.sql.hook.checker.util;

public class TokenInfo {

    public int start;
    public int stop;
    public String text;

    public TokenInfo(int start, int stop, String text) {
        this.start = start;
        this.stop = stop;
        this.text = text;
    }
}
