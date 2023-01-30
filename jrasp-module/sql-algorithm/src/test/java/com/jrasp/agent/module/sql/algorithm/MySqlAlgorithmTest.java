package com.jrasp.agent.module.sql.algorithm;

import com.jrasp.agent.api.RaspConfig;
import com.jrasp.agent.api.log.RaspLog;
import com.jrasp.agent.api.request.AttackInfo;
import com.jrasp.agent.module.sql.algorithm.impl.MySqlAlgorithm;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class MySqlAlgorithmTest {

    private static final Map<String, String> configMaps = new HashMap<String, String>();

    private static final RaspLog logger = new RaspLog() {
        @Override
        public void attack(AttackInfo attackInfo) {

        }

        @Override
        public void info(String message) {
            System.out.println("[info] " + message);
        }

        @Override
        public void warning(String message) {
            System.out.println("[warning] " + message);
        }

        @Override
        public void error(String message) {
            System.out.println("[error] " + message);
        }

        @Override
        public void error(String message, Throwable t) {
            System.out.println("[error] " + message);
            System.out.println("[Throwable] " + t.getLocalizedMessage());
        }
    };

    private static final RaspConfig raspConfig = new RaspConfig() {
        @Override
        public boolean isCheckDisable() {
            return false;
        }

        @Override
        public void setCheckDisable(boolean checkEnable) {

        }

        @Override
        public String getRedirectUrl() {
            return null;
        }

        @Override
        public String getJsonBlockContent() {
            return null;
        }

        @Override
        public String getXmlBlockContent() {
            return null;
        }

        @Override
        public String getHtmlBlockContent() {
            return null;
        }

        @Override
        public void setRedirectUrl(String redirectUrl) {

        }

        @Override
        public void setJsonBlockContent(String jsonBlockContent) {

        }

        @Override
        public void setXmlBlockContent(String xmlBlockContent) {

        }

        @Override
        public void setHtmlBlockContent(String htmlBlockContent) {

        }

        @Override
        public int getBlockStatusCode() {
            return 0;
        }

        @Override
        public void setBlockStatusCode(int blockStatusCode) {

        }
    };

    @Test
    public void test() {
        MySqlAlgorithm mySqlAlgorithm = new MySqlAlgorithm(configMaps, raspConfig, logger);
        String sql = "select top 5 * from CG_Merchandise where 1=1 and isnull(merchandisedaima,'')!='' and ISNULL(isavailable,'1')=? and merchandisename like ? and type like ? and CHARINDEX(ISNULL(seltype,'0'),';0;1;2;3;')>? and brandname like ? order by OperateDate desc";
        try {
            long time0 = System.currentTimeMillis();
            mySqlAlgorithm.check(null, sql);
            long time1 = System.currentTimeMillis();
            mySqlAlgorithm.check(null, sql);
            long time2 = System.currentTimeMillis();
            logger.info("sql first check cost time: " + (time1 - time0) + ", second check cost time: " + (time2 - time1));
            assert (time2 - time1) < (time1 - time0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}