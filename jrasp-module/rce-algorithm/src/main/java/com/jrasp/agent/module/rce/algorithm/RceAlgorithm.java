package com.jrasp.agent.module.rce.algorithm;

import com.jrasp.agent.api.*;
import com.jrasp.agent.api.algorithm.Algorithm;
import com.jrasp.agent.api.algorithm.AlgorithmManager;
import com.jrasp.agent.api.annotation.Information;
import com.jrasp.agent.api.annotation.RaspResource;
import com.jrasp.agent.api.log.RaspLog;
import com.jrasp.agent.api.request.AttackInfo;
import com.jrasp.agent.api.request.Context;
import com.jrasp.agent.api.util.ParamSupported;
import com.jrasp.agent.api.util.StackTrace;
import org.kohsuke.MetaInfServices;

import java.util.*;

@MetaInfServices(Module.class)
@Information(id = "rce-algorithm", author = "jrasp")
public class RceAlgorithm extends ModuleLifecycleAdapter implements Module, Algorithm {

    @RaspResource
    private AlgorithmManager algorithmManager;

    @RaspResource
    private RaspLog logger;

    @RaspResource
    private RaspConfig raspConfig;

    @RaspResource
    private String metaInfo;


    private volatile Integer rceAction = 0;

    /**
     * 命令执行白名单
     */
    private volatile Set<String> rceWhiteSet = new HashSet<String>();

    /**
     * 命令执行黑名单
     */
    private volatile List<String> rceBlockList = Arrays.asList("curl", "wget", "echo", "touch", "gawk", "telnet",
            "xterm", "perl", "python", "python3", "ruby", "lua", "whoami", "dir", "ls", "ping", "ip", "cat",
            "type", "php", "pwd", "ifconfig", "ipconfig", "alias", "export", "nc", "crontab", "find", "wmic", "net",
            "tac", "more", "bzmore", "less", "bzless", "head", "tail", "nl", "sed", "sort", "uniq", "rev", "od", "vim",
            "vi", "man", "paste", "grep", "file", "dd", "systeminfo", "findstr", "tasklist", "netstat", "netsh",
            "powershell", "for", "arp", "quser", "chmod", "useradd", "hostname", "pwd", "cd", "cp", "mv", "history",
            "tar", "zip", "route", "uname", "id", "passwd", "rpm", "dmesg", "env", "ps", "top", "dpkg", "ss", "lsof",
            "chkconfig", "/bin/sh", "/bin/bash"
    );

    private Set<String> rceDangerStackSet = new HashSet<String>(Arrays.asList(
            "com.thoughtworks.xstream.XStream.unmarshal",
            "java.beans.XMLDecoder.readObject",
            "java.io.ObjectInputStream.readObject",
            "org.apache.dubbo.common.serialize.hessian2.Hessian2ObjectInput.readObject",
            "com.alibaba.fastjson.JSON.parse",
            "com.fasterxml.jackson.databind.ObjectMapper.readValue",
            "payload.execCommand",
            "net.rebeyond.behinder",
            "org.springframework.expression.spel.support.ReflectiveMethodExecutor.execute",
            "freemarker.template.utility.Execute.exec",
            "freemarker.core.Expression.eval",
            "bsh.Reflect.invokeMethod",
            "org.jboss.el.util.ReflectionUtil.invokeMethod",
            "org.codehaus.groovy.runtime.ProcessGroovyMethods.execute",
            "org.codehaus.groovy.runtime.callsite.AbstractCallSite.call",
            "ScriptFunction.invoke",
            "com.caucho.hessian.io.HessianInput.readObject",
            "org.apache.velocity.runtime.parser.node.ASTMethod.execute",
            "org.apache.commons.jexl3.internal.Interpreter.call",
            "javax.script.AbstractScriptEngine.eval",
            "javax.el.ELProcessor.getValue",
            "ognl.OgnlRuntime.invokeMethod",
            "javax.naming.InitialContext.lookup",
            "org.mvel2.MVEL.executeExpression",
            "org.mvel.MVEL.executeExpression",
            "ysoserial.Pwner",
            "org.yaml.snakeyaml.Yaml.load",
            "org.mozilla.javascript.Context.evaluateString",
            "command.Exec.equals",
            "java.lang.ref.Finalizer.runFinalizer",
            "java.sql.DriverManager.getConnection"
    ));

    // 防止误报，可以加上栈白名单
    private Set<String> rceWhiteStackSet = new HashSet<String>(

    );

    @Override
    public boolean update(Map<String, String> configMaps) {
        this.rceAction = ParamSupported.getParameter(configMaps, "rce_action", Integer.class, rceAction);
        this.rceWhiteSet = ParamSupported.getParameter(configMaps, "rce_white_list", Set.class, rceWhiteSet);
        this.rceBlockList = ParamSupported.getParameter(configMaps, "rce_block_list", List.class, rceBlockList);
        this.rceDangerStackSet = ParamSupported.getParameter(configMaps, "rce_danger_stack_list", Set.class, rceDangerStackSet);
        this.rceWhiteStackSet = ParamSupported.getParameter(configMaps, "rce_white_stack_list", Set.class, rceWhiteStackSet);
        return true;
    }

    @Override
    public String getType() {
        return "rce";
    }

    @Override
    public void check(Context context, Object... parameters) throws Exception {
        if (rceAction > -1) {

            // 过滤白栈名单
            String[] stacks = StackTrace.getStackTraceString();
            if (isStackContainsWhiteKey(stacks)) {
                return;
            }

            // 过滤命令白名单
            String cmd = (String) parameters[0];
            List<String> tokens = getTokens(cmd);
            String javaCmd = tokens.get(0);
            if (rceWhiteSet.contains(javaCmd)) {
                return;
            }

            // jsp 命令执行
            if (context != null && context.fromJsp()) {
                doActionCtl(rceAction, context, cmd, "Command execution from jsp", cmd, "JSP命令执行", 80);
                return;
            }

            // 检测算法1：检测WebShell管理工具命令执行
            if (isBehinderRealCMDBackdoor(stacks, cmd)) {
                doActionCtl(1, context, cmd, "Behinder RealCMD backdoor", cmd, "冰蝎命令执行后门", 100);
                return;
            }
            if (isBehinderRunCMDBackdoor(stacks)) {
                doActionCtl(1, context, cmd, "Behinder RunCMD backdoor", cmd, "冰蝎命令执行后门", 100);
                return;
            }
            if (isGodzillaExecCommandBackdoor(stacks)) {
                doActionCtl(1, context, cmd, "Godzilla exeCommand backdoor", cmd, "哥斯拉命令执行后门", 100);
                return;
            }

            // 检测算法2： 用户输入后门
            // 用户命令是否包含在参数列表中
            if (context != null) {
                String includeParameter = include(context.getParametersString(), tokens);
                if (includeParameter != null) {
                    doActionCtl(rceAction, context, cmd, "rce token contains in http parameters", includeParameter, "command inject", 80);
                    return;
                }
                String includeHeader = include(context.getHeaderString(), tokens);
                if (includeHeader != null) {
                    doActionCtl(rceAction, context, cmd, "rce token contains in http headers", includeHeader, "command inject", 80);
                    return;
                }
            }

            //  检测算法3： 包含敏感字符
            for (String token : tokens) {
                if (rceBlockList.contains(token)) {
                    doActionCtl(rceAction, context, cmd, "java cmd [" + token + "] in black list.", cmd, "command inject", 80);
                    return;
                }
            }

            // 检测算法4： 栈特征
            String[] stackTraceString = StackTrace.getStackTraceString(100, false);
            for (String stack : stackTraceString) {
                if (rceDangerStackSet.contains(stack)) {
                    doActionCtl(rceAction, context, cmd, "danger rce stack: " + stack, cmd, "command inject ", 90);
                    return;
                }
            }

            // 检测算法5：命令执行监控
            doActionCtl(rceAction, context, cmd, "log all rce", cmd, "command inject", 50);
        }
    }

    // 白名单直接跳过检测
    private boolean isStackContainsWhiteKey(String[] currentStack) {
        if (currentStack == null || currentStack.length == 0) {
            return false;
        }
        for (String stack : currentStack) {
            for (String keyword : rceWhiteStackSet) {
                if (stack.contains(keyword)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isBehinderRealCMDBackdoor(String[] stacks, String cmd) {
        for (String stack : stacks) {
            if (stack.contains("RealCMD")) {
                return true;
            }
        }
        if (stacks.length == 6) {
            if (stacks[4].contains("run(") && stacks[5].contains("Thread.run")) {
                return true;
            }
        }
        if (cmd.equals("cmd.exe")) {
            return true;
        }
        return false;
    }

    private boolean isBehinderRunCMDBackdoor(String[] stacks) {
        for (int i = 0; i < stacks.length; i++) {
            if (stacks[i].contains("RunCMD(Cmd") && stacks.length > i + 1 && stacks[i + 1].contains("equals(Cmd")) {
                return true;
            }
        }
        return false;
    }

    private boolean isGodzillaExecCommandBackdoor(String[] stacks) {
        boolean flag1 = false;
        boolean flag2 = false;
        boolean flag3 = false;
        for (String stack : stacks) {
            if (stack.contains("execCommand(payload")) {
                flag1 = true;
            } else if (stack.contains("run(payload")) {
                flag2 = true;
            } else if (stack.contains("toString(payload")) {
                flag3 = true;
            }
        }
        return flag1 && flag2 && flag3;
    }

    private String include(String httpParameters, List<String> cmdArgs) {
        if (httpParameters != null) {
            for (String item : cmdArgs) {
                if (httpParameters.contains(item)) {
                    return item;
                }
            }
        }
        return null;
    }

    private void doActionCtl(int action, Context context, String cmd, String checkType, String message, String attackType, int level) throws ProcessControlException {
        if (action > -1) {
            boolean enableBlock = action == 1;
            AttackInfo attackInfo = new AttackInfo(context, metaInfo, cmd, enableBlock, attackType, checkType, message, level);
            logger.attack(attackInfo);
            if (enableBlock && !checkType.equals("log all rce")) {
                ProcessController.throwsImmediatelyAndSendResponse(attackInfo, raspConfig, new RuntimeException("rce block by JRASP."));
            }
        }
    }

    @Override
    public String getDescribe() {
        return "rce check algorithm";
    }

    public static List<String> getTokens(String str) {
        List<String> tokens = new ArrayList<String>();
        // 修改为不包含分割字符
        StringTokenizer tokenizer = new StringTokenizer(str, "\t\n\r\f\";|& ", false);
        while (tokenizer.hasMoreElements()) {
            tokens.add(tokenizer.nextToken());
        }
        return tokens;
    }

    @Override
    public void loadCompleted() {
        algorithmManager.register(this);
    }

}
