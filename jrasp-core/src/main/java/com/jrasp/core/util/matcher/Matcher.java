package com.jrasp.core.util.matcher;

import com.jrasp.core.util.matcher.structure.ClassStructure;

public interface Matcher {
    MatchingResult matching(ClassStructure classStructure);

}
