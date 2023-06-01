package com.jrasp.agent.module.sql.algorithm.impl.checker;

import java.util.regex.Pattern;

public class TokenCombine {
    final Pattern[] tokens;

    final int length;

    int index;

    StringBuilder matches = new StringBuilder();

    public static final int INVALID = 0;

    public static final int VALID = 1;

    public static final int MATCHED = 2;

    public TokenCombine(String[] tokens) {
        this.length = tokens.length;
        this.tokens = new Pattern[this.length];
        int i = 0;
        for (String kw : tokens) {
            this.tokens[i] = Pattern.compile(kw);
            i++;
        }
        this.index = 0;
    }

    private TokenCombine(Pattern[] keywords, int index) {
        this.length = keywords.length;
        this.tokens = keywords;
        this.index = index;
    }

    public TokenCombine copy() {
        return new TokenCombine(this.tokens, this.index);
    }

    public int feed(String token) {
        if (this.index >= this.length)
            return 0;
        if (this.tokens[this.index].matcher(token).matches()) {
            this.index++;
            this.matches.append(token);
        } else {
            return 0;
        }
        if (this.index == this.length)
            return 2;
        this.matches.append(" ");
        return 1;
    }

    public String getMatches() {
        return this.matches.toString();
    }
}
