package com.jrasp.agent.encrypt;

import com.jrasp.agent.encrypt.util.*;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.util.*;

public class JarEncryptor {

    private static final String LIB_JAR_DIR = "__temp__";

    //要加密的jar或war
    private String jarPath = null;

    //要加密的包，多个用逗号隔开
    private List<String> packages = null;

    //密码
    private String password = null;

    //jar还是war
    private String jarOrWar = null;

    //工作目录
    private File targetDir = null;

    //加密的文件数量
    private Integer encryptFileCount = null;

    //存储解析出来的类名和路径
    private Map<String, String> resolveClassName = new HashMap<>();

    /**
     * 构造方法
     *
     * @param jarPath  要加密的jar或war
     * @param password 密码
     */
    public JarEncryptor(String jarPath, String password) {
        super();
        this.jarPath = jarPath;
        this.password = password;
    }

    /**
     * 加密jar的主要过程
     *
     * @return 解密后生成的文件的绝对路径
     */
    public String doEncryptJar() {
        if (!jarPath.endsWith(".jar") && !jarPath.endsWith(".war")) {
            throw new RuntimeException("jar/war文件格式有误");
        }
        if (!new File(jarPath).exists()) {
            throw new RuntimeException("文件不存在:" + jarPath);
        }
        if (password == null || password.length() == 0) {
            throw new RuntimeException("密码不能为空");
        }

        this.jarOrWar = jarPath.substring(jarPath.lastIndexOf(".") + 1);
        Log.debug("加密类型：" + jarOrWar);

        //临时work目录
        this.targetDir = new File(jarPath.replace("." + jarOrWar, LIB_JAR_DIR));
        Log.debug("临时目录：" + targetDir);

        //[1]释放所有文件
        List<String> allFile = JarUtils.unJar(jarPath, this.targetDir.getAbsolutePath());

        //[2]提取所有需要加密的class文件
        List<File> classFiles = filterClasses(allFile);

        if (classFiles.isEmpty()) {
            throw new RuntimeException(jarPath + " 未匹配到任何类，请检查待加密的模块包路径是否符合规范");
        }

        //[3]将正常的class加密，压缩另存
        List<String> encryptClass = encryptClass(classFiles);
        this.encryptFileCount = encryptClass.size();

        //[4]打包回去
        return packageJar(allFile);
    }


    /**
     * 找出所有需要加密的class文件
     *
     * @param allFile 所有文件
     * @return 待加密的class列表
     */
    public List<File> filterClasses(List<String> allFile) {
        List<File> classFiles = new ArrayList<>();
        allFile.forEach(file -> {
            if (!file.endsWith(".class")) {
                return;
            }
            //解析出类全名
            String className = resolveClassName(file, true);
            //判断包名相同和是否排除的类
            if (StrUtils.isMatchs(this.packages, className, false)) {
                classFiles.add(new File(file));
                Log.debug("待加密: " + file);
            }
        });
        return classFiles;
    }

    /**
     * 加密class文件，放在META-INF/classes里
     *
     * @param classFiles jar/war 下需要加密的class文件
     * @return 已经加密的类名
     */
    private List<String> encryptClass(List<File> classFiles) {
        List<String> encryptClasses = new ArrayList<>();

        //加密另存
        classFiles.forEach(classFile -> {
            String className = classFile.getName();
            if (className.endsWith(".class")) {
                className = resolveClassName(classFile.getAbsolutePath(), true);
            }
            byte[] bytes = IoUtils.readFileToByte(classFile);
            bytes = encrypt(bytes, password);
            File targetFile = new File(classFile.getAbsolutePath());
            IoUtils.writeFile(targetFile, bytes);
            encryptClasses.add(className);
            Log.debug("加密：" + className);
        });

        return encryptClasses;
    }

    /**
     * 压缩成jar
     *
     * @return 打包后的jar绝对路径
     */
    private String packageJar(List<String> libJarFiles) {
        //[1]先打包lib下的jar
        libJarFiles.forEach(targetJar -> {
            if (!targetJar.endsWith(".jar")) {
                return;
            }

            String srcJarDir = targetJar.substring(0, targetJar.length() - 4) + LIB_JAR_DIR;
            if (!new File(srcJarDir).exists()) {
                return;
            }
            JarUtils.doJar(srcJarDir, targetJar);
            IoUtils.delete(new File(srcJarDir));
            Log.debug("打包: " + targetJar);
        });

        //删除META-INF下的maven
        IoUtils.delete(new File(this.targetDir, "META-INF/maven"));

        //[2]再打包jar
        String targetJar = jarPath.replace("." + jarOrWar, "-encrypted." + jarOrWar);
        String result = JarUtils.doJar(this.targetDir.getAbsolutePath(), targetJar);
        IoUtils.delete(this.targetDir);
        Log.debug("打包: " + targetJar);
        return result;
    }

    /**
     * 根据class的绝对路径解析出class名称或class包所在的路径
     *
     * @param fileName    class绝对路径
     * @param classOrPath true|false
     * @return class名称|包所在的路径
     */
    private String resolveClassName(String fileName, boolean classOrPath) {
        String result = resolveClassName.get(fileName + classOrPath);
        if (result != null) {
            return result;
        }
        String file = fileName.substring(0, fileName.length() - 6);
        String K_CLASSES = File.separator + "classes" + File.separator;
        String K_LIB = File.separator + "lib" + File.separator;

        String clsPath;
        String clsName;
        //lib内的的jar包
        if (file.contains(K_LIB)) {
            clsName = file.substring(file.indexOf(LIB_JAR_DIR, file.indexOf(K_LIB))
                    + LIB_JAR_DIR.length() + 1);
            clsPath = file.substring(0, file.length() - clsName.length() - 1);
        }
        //jar/war包-INF/classes下的class文件
        else if (file.contains(K_CLASSES)) {
            clsName = file.substring(file.indexOf(K_CLASSES) + K_CLASSES.length());
            clsPath = file.substring(0, file.length() - clsName.length() - 1);

        }
        //jar包下的class文件
        else {
            clsName = file.substring(file.indexOf(LIB_JAR_DIR) + LIB_JAR_DIR.length() + 1);
            clsPath = file.substring(0, file.length() - clsName.length() - 1);
        }
        result = classOrPath ? clsName.replace(File.separator, ".") : clsPath;
        resolveClassName.put(fileName + classOrPath, result);
        return result;
    }


    public Integer getEncryptFileCount() {
        return encryptFileCount;
    }

    public void setPackages(List<String> packages) {
        this.packages = packages;
    }

    /**
     * AES 加密
     *
     * @param content 待加密内容
     * @param aesKey  密码
     * @return
     */
    public static byte[] encrypt(byte[] content, String aesKey) {
        if (aesKey == null || aesKey.length() != 16) {
            return null;
        }
        try {
            byte[] bytes = aesKey.getBytes("UTF-8");
            SecretKeySpec skeySpec = new SecretKeySpec(bytes, "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
            return cipher.doFinal(content);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
