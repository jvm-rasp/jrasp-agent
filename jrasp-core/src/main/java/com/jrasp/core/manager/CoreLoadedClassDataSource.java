package com.jrasp.core.manager;


import com.jrasp.api.resource.LoadedClassDataSource;
import com.jrasp.core.util.matcher.Matcher;

import java.util.List;

public interface CoreLoadedClassDataSource extends LoadedClassDataSource {
    List<Class<?>> findForReTransform(Matcher matcher);
}
