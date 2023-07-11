package com.jrasp.agent.module.xxe.hook;

import org.junit.Test;

import javax.xml.parsers.DocumentBuilderFactory;

public class XxeModuleTest {

    private static final String FEATURE_DEFAULTS_1 = "http://apache.org/xml/features/disallow-doctype-decl";
    private static final String FEATURE_DEFAULTS_2 = "http://xml.org/sax/features/external-general-entities";
    private static final String FEATURE_DEFAULTS_3 = "http://xml.org/sax/features/external-parameter-entities";
    private static final String FEATURE_DEFAULTS_4 = "http://apache.org/xml/features/nonvalidating/load-external-dtd";


    @Test
    public void test() throws Exception {
        javax.xml.parsers.DocumentBuilderFactory instance = org.apache.xerces.jaxp.DocumentBuilderFactoryImpl.newInstance();
        assert !setXXE(instance);
    }

    private boolean setXXE(DocumentBuilderFactory instance) throws Exception {
        if (instance != null) {
            try {
                instance.getClass().getDeclaredMethod("setFeature", String.class, boolean.class);
            } catch (Exception e) {
                return false;
            }
            instance.setFeature(FEATURE_DEFAULTS_1, true);
            instance.setFeature(FEATURE_DEFAULTS_2, false);
            instance.setFeature(FEATURE_DEFAULTS_3, false);
            instance.setFeature(FEATURE_DEFAULTS_4, false);
            instance.setXIncludeAware(false);
            instance.setExpandEntityReferences(false);
            return true;
        }
        return true;
    }


}
