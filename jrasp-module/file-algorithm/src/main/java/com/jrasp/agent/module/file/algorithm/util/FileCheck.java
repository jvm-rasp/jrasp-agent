package com.jrasp.agent.module.file.algorithm.util;

public class FileCheck {

    public static boolean isFromUserInput(String[] params, String target) {
        for (String param : params) {
            if (param.equals(target))
                return true;
        }
        return false;
    }

    public static boolean isPathEndWithUserInput(String[] allParams, String path, String realPath, boolean isLcsSearch) {
        boolean isWindows = OSUtil.isWindows();
        boolean isAbsolutePath = isAbsolutePath(path, isWindows);
        String cachePath = path;
        for (String param : allParams) {
            String simpleParam;
            String simpleTarget;
            if (param.startsWith("file://") && isAbsolutePath && param.endsWith(cachePath)) {
                return true;
            }
            if (isWindows) {
                param = param.replace('/', '\\');
                path = path.replace('/', '\\');
                realPath = realPath.replace('/', '\\');
                simpleParam = param.replace("\\\\", "\\").replace("\\.\\", "\\");
                simpleTarget = path.replace("\\\\", "\\").replace("\\.\\", "\\");
            } else {
                simpleParam = param.replace("//", "/").replace("/./", "/");
                simpleTarget = path.replace("//", "/").replace("/./", "/");
            }
            if ((path.endsWith(param) || simpleTarget.endsWith(simpleParam)) && (
                    paramsHasTraversal(param) || param.equals(realPath) || simpleParam.equals(realPath))) {
                return true;
            }
        }
        return false;
    }

    private static boolean paramsHasTraversal(String param) {
        String path = "/" + param.replace('\\', '/') + "/";
        return (path.contains("/../"));
    }

    private static boolean isAbsolutePath(String path, boolean isWindows) {
        if (isWindows) {
            if (path.length() > 1 && path.charAt(1) == ':') {
                char drive = Character.toLowerCase(path.charAt(0));
                return drive >= 'a' && drive <= 'z';
            }
            return false;
        }
        return (path.charAt(0) == '/');
    }
}
