package com.jrasp.core.manager.impl;

import com.jrasp.api.ModuleException;
import com.jrasp.api.resource.ModuleController;
import com.jrasp.core.CoreModule;
import com.jrasp.core.manager.CoreModuleManager;

/**
 * 沙箱模块控制器
 */
class DefaultModuleController implements ModuleController {

    private final CoreModule coreModule;
    private final CoreModuleManager coreModuleManager;

    DefaultModuleController(final CoreModule coreModule,
                            final CoreModuleManager coreModuleManager) {
        this.coreModule = coreModule;
        this.coreModuleManager = coreModuleManager;
    }

    @Override
    public void active() throws ModuleException {
        coreModuleManager.active(coreModule);
    }

    @Override
    public void frozen() throws ModuleException {
        coreModuleManager.frozen(coreModule, false);
    }
}
