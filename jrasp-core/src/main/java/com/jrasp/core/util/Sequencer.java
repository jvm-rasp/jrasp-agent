package com.jrasp.core.util;

import java.com.jrasp.spy.Spy;

public class Sequencer {

    /**
     * 生成下一条序列
     *
     * @return 下一条序列
     */
    public int next() {
        // 这里直接修改为引用Spy的全局唯一序列，修复 #125
        return Spy.nextSequence();
    }

}
