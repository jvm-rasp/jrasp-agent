package com.jrasp.module.admin;

import com.jrasp.api.json.JSONObject;
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
import static com.jrasp.module.admin.AdminModuleLogIdConstant.*;

@MetaInfServices(Module.class)
@Information(id = "module", version = "0.0.1", author = "jrasp")
public class ModuleMgrModule implements Module {


    @Resource
    private Log logger;

    @Resource
    private JSONObject jsonObject;

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
                logger.warn(MODULE_LIST_ERROR_LOG_ID,"get module info occur error when list modules, module[id={};class={};], error={}, ignore this module.",
                        me.getUniqueId(), module.getClass(), me.getErrorCode(), me);
            }

        }
        writer.println(jsonObject.toFormatJSONString(RestResultUtils.success("module list success", moduleInfoList)));
    }

    @Command("flush")
    public void flush(final Map<String, String> param,
                      final PrintWriter writer) throws ModuleException {
        final String isForceString = getParamWithDefault(param, "force", EMPTY);
        final boolean isForce = BooleanUtils.toBoolean(isForceString);
        moduleManager.flush(isForce);
        writer.println(jsonObject.toFormatJSONString(RestResultUtils.success("modules flush finished")));
    }

    @Command("reset")
    public void reset(final PrintWriter writer) throws ModuleException {
        moduleManager.reset();
        writer.println(jsonObject.toFormatJSONString(RestResultUtils.success("modules reset finished")));
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
                logger.warn(MODULE_UNLOAD_ERROR_LOG_ID,"unload module[id={};] occur error={}.", me.getUniqueId(), me.getErrorCode(), me);
            }
        }
        writer.println(jsonObject.toFormatJSONString(RestResultUtils.success("modules unload finished", total)));
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
                    logger.warn(MODULE_ACTIVE_ERROR_LOG_ID,"active module[id={};] occur error={}.", me.getUniqueId(), me.getErrorCode(), me);
                }// try
            } else {
                total++;
            }
        }// for
        writer.println(jsonObject.toFormatJSONString(RestResultUtils.success("modules activate finished", total)));
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
                    logger.warn(MODULE_FROZEN_ERROR_LOG_ID,"frozen module[id={};] occur error={}.", me.getUniqueId(), me.getErrorCode(), me);
                }
            } else {
                total++;
            }

        }
        writer.println(jsonObject.toFormatJSONString(RestResultUtils.success("modules frozen finished", total)));
    }

    @Command("detail")
    public void detail(final Map<String, String> param,
                       final PrintWriter writer) throws ModuleException {
        final String uniqueId = param.get("id");
        if (StringUtils.isBlank(uniqueId)) {
            writer.println(jsonObject.toFormatJSONString(RestResultUtils.failed(CLIENT_ERROR, "id parameter was required.")));
            return;
        }

        final Module module = moduleManager.get(uniqueId);
        if (null == module) {
            writer.println(jsonObject.toFormatJSONString(RestResultUtils.failed(CLIENT_ERROR, String.format("module[id=%s] is not existed.", uniqueId))));
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
        writer.println(jsonObject.toFormatJSONString(RestResultUtils.success("get module detail success", moduleInfo)));
    }

}
