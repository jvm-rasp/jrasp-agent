package com.jrasp.test.vulns.controller;

import org.apache.commons.io.IOUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.*;

import static org.springframework.util.MimeTypeUtils.APPLICATION_JSON_VALUE;
import static org.springframework.util.MimeTypeUtils.APPLICATION_XML_VALUE;

@RestController
@RequestMapping("/CMD/")
public class CMDController {

    public static Map<String, Object> execCMD(String cmd) throws Exception {
        Map<String, Object> data = new LinkedHashMap<String, Object>();

        if (cmd != null) {
            Process process = new ProcessBuilder(cmd.split("\\s+")).start();
            String result = IOUtils.toString(process.getInputStream());
            data.put("result", result);
        } else {
            data.put("msg", "参数不能为空！");
        }

        return data;
    }

    public static Map<String, Object> unixProcess(String cmd) throws Exception {
        Map<String, Object> data = new HashMap<String, Object>();

        if (cmd != null) {
            String[] commands = cmd.split("\\s+");
            Class<?> processClass;

            try {
                processClass = Class.forName("java.lang.UNIXProcess");
            } catch (ClassNotFoundException e) {
                processClass = Class.forName("java.lang.ProcessImpl");
            }

            Constructor<?> constructor = processClass.getDeclaredConstructors()[0];
            constructor.setAccessible(true);
            byte[][] args = new byte[commands.length - 1][];
            int size = args.length;

            for (int i = 0; i < args.length; i++) {
                args[i] = commands[i + 1].getBytes();
                size += args[i].length;
            }

            byte[] argBlock = new byte[size];
            int i = 0;

            for (byte[] arg : args) {
                System.arraycopy(arg, 0, argBlock, i, arg.length);
                i += arg.length + 1;
            }

            byte[] bytes = commands[0].getBytes();
            byte[] resultBytes = new byte[bytes.length + 1];
            System.arraycopy(bytes, 0, resultBytes, 0, bytes.length);
            resultBytes[resultBytes.length - 1] = (byte) 0;

            Type[] parameterTypes = constructor.getGenericParameterTypes();
            List<Object> argList = new ArrayList<Object>();

            Object[] objs = new Object[]{
                    resultBytes, argBlock, args.length, null, 1, null, new int[]{-1, -1, -1}, false
            };

            Collections.addAll(argList, objs);

            if (parameterTypes.length == 9) {
                argList.add(false);
            }

            Object object = constructor.newInstance(argList.toArray(new Object[0]));

            Method inMethod = object.getClass().getDeclaredMethod("getInputStream");
            inMethod.setAccessible(true);

            String result = IOUtils.toString((InputStream) inMethod.invoke(object));

            data.put("result", result);
        }

        return data;
    }

    @GetMapping("/get/cmd.do")
    public Map<String, Object> getProcessBuilder(String cmd) throws Exception {
        return execCMD(cmd);
    }

    @PostMapping("/post/cmd.do")
    public Map<String, Object> postProcessBuilder(String cmd) throws Exception {
        return execCMD(cmd);
    }

    @PostMapping("/cookie/cmd.do")
    public Map<String, Object> cookieProcessBuilder(@CookieValue(name = "cmd") String cmd) throws Exception {
        return execCMD(cmd);
    }

    @PostMapping("/header/cmd.do")
    public Map<String, Object> headerProcessBuilder(@RequestHeader(name = "cmd") String cmd) throws Exception {
        return execCMD(cmd);
    }

    @PostMapping(value = "/xml/cmd.do", consumes = APPLICATION_XML_VALUE)
    public Map<String, Object> xmlProcessBuilder(@RequestBody Map<String, Object> map) throws Exception {
        return execCMD((String) map.get("cmd"));
    }

    @PostMapping(value = "/json/cmd.do", consumes = APPLICATION_JSON_VALUE)
    public Map<String, Object> jsonProcessBuilder(@RequestBody Map<String, Object> map) throws Exception {
        return execCMD((String) map.get("cmd"));
    }

    @PostMapping("/form/cmd.do")
    public Map<String, Object> multipartProcessBuilder(MultipartFile file) throws Exception {
        return execCMD(file.getOriginalFilename());
    }

    @GetMapping("/get/unixProcess.do")
    public Map<String, Object> getUnixProcess(String cmd) throws Exception {
        return unixProcess(cmd);
    }

    @PostMapping("/post/unixProcess.do")
    public Map<String, Object> postUnixProcess(String cmd) throws Exception {
        return unixProcess(cmd);
    }

    @PostMapping("/cookie/unixProcess.do")
    public Map<String, Object> cookieUnixProcess(@CookieValue(name = "cmd") String cmd) throws Exception {
        return unixProcess(cmd);
    }

    @PostMapping("/header/unixProcess.do")
    public Map<String, Object> headerUnixProcess(@RequestHeader(name = "cmd") String cmd) throws Exception {
        return unixProcess(cmd);
    }

    @PostMapping(value = "/xml/unixProcess.do", consumes = APPLICATION_XML_VALUE)
    public Map<String, Object> xmlUnixProcess(@RequestBody Map<String, Object> map) throws Exception {
        return unixProcess((String) map.get("cmd"));
    }

    @PostMapping(value = "/json/unixProcess.do", consumes = APPLICATION_JSON_VALUE)
    public Map<String, Object> jsonUnixProcess(@RequestBody Map<String, Object> map) throws Exception {
        return unixProcess((String) map.get("cmd"));
    }

    @PostMapping("/form/unixProcess.do")
    public Map<String, Object> multipartUnixProcess(MultipartFile file) throws Exception {
        return unixProcess(file.getOriginalFilename());
    }

}