package com.jrasp.agent.module.sql.hook.checker.util;

import com.jrasp.agent.module.sql.parser.SQLLexer;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.ConsoleErrorListener;
import org.antlr.v4.runtime.Token;

import java.util.LinkedList;
import java.util.List;

public class SQLUtil {

    public static List<TokenInfo> sqlTokenize(String query, String server) {
        CharStream codePointCharStream = new ANTLRInputStream(query);
        SQLLexer lexer = new SQLLexer(codePointCharStream);
        lexer.removeErrorListener(ConsoleErrorListener.INSTANCE);
        List<TokenInfo> tokens = new LinkedList<TokenInfo>();
        for (Token token = lexer.nextToken(); token.getType() != -1; token = lexer.nextToken()) {
            tokens.add(new TokenInfo(token.getStartIndex(), token.getStopIndex(), token.getText()));
        }
        return tokens;
    }
}
