package com.jrasp.core;

import com.sun.tools.attach.VirtualMachine;
import org.apache.commons.lang3.StringUtils;

import static com.jrasp.core.util.RASPStringUtils.getCauseMessage;

/**
 * 沙箱内核启动器
 */
public class CoreLauncher {


    public CoreLauncher(final String targetJvmPid,
                        final String agentJarPath,
                        final String token) throws Exception {

        // 加载agent
        attachAgent(targetJvmPid, agentJarPath, token);

    }

    /**
     * 内核启动程序
     *
     * @param args 参数
     *             [0] : PID
     *             [1] : agent.jar's value
     *             [2] : token
     */
    public static void main(String[] args) {
        try {

            // check args
            if (args.length != 3
                    || StringUtils.isBlank(args[0])
                    || StringUtils.isBlank(args[1])
                    || StringUtils.isBlank(args[2])) {
                throw new IllegalArgumentException("illegal args");
            }

            new CoreLauncher(args[0], args[1], args[2]);
        } catch (Throwable t) {
            t.printStackTrace(System.err);
            System.err.println("jrasp load jvm failed : " + getCauseMessage(t));
            System.exit(-1);
        }
    }

    // 加载Agent
    private void attachAgent(final String targetJvmPid,
                             final String agentJarPath,
                             final String cfg) throws Exception {

        VirtualMachine vmObj = null;
        try {

            vmObj = VirtualMachine.attach(targetJvmPid);
            if (vmObj != null) {
                vmObj.loadAgent(agentJarPath, cfg);
            }

        } catch (Exception e) {
            String message = e.getMessage();
            if (message != null) {
                if (message.contains("Non-numeric value found") ||message.contains("0")) {
                    // https://stackoverflow.com/questions/54340438/virtualmachine-attach-throws-com-sun-tools-attach-agentloadexception-0-when-usi
                    System.out.println("[info] It seems to use the lower version of JDK to attach the higher version of JDK.");
                }
            } else {
                throw e;
            }
        } finally {
            if (null != vmObj) {
                vmObj.detach();
            }
        }

    }

}
