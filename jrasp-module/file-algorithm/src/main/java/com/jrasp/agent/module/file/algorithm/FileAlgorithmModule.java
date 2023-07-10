package com.jrasp.agent.module.file.algorithm;

import com.jrasp.agent.api.Module;
import com.jrasp.agent.api.ModuleLifecycleAdapter;
import com.jrasp.agent.api.RaspConfig;
import com.jrasp.agent.api.algorithm.AlgorithmManager;
import com.jrasp.agent.api.annotation.Information;
import com.jrasp.agent.api.annotation.RaspResource;
import com.jrasp.agent.api.log.RaspLog;
import com.jrasp.agent.module.file.algorithm.impl.*;
import org.kohsuke.MetaInfServices;

import java.util.Map;

/**
 * @author jrasp
 */
@MetaInfServices(Module.class)
@Information(id = "file-algorithm", author = "jrasp")
public class FileAlgorithmModule extends ModuleLifecycleAdapter implements Module {

    @RaspResource
    private RaspLog logger;

    @RaspResource
    private RaspConfig raspConfig;

    @RaspResource
    private AlgorithmManager algorithmManager;

    @RaspResource
    private String metaInfo;

    private volatile FileDeleteAlgorithm fileDeleteAlgorithm;

    private volatile FileListAlgorithm fileListAlgorithm;

    private volatile FileReadAlgorithm fileReadAlgorithm;

    private volatile FileUploadAlgorithm fileUploadAlgorithm;

    private volatile FileInitAlgorithm fileInitAlgorithm;

    // 其他算法实例这里添加

    @Override
    public boolean update(Map<String, String> configMaps) {
        this.fileDeleteAlgorithm = new FileDeleteAlgorithm(configMaps, raspConfig, logger, metaInfo);
        this.fileListAlgorithm = new FileListAlgorithm(configMaps, raspConfig, logger, metaInfo);
        this.fileReadAlgorithm = new FileReadAlgorithm(configMaps, raspConfig, logger, metaInfo);
        this.fileUploadAlgorithm = new FileUploadAlgorithm(configMaps, raspConfig, logger, metaInfo);
        this.fileInitAlgorithm = new FileInitAlgorithm(configMaps, raspConfig, logger, metaInfo);
        algorithmManager.register(fileListAlgorithm, fileDeleteAlgorithm, fileReadAlgorithm, fileUploadAlgorithm, fileInitAlgorithm);
        return true;
    }

    @Override
    public void loadCompleted() {
        this.fileDeleteAlgorithm = new FileDeleteAlgorithm(logger);
        this.fileListAlgorithm = new FileListAlgorithm(logger);
        this.fileReadAlgorithm = new FileReadAlgorithm(logger);
        this.fileUploadAlgorithm = new FileUploadAlgorithm(logger);
        this.fileInitAlgorithm = new FileInitAlgorithm(logger);
        algorithmManager.register(fileListAlgorithm, fileDeleteAlgorithm, fileReadAlgorithm, fileUploadAlgorithm, fileInitAlgorithm);
    }

    @Override
    public void onUnload() throws Throwable {
        if (fileDeleteAlgorithm != null) {
            algorithmManager.destroy(fileDeleteAlgorithm);
            fileDeleteAlgorithm = null;
        }
        if (fileListAlgorithm != null) {
            algorithmManager.destroy(fileListAlgorithm);
            fileListAlgorithm = null;
        }
        if (fileReadAlgorithm != null) {
            algorithmManager.destroy(fileReadAlgorithm);
        }
        if (fileUploadAlgorithm != null) {
            algorithmManager.destroy(fileUploadAlgorithm);
        }
        if (fileInitAlgorithm != null) {
            algorithmManager.destroy(fileInitAlgorithm);
        }
        logger.info("file-algorithm onUnload success.");
    }
}
