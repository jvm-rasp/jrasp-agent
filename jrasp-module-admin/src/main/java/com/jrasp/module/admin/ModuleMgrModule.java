package com.jrasp.module.admin;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.jrasp.api.Information;
import com.jrasp.api.Module;
import com.jrasp.api.ModuleException;
import com.jrasp.api.Resource;
import com.jrasp.api.annotation.Command;
import com.jrasp.api.log.Log;
import com.jrasp.api.model.ModuleInfo;
import com.jrasp.api.model.RestResultUtils;
import com.jrasp.api.resource.ModuleManager;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.kohsuke.MetaInfServices;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.jrasp.api.model.ResultCodeEnum.CLIENT_ERROR;
import static com.jrasp.api.util.GaStringUtils.matching;
import static org.apache.commons.lang3.StringUtils.EMPTY;

@MetaInfServices(Module.class)
@Information(id = "module", version = "0.0.1", author = "jrasp")
public class ModuleMgrModule implements Module {

    @Resource
    private Log logger;

    @Resource
    private ModuleManager moduleManager;

    // 获取参数值
    private String getParamWithDefault(final Map<String, String> param, final String name, final String defaultValue) {
        final String valueFromReq = param.get(name);
        return StringUtils.isBlank(valueFromReq)
                ? defaultValue
                : valueFromReq;
    }

    // 搜索模块
    private Collection<Module> search(final String idsStringPattern) {
        final Collection<Module> foundModules = new ArrayList<Module>();
        for (Module module : moduleManager.list()) {
            final Information moduleInfo = module.getClass().getAnnotation(Information.class);
            if (!matching(moduleInfo.id(), idsStringPattern)) {
                continue;
            }
            foundModules.add(module);
        }
        return foundModules;
    }

    @Command("list")
    public void list(final PrintWriter writer) throws IOException {
        Collection<Module> moduleList = moduleManager.list();
        List<ModuleInfo> moduleInfoList = new ArrayList<ModuleInfo>(moduleList.size());
        for (final Module module : moduleList) {
            final Information info = module.getClass().getAnnotation(Information.class);
            try {
                String id = info.id();
                final boolean isActivated = moduleManager.isActivated(id);
                final boolean isLoaded = moduleManager.isLoaded(id);
                final int cCnt = moduleManager.cCnt(id);
                final int mCnt = moduleManager.mCnt(id);
                ModuleInfo moduleInfo = new ModuleInfo(id, isActivated, isLoaded, cCnt, mCnt);
                moduleInfoList.add(moduleInfo);
            } catch (ModuleException me) {
                logger.warn("get module info occur error when list modules, module[id={};class={};], error={}, ignore this module.",
                        me.getUniqueId(), module.getClass(), me.getErrorCode(), me);
            }

        }
        writer.println(JSONObject.toJSONString(RestResultUtils.success("module list success", moduleInfoList), SerializerFeature.PrettyFormat));
    }

    @Command("flush")
    public void flush(final Map<String, String> param,
                      final PrintWriter writer) throws ModuleException {
        final String isForceString = getParamWithDefault(param, "force", EMPTY);
        final boolean isForce = BooleanUtils.toBoolean(isForceString);
        moduleManager.flush(isForce);
        writer.println(JSONObject.toJSONString(RestResultUtils.success("modules flush finished"), SerializerFeature.PrettyFormat));
    }

    @Command("reset")
    public void reset(final PrintWriter writer) throws ModuleException {
        moduleManager.reset();
        writer.println(JSONObject.toJSONString(RestResultUtils.success("modules reset finished"), SerializerFeature.PrettyFormat));
    }

    @Command("unload")
    public void unload(final Map<String, String> param,
                       final PrintWriter writer) {
        int total = 0;
        final String idsStringPattern = getParamWithDefault(param, "ids", EMPTY);
        for (final Module module : search(idsStringPattern)) {
            final Information info = module.getClass().getAnnotation(Information.class);
            try {
                moduleManager.unload(info.id());
                total++;
            } catch (ModuleException me) {
                logger.warn("unload module[id={};] occur error={}.", me.getUniqueId(), me.getErrorCode(), me);
            }
        }
        writer.println(JSONObject.toJSONString(RestResultUtils.success("modules unload finished", total), SerializerFeature.PrettyFormat));
    }

    @Command("active")
    public void active(final Map<String, String> param,
                       final PrintWriter writer) throws ModuleException {
        int total = 0;
        final String idsStringPattern = getParamWithDefault(param, "ids", EMPTY);
        for (final Module module : search(idsStringPattern)) {
            final Information info = module.getClass().getAnnotation(Information.class);
            final boolean isActivated = moduleManager.isActivated(info.id());
            if (!isActivated) {
                try {
                    moduleManager.active(info.id());
                    total++;
                } catch (ModuleException me) {
                    logger.warn("active module[id={};] occur error={}.", me.getUniqueId(), me.getErrorCode(), me);
                }// try
            } else {
                total++;
            }
        }// for
        writer.println(JSONObject.toJSONString(RestResultUtils.success("modules activate finished", total), SerializerFeature.PrettyFormat));
    }

    @Command("frozen")
    public void frozen(final Map<String, String> param,
                       final PrintWriter writer) throws ModuleException {
        int total = 0;
        final String idsStringPattern = getParamWithDefault(param, "ids", EMPTY);
        for (final Module module : search(idsStringPattern)) {
            final Information info = module.getClass().getAnnotation(Information.class);
            final boolean isActivated = moduleManager.isActivated(info.id());
            if (isActivated) {
                try {
                    moduleManager.frozen(info.id());
                    total++;
                } catch (ModuleException me) {
                    logger.warn("frozen module[id={};] occur error={}.", me.getUniqueId(), me.getErrorCode(), me);
                }
            } else {
                total++;
            }

        }
        writer.println(JSONObject.toJSONString(RestResultUtils.success("modules frozen finished", total), SerializerFeature.PrettyFormat));
    }

    @Command("detail")
    public void detail(final Map<String, String> param,
                       final PrintWriter writer) throws ModuleException {
        final String uniqueId = param.get("id");
        if (StringUtils.isBlank(uniqueId)) {
            writer.println(JSONObject.toJSONString(RestResultUtils.failed(CLIENT_ERROR, "id parameter was required."), SerializerFeature.PrettyFormat));
            return;
        }

        final Module module = moduleManager.get(uniqueId);
        if (null == module) {
            writer.println(JSONObject.toJSONString(RestResultUtils.failed(CLIENT_ERROR, String.format("module[id=%s] is not existed.", uniqueId)), SerializerFeature.PrettyFormat));
            return;
        }
        final Information info = module.getClass().getAnnotation(Information.class);

        String id = info.id();
        final boolean isActivated = moduleManager.isActivated(id);
        final boolean isLoaded = moduleManager.isLoaded(id);
        final int cCnt = moduleManager.cCnt(id);
        final int mCnt = moduleManager.mCnt(id);
        final String version = info.version();
        ModuleInfo moduleInfo = new ModuleInfo(id, isActivated, isLoaded, cCnt, mCnt, version);
        writer.println(JSONObject.toJSONString(RestResultUtils.success("get module detail success", moduleInfo), SerializerFeature.PrettyFormat));
    }

}
