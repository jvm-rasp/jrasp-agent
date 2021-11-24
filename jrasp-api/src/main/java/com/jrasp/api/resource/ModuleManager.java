package com.jrasp.api.resource;

import com.jrasp.api.Module;
import com.jrasp.api.ModuleException;

import java.io.File;
import java.util.Collection;


public interface ModuleManager {

    void flush(boolean isForce) throws ModuleException;

    void reset() throws ModuleException;

    void unload(String uniqueId) throws ModuleException;

    void active(String uniqueId) throws ModuleException;

    void frozen(String uniqueId) throws ModuleException;

    Collection<Module> list();

    Module get(String uniqueId);

    int cCnt(String uniqueId) throws ModuleException;

    int mCnt(String uniqueId) throws ModuleException;

    boolean isActivated(String uniqueId) throws ModuleException;

    boolean isLoaded(String uniqueId) throws ModuleException;

    File getJarFile(String uniqueId) throws ModuleException;

}
