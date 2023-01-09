package com.jrasp.agent.module.rce.hook;

import com.jrasp.agent.api.annotation.Information;
import com.jrasp.agent.api.LoadCompleted;
import com.jrasp.agent.api.Module;
import com.jrasp.agent.api.ModuleLifecycleAdapter;
import com.jrasp.agent.api.algorithm.AlgorithmManager;
import com.jrasp.agent.api.annotation.RaspResource;
import com.jrasp.agent.api.listener.Advice;
import com.jrasp.agent.api.listener.AdviceListener;
import com.jrasp.agent.api.matcher.ClassMatcher;
import com.jrasp.agent.api.matcher.EventWatchBuilder;
import com.jrasp.agent.api.matcher.MethodMatcher;
import com.jrasp.agent.api.matcher.ModuleEventWatcher;
import com.jrasp.agent.api.request.Context;
import org.kohsuke.MetaInfServices;

import java.util.*;

/**
 * 命令执行方法hook模块
 * hook类是最底层的native方法,不可能绕过
 * 业内第一款能彻底防止绕过的rasp
 */
@MetaInfServices(Module.class)
@Information(id = "rce-hook", author = "jrasp")
public class RceHook extends ModuleLifecycleAdapter implements Module, LoadCompleted {

    @RaspResource
    private ModuleEventWatcher moduleEventWatcher;

    @RaspResource
    private ThreadLocal<Context> context;

    @RaspResource
    private AlgorithmManager algorithmManager;

    private volatile Boolean disable = false;

    private final static String TYPE = "rce";

    @Override
    public boolean update(Map<String, String> configMaps) {
        // 是否禁用hook点
        String disableHookStr = configMaps.get("disable");
        this.disable = Boolean.valueOf(disableHookStr);
        return true;
    }

    @Override
    public void loadCompleted() {
        nativeProcessRceHook();
    }

    /**
     * hook类参考：https://www.jrasp.com/guide/hook/rce.html
     */
    public void nativeProcessRceHook() {
        new EventWatchBuilder(moduleEventWatcher)
                .onClass(new ClassMatcher("java/lang/UNIXProcess")
                        .onMethod(new MethodMatcher("forkAndExec(I[B[B[BI[BI[B[IZ)I",
                                new UnixCommandAdviceListener()))
                )
                .onClass(new ClassMatcher("java/lang/ProcessImpl")
                        .onMethod(new MethodMatcher("forkAndExec(I[B[B[BI[BI[B[IZ)I",
                                new UnixCommandAdviceListener()))
                )
                .onClass(new ClassMatcher("java/lang/ProcessImpl")
                        .onMethod(new MethodMatcher("create(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;[JZ)J",
                                new WindowsCommandAdviceListener())))
                .build();
    }

    public class UnixCommandAdviceListener extends AdviceListener {
        @Override
        protected void before(Advice advice) throws Throwable {
            if (disable) {
                return;
            }
            byte[] prog = (byte[]) advice.getParameterArray()[2];     // 命令
            byte[] argBlock = (byte[]) advice.getParameterArray()[3]; // 参数
            String cmd = getCommand(prog);
            String args = getArgs(argBlock);
            algorithmManager.doCheck(TYPE, context.get(), cmd + " " + args);
        }

        @Override
        protected void afterThrowing(Advice advice) throws Throwable {
            // 方法调用完成，如果抛出异常（插桩的代码bug导致的异常或者主动阻断的异常）将清除上下文环境变量
            context.remove();
        }
    }

    public class WindowsCommandAdviceListener extends AdviceListener {
        @Override
        protected void before(Advice advice) throws Throwable {
            if (disable) {
                return;
            }
            String cmdStr = (String) advice.getParameterArray()[0];     // 命令
            algorithmManager.doCheck(TYPE, context.get(), cmdStr);
        }

        @Override
        protected void afterThrowing(Advice advice) throws Throwable {
            // 方法调用完成，如果抛出异常（插桩的代码bug导致的异常或者主动阻断的异常）将清除上下文环境变量
            context.remove();
        }
    }

    /**
     * 命令如何转为字符串，参考jdk的方法即可
     *
     * @param command
     * @return
     */
    public static String getCommand(byte[] command) {
        if (command != null && command.length > 0) {
            // cmd 字符串的范围: [0,command.length - 1), 因为command最后一位为 \u0000 字符，需要去掉
            return new String(command, 0, command.length - 1);
        }
        return "";
    }

    // 参数
    public static String getArgs(byte[] args) {
        StringBuilder stringBuffer = new StringBuilder();
        if (args != null && args.length > 0) {
            int position = 0;
            for (int i = 0; i < args.length; i++) {
                // 空格是字符或者参数的分割符号
                if (args[i] == 0) {
                    stringBuffer.append(new String(Arrays.copyOfRange(args, position, i)));
                    position = i + 1;
                }
            }
        }
        return stringBuffer.toString();
    }

}
