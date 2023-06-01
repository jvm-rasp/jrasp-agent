package com.jrasp.agent.module.sql.algorithm;

import com.jrasp.agent.api.Module;
import com.jrasp.agent.api.ModuleLifecycleAdapter;
import com.jrasp.agent.api.RaspConfig;
import com.jrasp.agent.api.algorithm.AlgorithmManager;
import com.jrasp.agent.api.annotation.Information;
import com.jrasp.agent.api.annotation.RaspResource;
import com.jrasp.agent.api.log.RaspLog;
import com.jrasp.agent.module.sql.algorithm.impl.MySqlAlgorithm;
import com.jrasp.agent.module.sql.algorithm.impl.OracleAlgorithm;
import com.jrasp.agent.module.sql.algorithm.impl.SQLServerAlgorithm;
import org.kohsuke.MetaInfServices;

import java.util.Map;

/**
 * sql注入检测算法
 * 支持的 sql 中间件：mysql
 * 其他类型数据库请自行实现
 */
@MetaInfServices(Module.class)
@Information(id = "sql-algorithm", author = "jrasp")
public class SqlAlgorithm extends ModuleLifecycleAdapter implements Module {

    @RaspResource
    private RaspLog logger;

    @RaspResource
    private AlgorithmManager algorithmManager;

    @RaspResource
    private RaspConfig raspConfig;

    @RaspResource
    private String metaInfo;

    private volatile MySqlAlgorithm mySqlAlgorithm;

    private volatile OracleAlgorithm oracleAlgorithm;

    private volatile SQLServerAlgorithm sqlServerAlgorithm;

    // 其他算法实例这里添加
    @Override
    public boolean update(Map<String, String> configMaps) {
        mySqlAlgorithm = new MySqlAlgorithm(configMaps, raspConfig, logger, metaInfo);
        oracleAlgorithm = new OracleAlgorithm(configMaps, raspConfig, logger, metaInfo);
        sqlServerAlgorithm = new SQLServerAlgorithm(configMaps, raspConfig, logger, metaInfo);
        algorithmManager.register(mySqlAlgorithm, oracleAlgorithm, sqlServerAlgorithm);
        return true;
    }

    @Override
    public void loadCompleted() {
        mySqlAlgorithm = new MySqlAlgorithm(raspConfig, logger);
        oracleAlgorithm = new OracleAlgorithm(raspConfig, logger);
        sqlServerAlgorithm = new SQLServerAlgorithm(raspConfig, logger);
        algorithmManager.register(mySqlAlgorithm, oracleAlgorithm, sqlServerAlgorithm);
    }

    @Override
    public void onUnload() throws Throwable {
        if (mySqlAlgorithm != null) {
            algorithmManager.destroy(mySqlAlgorithm);
            mySqlAlgorithm = null;
        }
        if (oracleAlgorithm != null) {
            algorithmManager.destroy(oracleAlgorithm);
            oracleAlgorithm = null;
        }
        if (sqlServerAlgorithm != null) {
            algorithmManager.destroy(sqlServerAlgorithm);
            sqlServerAlgorithm = null;
        }
        logger.info("sql algorithm onUnload success.");
    }
}
