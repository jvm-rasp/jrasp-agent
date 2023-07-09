package com.jrasp.agent.module.rce.hook;

import com.jrasp.agent.api.LoadCompleted;
import com.jrasp.agent.api.Module;
import com.jrasp.agent.api.ModuleLifecycleAdapter;
import com.jrasp.agent.api.algorithm.AlgorithmManager;
import com.jrasp.agent.api.annotation.Information;
import com.jrasp.agent.api.annotation.RaspResource;
import com.jrasp.agent.api.annotation.RaspValue;
import com.jrasp.agent.api.listener.Advice;
import com.jrasp.agent.api.listener.AdviceListener;
import com.jrasp.agent.api.matcher.ClassMatcher;
import com.jrasp.agent.api.matcher.EventWatchBuilder;
import com.jrasp.agent.api.matcher.MethodMatcher;
import com.jrasp.agent.api.matcher.ModuleEventWatcher;
import com.jrasp.agent.api.request.Context;
import com.jrasp.agent.api.util.StringUtils;
import org.kohsuke.MetaInfServices;

import java.util.*;

import static com.jrasp.agent.api.util.ParamSupported.getParameter;

/**
 * 命令执行方法hook模块
 * hook类是最底层的native方法,不可能绕过
 * 业内第一款能彻底防止绕过的rasp
 * 2023.7.9 验证通过
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

    @RaspValue(name = "disable", value = "false")
    private volatile Boolean disable;

    private final static String TYPE = "rce";

    @Override
    public boolean update(Map<String, String> configMaps) {
        this.disable = getParameter(configMaps, "disable", Boolean.class, disable);
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
                /**
                 * @see java.lang.UNIXProcess#forkAndExec
                 */
                .onClass(new ClassMatcher("java/lang/UNIXProcess").onMethod(new MethodMatcher("forkAndExec(I[B[B[BI[BI[B[IZ)I", new UnixCommandAdviceListener())))
                /**
                 * @see java.lang.ProcessImpl#forkAndExec
                 */
                .onClass(new ClassMatcher("java/lang/ProcessImpl").onMethod(new MethodMatcher("forkAndExec(I[B[B[BI[BI[B[IZ)I", new UnixCommandAdviceListener())))
                /**
                 * @see java.lang.ProcessImpl#create
                 */
                .onClass(new ClassMatcher("java/lang/ProcessImpl").onMethod(new MethodMatcher("create(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;[JZ)J", new WindowsCommandAdviceListener()))).build();
    }

    public class UnixCommandAdviceListener extends AdviceListener {
        @Override
        protected void before(Advice advice) throws Throwable {
            if (disable) {
                return;
            }
            byte[] prog = (byte[]) advice.getParameterArray()[2];     // 命令
            byte[] argBlock = (byte[]) advice.getParameterArray()[3]; // 参数
            String cmd = unixCommand(prog, argBlock);
            algorithmManager.doCheck(TYPE, context.get(), cmd);
        }

        @Override
        protected void afterThrowing(Advice advice) throws Throwable {
            context.remove();
        }
    }

    public class WindowsCommandAdviceListener extends AdviceListener {
        @Override
        protected void before(Advice advice) throws Throwable {
            if (disable) {
                return;
            }
            String cmd = (String) advice.getParameterArray()[0];     // 命令+参数
            algorithmManager.doCheck(TYPE, context.get(), cmd);
        }
    }

    /**
     * 参数转换参考 open-rasp 有改动
     *
     * @param command
     * @param args
     * @return
     */
    public static String unixCommand(byte[] command, byte[] args) {
        List<String> commands = new ArrayList<String>();

        // 命令
        if (command != null && command.length > 0) {
            /**
             * 去掉最后一个byte的原因
             * toCString 方法会给字符串末尾追加一个(byte)0
             * 因此cmd字符串的范围: [0,command.length - 1), 因为command最后一位为\u0000字符需要去掉
             * @see java.lang.ProcessImpl#toCString(String)
             * @see java.lang.UNIXProcess#toCString(String)
             */
            commands.add(new String(command, 0, command.length - 1));
        }

        // 参数
        if (args != null && args.length > 0) {
            int position = 0;
            for (int i = 0; i < args.length; i++) {
                // (byte)0 为字符串参数的分割符号
                if (args[i] == 0) {
                    commands.add(new String(Arrays.copyOfRange(args, position, i)));
                    position = i + 1;
                }
            }
        }
        // 数组转成字符串，方便检测引擎统一处理
        return StringUtils.join(commands, " ");
    }

}
