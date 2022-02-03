package com.jrasp.module.admin;

import com.jrasp.api.json.JSONObject;
import com.jrasp.api.*;
import com.jrasp.api.Module;
import com.jrasp.api.annotation.Command;
import com.jrasp.api.model.RestResultUtils;
import org.kohsuke.MetaInfServices;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;

@MetaInfServices(Module.class)
@Information(id = "info", version = "0.0.1", author = "jrasp")
public class InfoModule implements Module {


    @Resource
    private ConfigInfo configInfo;

    @Resource
    private JSONObject jsonObject;

    @Command("version")
    public void version(final PrintWriter writer) throws IOException {
        HashMap<String, Object> infoMap = new HashMap<String, Object>();
        infoMap.put("username", configInfo.getUsername());
        infoMap.put("version", configInfo.getVersion());
        infoMap.put("mode", configInfo.getMode());
        infoMap.put("raspHome", configInfo.getRaspHome());
        writer.println(jsonObject.toJSONString(RestResultUtils.success(infoMap)));
        writer.flush();
    }
}
