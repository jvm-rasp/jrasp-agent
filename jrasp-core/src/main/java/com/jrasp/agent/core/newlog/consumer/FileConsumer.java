package com.jrasp.agent.core.newlog.consumer;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 日志消费：写本地文件
 *  @author jrasp
 */
public class FileConsumer extends AbstractConsumer {

    // 文件大小50M
    private static final int logSize = 50 * 1024 * 1024;

    private String logFile = null;

    private static final String LOG_NAME = "jrasp-agent.log";

    public FileConsumer(String logPath) {
        this.logFile = logPath + File.separator + LOG_NAME;
    }

    /**
     * 消费队列，写入文件或者日志
     *
     * @param msg
     */
    @Override
    public void consumer(String msg) throws Exception {
        publish(msg);
    }

    private void publish(String msg) throws Exception {
        PrintWriter out = null;
        try {
            File file = getLogFile();
            out = new PrintWriter(new BufferedWriter(new FileWriter(file, true)));
            // TODO 一次写入多个
            out.println(msg);
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    public synchronized List<String> syncOfflineLogs() throws Exception {
        List<String> list = null;
        File file = getLogFile();
        try {
            InputStreamReader inputStreamReader = new InputStreamReader(new FileInputStream(file), "GBK");
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            String content = null;
            list = new ArrayList<String>();
            while ((content = bufferedReader.readLine()) != null) {
                list.add(content);
            }
            bufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        recreateLogFile(file);
        return list;
    }


    private void recreateLogFile() throws Exception {
        File file = new File(logFile);
        if (file.exists()) {
            file.delete();
        }
    }

    private File getLogFile() throws IOException {
        File file = new File(logFile);
        if (!file.exists()) {
            createNewLogFile(file);
        } else if (file.length() >= logSize) {
            recreateLogFile(file);
        }
        return file;
    }

    private void createNewLogFile(File file) throws IOException {
        File parent = file.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
        file.createNewFile();
    }

    private void recreateLogFile(File file) throws IOException {
        file.delete();
        file.createNewFile();
    }

}
