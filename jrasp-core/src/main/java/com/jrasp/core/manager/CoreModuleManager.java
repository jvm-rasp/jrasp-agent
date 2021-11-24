package com.jrasp.core.manager;

import com.jrasp.api.ModuleException;
import com.jrasp.core.CoreModule;

import java.util.Collection;

public interface CoreModuleManager {

    void flush(boolean isForce) throws ModuleException;

    CoreModuleManager reset() throws ModuleException;

    void active(CoreModule coreModule) throws ModuleException;

    void frozen(CoreModule coreModule, boolean isIgnoreModuleException) throws ModuleException;

    Collection<CoreModule> list();

    CoreModule get(String uniqueId);

    CoreModule getThrowsExceptionIfNull(String uniqueId) throws ModuleException;


    CoreModule unload(CoreModule coreModule, boolean isIgnoreModuleException) throws ModuleException;


    void unloadAll();

}
