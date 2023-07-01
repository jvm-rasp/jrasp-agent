package com.jrasp.agent.api.request;

import com.jrasp.agent.api.util.EscapeUtil;
import com.jrasp.agent.api.util.StackTrace;

import java.util.Arrays;

import static com.jrasp.agent.api.util.StringUtils.array2String;
import static com.jrasp.agent.api.util.StringUtils.escape;

/**
 * 攻击特征信息
 *
 * @author jrasp
 */
public class AttackInfo {
    /**
     * 上下文 信息
     */
    private Context context;

    /**
     * Java 应用名称
     */
    private String appName;

    /**
     * 模块元数据信息
     */
    private String metaInfo = "";

    /**
     * 栈
     */
    private String[] stackTrace;

    /**
     * 参数
     */
    private String payload;

    /**
     * 是否阻断
     */
    private boolean isBlocked;

    /**
     * 攻击类型：rce、sql-inject、spel
     */
    private String attackType;

    /**
     * 检测算法
     */
    private String algorithm;

    /**
     * 扩展字段，非必需
     */
    private String extend;

    /**
     * 攻击时间
     * unix 时间
     */
    private long attackTime;

    /**
     * 风险等级 0～100
     */
    private int level = 0;

    public AttackInfo(Context context, String metaInfo, String payload, boolean isBlocked, String attackType, String algorithm, String extend, int level) {
        this.context = context;
        this.metaInfo = metaInfo;
        this.stackTrace = StackTrace.getStackTraceString();
        this.payload = payload;
        this.isBlocked = isBlocked;
        this.attackType = attackType;
        this.attackTime = System.currentTimeMillis();
        this.algorithm = algorithm;
        this.extend = extend;
        this.level = level;
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String[] getStackTrace() {
        return stackTrace;
    }

    public void setStackTrace(String[] stackTrace) {
        this.stackTrace = stackTrace;
    }

    public boolean isBlocked() {
        return isBlocked;
    }

    public void setBlocked(boolean blocked) {
        isBlocked = blocked;
    }

    public String getAttackType() {
        return attackType;
    }

    public void setAttackType(String attackType) {
        this.attackType = attackType;
    }

    public long getAttackTime() {
        return attackTime;
    }

    public void setAttackTime(long attackTime) {
        this.attackTime = attackTime;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public String getExtend() {
        return extend;
    }

    public void setExtend(String extend) {
        this.extend = extend;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    /**
     * TODO 因为不使用任何json工具，这里需要手动实现json拼接
     * 原因：
     * 1.各自json工具，都有大量漏洞
     * 2.json工具的使用可能导致内存泄漏
     *
     * @return
     */
    public String toJSON() {
        final StringBuilder sb = new StringBuilder("{");
        sb.append("\"context\":")
                .append(context.toJSON());
        sb.append(",\"appName\":\"")
                .append(appName).append('\"');
        sb.append(",\"metaInfo\":\"")
                .append(metaInfo).append('\"');
        sb.append(",\"stackTrace\":\"")
                .append(array2String(stackTrace).replace("\\", "\\\\")).append('\"');
        sb.append(",\"payload\":")
                // 转义符号
                .append(EscapeUtil.quote(payload));
        sb.append(",\"isBlocked\":")
                .append(isBlocked);
        sb.append(",\"attackType\":\"")
                .append(escape(attackType)).append('\"');
        sb.append(",\"algorithm\":\"")
                .append(escape(algorithm)).append('\"');
        sb.append(",\"extend\":\"")
                .append(escape(extend.replace("\\", "\\\\").replace("\"", "\\\""))).append('\"');
        sb.append(",\"attackTime\":")
                .append(attackTime);
        sb.append(",\"level\":")
                .append(level);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public String toString() {
        return "AttackInfo{" +
                "context=" + context +
                "appName=" + appName +
                ", stackTrace=" + Arrays.toString(stackTrace) +
                ", payload='" + payload + '\'' +
                ", isBlocked=" + isBlocked +
                ", attackType='" + attackType + '\'' +
                ", algorithm='" + algorithm + '\'' +
                ", extend='" + extend + '\'' +
                ", attackTime=" + attackTime +
                ", level=" + level +
                '}';
    }
}
