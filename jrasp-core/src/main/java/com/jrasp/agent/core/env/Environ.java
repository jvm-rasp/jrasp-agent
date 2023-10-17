package com.jrasp.agent.core.env;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class Environ {

    private static final String CONTAINERENUM_FILE = "/proc/1/cgroup";

    private static final String DOCKER_KEY = "/docker/";

    private static final String K8S_KEY = "/kubepods/";

    private static ContainerEnum container = null;

    public static ContainerEnum getContainerType() {
        if (container != null) {
            return container;
        }

        if (isWindows()) {
            container = ContainerEnum.NOT;
            return container;
        }

        File cgroupFile = new File(CONTAINERENUM_FILE);
        if (!cgroupFile.exists()) {
            container = ContainerEnum.NOT;
            return container;
        }

        try {
            String cgroupInfo = readCgroupFile(cgroupFile);
            if (cgroupInfo.contains(DOCKER_KEY)) {
                container = ContainerEnum.DOCKER;
            } else if (cgroupInfo.contains(K8S_KEY)) {
                container = ContainerEnum.K8S;
            } else {
                container = ContainerEnum.UNKNOWN;
            }
        } catch (IOException e) {
            container = ContainerEnum.UNKNOWN;
        }

        return container;
    }

    private static String readCgroupFile(File filePath) throws IOException {
        StringBuilder content = new StringBuilder();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(filePath));
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append(System.getProperty("line.separator"));
            }
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return content.toString();
    }

    private static String OS = System.getProperty("os.name").toLowerCase();

    private static boolean isWindows() {
        return OS.contains("win");
    }
}