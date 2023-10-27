package com.jrasp.agent.module.response.algorithm;

import com.jrasp.agent.api.*;
import com.jrasp.agent.api.algorithm.Algorithm;
import com.jrasp.agent.api.algorithm.AlgorithmManager;
import com.jrasp.agent.api.annotation.Information;
import com.jrasp.agent.api.annotation.RaspResource;
import com.jrasp.agent.api.log.RaspLog;
import com.jrasp.agent.api.request.AttackInfo;
import com.jrasp.agent.api.request.Context;
import com.jrasp.agent.api.util.ParamSupported;
import org.kohsuke.MetaInfServices;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author jrasp
 * 检测算法来源于open-rasp
 */
@MetaInfServices(Module.class)
@Information(id = "repsponse-algorithm", author = "jrasp")
public class ResponseAlgorithm extends ModuleLifecycleAdapter implements Module, Algorithm {

    @RaspResource
    private AlgorithmManager algorithmManager;

    @RaspResource
    private RaspLog logger;

    @RaspResource
    private RaspConfig raspConfig;

    @RaspResource
    private String metaInfo;

    private volatile Integer responseAction = 0;

    // 同步检测，默认为false
    private volatile boolean checkSync = false;

    private final int queueSize = 1000; // 指定队列大小

    private final int poolSize = 5; // 指定线程池大小

    private ArrayBlockingQueue<Runnable> queue = new ArrayBlockingQueue<Runnable>(queueSize);

    private ThreadFactory threadFactory = new ThreadFactory() {
        private AtomicInteger count = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r);
            thread.setName("jrasp-body-check-thread-" + count.getAndIncrement());
            thread.setPriority(Thread.NORM_PRIORITY);
            return thread;
        }
    };

    private RejectedExecutionHandler handler = new RejectedExecutionHandler() {

        private AtomicInteger count = new AtomicInteger(1);

        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            int cnt = count.getAndIncrement();
            if (cnt % 1000 == 0) {
                logger.warning("body check queue is over, reject body cnt:" + cnt);
            }
        }
    };

    ExecutorService executor = new ThreadPoolExecutor(poolSize, poolSize, 0L, TimeUnit.MILLISECONDS, queue, threadFactory, handler); // 创建线程池

    @Override
    public boolean update(Map<String, String> configMaps) {
        this.responseAction = ParamSupported.getParameter(configMaps, "response_action", Integer.class, responseAction);
        this.checkSync = ParamSupported.getParameter(configMaps, "check_sync", boolean.class, checkSync);
        return true;
    }

    @Override
    public String getType() {
        return "response";
    }

    @Override
    public void check(final Context context, Object... parameters) throws Exception {
        if (responseAction > -1 && parameters != null && parameters.length > 0 && parameters[0] instanceof Map) {
            final Map<String, Object> map = (Map<String, Object>) parameters[0];
            final String data = (String) map.get("content");

            // 同步检测
            if (checkSync) {
                doCheck(context, data);
                return;
            }

            // 异步场景
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        doCheck(context, data);
                    } catch (Exception e) {
                        // 异步不抛出阻断异常
                    }
                }
            });
        }
    }

    private void doCheck(Context context, String data) throws ProcessControlException {
        Map<String, String> result = checkBody(data);
        if (result != null) {
            boolean enableBlock = responseAction == 1 && checkSync;
            AttackInfo attackInfo = new AttackInfo(context, metaInfo, null, result.get("match"), enableBlock, result.get("type"), "response check", result.get("parts"), 60);
            logger.attack(attackInfo);
            if (enableBlock) {
                ProcessController.throwsImmediatelyAndSendResponse(attackInfo, raspConfig, new RuntimeException("body contains user info block by JRASP."));
            }
        }
    }

    @Override
    public String getDescribe() {
        return "response check algorithm";
    }

    @Override
    public void loadCompleted() {
        algorithmManager.register(this);
    }

    @Override
    public void onUnload() throws Throwable {
        if (executor != null) {
            executor.shutdown();
            executor = null;
        }
        if (queue != null) {
            queue.clear();
            queue = null;
        }
    }

    private static Map<String, String> checkBody(String data) {

        // 检查身份证
        Map<String, String> firstIdentityCard = findFirstIdentityCard(data);
        if (firstIdentityCard != null) {
            return firstIdentityCard;
        }

        //  手机号
        Map<String, String> findFirstMobileNumber = findFirstMobileNumber(data);
        if (findFirstMobileNumber != null) {
            return findFirstMobileNumber;
        }
        // 银行卡号
        Map<String, String> firstBankCard = findFirstBankCard(data);
        if (firstBankCard != null) {
            return firstBankCard;
        }
        return null;
    }

    private final static Pattern regexChineseId = Pattern.compile("(?<!\\d)\\d{10}(?:[01]\\d)(?:[0123]\\d)\\d{3}(?:\\d|x|X)(?!\\d)");

    private final static int[] word = {7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2};

    public static Map<String, String> findFirstIdentityCard(String data) {

        Matcher m = regexChineseId.matcher(data);
        if (m.find()) {
            String id = m.group(0);
            // FIXME: 简单处理 springboot 接口误报问题
            if (id.charAt(0) == '0') {
                return null;
            }

            int sum = 0;
            for (int i = 0; i < word.length; i++) {
                sum += (id.charAt(i) - '0') * word[i];
            }
            if (id.charAt(17) == 'X' || id.charAt(17) == 'x') {
                sum += 10;
            } else {
                sum += id.charAt(17) - '0';
            }
            if (sum % 11 == 1) {
                return getResultInfo(data, m, "Identity Card");
            }
        }
        return null;
    }

    private static Pattern regexChinesePhone = Pattern.compile("(?<!\\w)(?:(?:00|\\+)?86 ?)?(1\\d{2})(?:[ -]?\\d){8}(?!\\w)");

    private static Set<Integer> prefixs = new HashSet<Integer>(Arrays.asList(133, 149, 153, 173, 174, 177, 180,
            181, 189, 199, 130, 131, 132, 145, 146, 155, 156, 166, 175, 176, 185, 186, 134, 135, 136, 137, 138, 139,
            147, 148, 150, 151, 152, 157, 158, 159, 165, 178, 182, 183, 184, 187, 188, 198, 170));

    public static Map<String, String> findFirstMobileNumber(String data) {
        Matcher m = regexChinesePhone.matcher(data);
        if (m.find()) {
            if (prefixs.contains(Integer.parseInt(m.group(1)))) {
                return getResultInfo(data, m, "Mobile Number");
            }
        }
        return null;
    }

    private static Pattern regexBankCard = Pattern.compile("(?<!\\d)(?:62|3|5[1-5]|4\\d)\\d{2}(?:[ -]?\\d{4}){3}(?!\\d)");

    public static Map<String, String> findFirstBankCard(String data) {
        Matcher m = regexBankCard.matcher(data);
        if (m.find()) {
            String card = m.group(0).replaceAll(" |-", "");
            if (luhnCheck(card)) {
                return getResultInfo(data, m, "Bank Card");
            }
        }
        return null;
    }

    private static boolean luhnCheck(String cardNo) {
        int sum = 0;
        int digit = 0;
        int addend = 0;
        boolean timesTwo = false;
        for (int i = cardNo.length() - 1; i >= 0; i--) {
            digit = Integer.parseInt(cardNo.substring(i, i + 1));
            if (timesTwo) {
                addend = digit * 2;
                if (addend > 9) {
                    addend -= 9;
                }
            } else {
                addend = digit;
            }
            sum += addend;
            timesTwo = !timesTwo;
        }
        int modulus = sum % 10;
        return modulus == 0;
    }

    public static Map<String, String> getResultInfo(String data, Matcher m, String type) {
        Map<String, String> result = new HashMap<String, String>();
        result.put("type", type);
        result.put("match", m.group(0));
        int startIndex = Math.max(m.start() - 40, 0);
        if (startIndex < 0) {
            startIndex = 0;
        }
        int endIndex = Math.min(m.end() + 40, data.length());
        result.put("parts", data.substring(startIndex, endIndex));
        return result;
    }

}