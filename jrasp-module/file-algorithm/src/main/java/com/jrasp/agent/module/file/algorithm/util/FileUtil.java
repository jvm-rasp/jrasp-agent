package com.jrasp.agent.module.file.algorithm.util;

import java.io.File;
import java.io.IOException;

public class FileUtil {

    public static String getRealPath(File file) {
        String absPath = file.getAbsolutePath();
        if (OSUtil.isWindows()) {
            int index = absPath.indexOf("::$");
            if (index >= 0) {
                file = new File(absPath.substring(0, index));
            }
        }
        try {
            return file.getCanonicalPath();
        } catch (IOException e) {
            return absPath;
        }
    }
}
