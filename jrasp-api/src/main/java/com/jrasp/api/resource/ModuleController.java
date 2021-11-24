package com.jrasp.api.resource;

import com.jrasp.api.ModuleException;

public interface ModuleController {

    void active() throws ModuleException;

    void frozen() throws ModuleException;

}
