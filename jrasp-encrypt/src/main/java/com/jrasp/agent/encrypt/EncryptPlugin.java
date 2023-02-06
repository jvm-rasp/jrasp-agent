package com.jrasp.agent.encrypt;

import com.jrasp.agent.encrypt.util.IoUtils;
import com.jrasp.agent.encrypt.util.StrUtils;
import org.apache.maven.model.Build;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.List;

/**
 * 加密模块jar的maven插件
 *
 * @author jrasp
 */
@Mojo(name = "encrypt", defaultPhase = LifecyclePhase.PACKAGE)
public class EncryptPlugin extends AbstractMojo {

    //MavenProject
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    //密码 文件
    @Parameter(required = true)
    private String decryptKeyFile;

    //要加密的包名前缀
    @Parameter
    private String packages;

    /**
     * 打包的时候执行
     *
     * @throws MojoExecutionException MojoExecutionException
     * @throws MojoFailureException   MojoFailureException
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        Log logger = getLog();
        Build build = project.getBuild();
        long t1 = System.currentTimeMillis();

        if ("pom".equals(project.getPackaging())) {
            logger.warn("pom project [" + project.getName() + "] ignore!");
            return;
        }

        String targetJar = build.getDirectory() + File.separator + build.getFinalName()
                + "." + project.getPackaging();
        logger.info("Encrypting " + project.getPackaging() + " [" + targetJar + "]");
        List<String> packageList = StrUtils.toList(packages);

        File file = new File(decryptKeyFile);
        if (file.isDirectory()) {
            logger.error("decrypt key file not exist, file: " + decryptKeyFile);
            return;
        }

        byte[] content = IoUtils.readFileToByte(file);
        String password = new String(content);

        JarEncryptor encryptor = new JarEncryptor(targetJar, password.trim());
        encryptor.setPackages(packageList);
        String result = encryptor.doEncryptJar();
        long t2 = System.currentTimeMillis();

        logger.info("Encrypt " + encryptor.getEncryptFileCount() + " classes");
        logger.info("Encrypted " + project.getPackaging() + " [" + result + "]");
        logger.info("Encrypt complete");
        logger.info("Time [" + ((t2 - t1) / 1000d) + " s]");
        logger.info("");
    }

}