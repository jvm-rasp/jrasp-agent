package com.jrasp.api.listener.ext;

import com.jrasp.api.event.Event;
import com.jrasp.api.filter.ExtFilter;
import com.jrasp.api.filter.Filter;
import com.jrasp.api.listener.EventListener;
import com.jrasp.api.resource.ModuleEventWatcher;
import com.jrasp.api.resource.ModuleEventWatcher.Progress;
import com.jrasp.api.util.GaArrayUtils;
import com.jrasp.api.util.GaStringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.jrasp.api.event.Event.Type.*;
import static com.jrasp.api.listener.ext.EventWatchBuilder.PatternType.WILDCARD;
import static com.jrasp.api.util.GaCollectionUtils.add;
import static com.jrasp.api.util.GaStringUtils.getJavaClassName;
import static com.jrasp.api.util.GaStringUtils.getJavaClassNameArray;
import static java.util.regex.Pattern.quote;


public class EventWatchBuilder {

    public interface IBuildingForClass {

        IBuildingForClass includeBootstrap();

        IBuildingForClass isIncludeBootstrap(boolean isIncludeBootstrap);

        IBuildingForClass includeSubClasses();

        IBuildingForClass isIncludeSubClasses(boolean isIncludeSubClasses);

        IBuildingForClass withAccess(int access);

        IBuildingForClass hasInterfaceTypes(Class<?>... classes);

        IBuildingForClass hasInterfaceTypes(String... patterns);

        IBuildingForClass hasAnnotationTypes(Class<?>... classes);

        IBuildingForClass hasAnnotationTypes(String... patterns);

        IBuildingForBehavior onAnyBehavior();

        IBuildingForBehavior onBehavior(String pattern);

    }

    public interface IBuildingForBehavior {

        IBuildingForBehavior withAccess(int access);

        IBuildingForBehavior withEmptyParameterTypes();

        IBuildingForBehavior withParameterTypes(String... patterns);

        IBuildingForBehavior withParameterTypes(Class<?>... classes);

        IBuildingForBehavior hasExceptionTypes(String... patterns);

        IBuildingForBehavior hasExceptionTypes(Class<?>... classes);

        IBuildingForBehavior hasAnnotationTypes(String... patterns);

        IBuildingForBehavior hasAnnotationTypes(Class<?>... classes);

        IBuildingForBehavior onBehavior(String pattern);

        IBuildingForClass onClass(String pattern);

        IBuildingForClass onClass(Class<?> clazz);

        IBuildingForClass onAnyClass();

        IBuildingForWatching onWatching();

        EventWatcher onWatch(AdviceListener adviceListener);

        @Deprecated
        EventWatcher onWatch(AdviceListener adviceListener, Event.Type... eventTypeArray);

        EventWatcher onWatch(EventListener eventListener, Event.Type... eventTypeArray);

    }

    public interface IBuildingForWatching {

        IBuildingForWatching withProgress(Progress progress);

        IBuildingForWatching withCall();

        IBuildingForWatching withLine();

        EventWatcher onWatch(AdviceListener adviceListener);

        EventWatcher onWatch(EventListener eventListener, Event.Type... eventTypeArray);

    }

    public interface IBuildingForUnWatching {

        IBuildingForUnWatching withProgress(Progress progress);

        void onUnWatched();

    }


    // -------------------------- 这里开始实现 --------------------------

    public enum PatternType {
        WILDCARD,
        REGEX
    }

    private final ModuleEventWatcher moduleEventWatcher;
    private final PatternType patternType;
    private List<BuildingForClass> bfClasses = new ArrayList<BuildingForClass>();

    public EventWatchBuilder(final ModuleEventWatcher moduleEventWatcher) {
        this(moduleEventWatcher, WILDCARD);
    }

    public EventWatchBuilder(final ModuleEventWatcher moduleEventWatcher,
                             final PatternType patternType) {
        this.moduleEventWatcher = moduleEventWatcher;
        this.patternType = patternType;
    }

    private static boolean patternMatching(final String string,
                                           final String pattern,
                                           final PatternType patternType) {
        switch (patternType) {
            case WILDCARD:
                return GaStringUtils.matching(string, pattern);
            case REGEX:
                return string.matches(pattern);
            default:
                return false;
        }
    }

    private static String[] toRegexQuoteArray(final String[] stringArray) {
        if (null == stringArray) {
            return null;
        }
        final String[] regexQuoteArray = new String[stringArray.length];
        for (int index = 0; index < stringArray.length; index++) {
            regexQuoteArray[index] = quote(stringArray[index]);
        }
        return regexQuoteArray;
    }

    public IBuildingForClass onAnyClass() {
        switch (patternType) {
            case REGEX:
                return onClass(".*");
            case WILDCARD:
            default:
                return onClass("*");
        }
    }

    public IBuildingForClass onClass(final Class<?> clazz) {
        switch (patternType) {
            case REGEX: {
                return onClass(quote(getJavaClassName(clazz)));
            }
            case WILDCARD:
            default:
                return onClass(getJavaClassName(clazz));
        }

    }

    public IBuildingForClass onClass(final String pattern) {
        return add(bfClasses, new BuildingForClass(pattern));
    }

    private class BuildingForClass implements IBuildingForClass {

        private final String pattern;
        private int withAccess = 0;
        private boolean isIncludeSubClasses = false;
        private boolean isIncludeBootstrap = false;
        private final PatternGroupList hasInterfaceTypes = new PatternGroupList();
        private final PatternGroupList hasAnnotationTypes = new PatternGroupList();
        private final List<BuildingForBehavior> bfBehaviors = new ArrayList<BuildingForBehavior>();

        BuildingForClass(final String pattern) {
            this.pattern = pattern;
        }

        @Override
        public IBuildingForClass includeBootstrap() {
            this.isIncludeBootstrap = true;
            return this;
        }

        @Override
        public IBuildingForClass isIncludeBootstrap(boolean isIncludeBootstrap) {
            if (isIncludeBootstrap) {
                includeBootstrap();
            }
            return this;
        }

        @Override
        public IBuildingForClass includeSubClasses() {
            this.isIncludeSubClasses = true;
            return this;
        }

        @Override
        public IBuildingForClass isIncludeSubClasses(boolean isIncludeSubClasses) {
            if (isIncludeSubClasses) {
                includeSubClasses();
            }
            return this;
        }

        @Override
        public IBuildingForClass withAccess(final int access) {
            withAccess |= access;
            return this;
        }

        @Override
        public IBuildingForClass hasInterfaceTypes(final String... patterns) {
            hasInterfaceTypes.add(patterns);
            return this;
        }

        @Override
        public IBuildingForClass hasAnnotationTypes(final String... patterns) {
            hasAnnotationTypes.add(patterns);
            return this;
        }

        @Override
        public IBuildingForClass hasInterfaceTypes(final Class<?>... classes) {
            switch (patternType) {
                case REGEX:
                    return hasInterfaceTypes(toRegexQuoteArray(getJavaClassNameArray(classes)));
                case WILDCARD:
                default:
                    return hasInterfaceTypes(getJavaClassNameArray(classes));
            }
        }

        @Override
        public IBuildingForClass hasAnnotationTypes(final Class<?>... classes) {
            switch (patternType) {
                case REGEX:
                    return hasAnnotationTypes(toRegexQuoteArray(getJavaClassNameArray(classes)));
                case WILDCARD:
                default:
                    return hasAnnotationTypes(getJavaClassNameArray(classes));
            }
        }

        @Override
        public IBuildingForBehavior onBehavior(final String pattern) {
            return add(bfBehaviors, new BuildingForBehavior(this, pattern));
        }

        @Override
        public IBuildingForBehavior onAnyBehavior() {
            switch (patternType) {
                case REGEX:
                    return onBehavior(".*");
                case WILDCARD:
                default:
                    return onBehavior("*");
            }
        }

    }

    private class BuildingForBehavior implements IBuildingForBehavior {

        private final BuildingForClass bfClass;
        private final String pattern;
        private int withAccess = 0;
        private final PatternGroupList withParameterTypes = new PatternGroupList();
        private final PatternGroupList hasExceptionTypes = new PatternGroupList();
        private final PatternGroupList hasAnnotationTypes = new PatternGroupList();

        BuildingForBehavior(final BuildingForClass bfClass,
                            final String pattern) {
            this.bfClass = bfClass;
            this.pattern = pattern;
        }

        @Override
        public IBuildingForBehavior withAccess(final int access) {
            withAccess |= access;
            return this;
        }

        @Override
        public IBuildingForBehavior withEmptyParameterTypes() {
            withParameterTypes.add();
            return this;
        }

        @Override
        public IBuildingForBehavior withParameterTypes(final String... patterns) {
            withParameterTypes.add(patterns);
            return this;
        }

        @Override
        public IBuildingForBehavior withParameterTypes(final Class<?>... classes) {
            switch (patternType) {
                case REGEX:
                    return withParameterTypes(toRegexQuoteArray(getJavaClassNameArray(classes)));
                case WILDCARD:
                default:
                    return withParameterTypes(getJavaClassNameArray(classes));
            }
        }

        @Override
        public IBuildingForBehavior hasExceptionTypes(final String... patterns) {
            hasExceptionTypes.add(patterns);
            return this;
        }

        @Override
        public IBuildingForBehavior hasExceptionTypes(final Class<?>... classes) {
            switch (patternType) {
                case REGEX:
                    return hasExceptionTypes(toRegexQuoteArray(getJavaClassNameArray(classes)));
                case WILDCARD:
                default:
                    return hasExceptionTypes(getJavaClassNameArray(classes));
            }
        }

        @Override
        public IBuildingForBehavior hasAnnotationTypes(final String... patterns) {
            hasAnnotationTypes.add(patterns);
            return this;
        }

        @Override
        public IBuildingForBehavior hasAnnotationTypes(final Class<?>... classes) {
            switch (patternType) {
                case REGEX:
                    return hasAnnotationTypes(toRegexQuoteArray(getJavaClassNameArray(classes)));
                case WILDCARD:
                default:
                    return hasAnnotationTypes(getJavaClassNameArray(classes));
            }
        }

        @Override
        public IBuildingForBehavior onBehavior(final String pattern) {
            return bfClass.onBehavior(pattern);
        }

        @Override
        public IBuildingForClass onClass(final String pattern) {
            return EventWatchBuilder.this.onClass(pattern);
        }

        @Override
        public IBuildingForClass onClass(final Class<?> clazz) {
            return EventWatchBuilder.this.onClass(clazz);
        }

        @Override
        public IBuildingForClass onAnyClass() {
            return EventWatchBuilder.this.onAnyClass();
        }

        @Override
        public IBuildingForWatching onWatching() {
            return new BuildingForWatching();
        }

        @Override
        public EventWatcher onWatch(AdviceListener adviceListener) {
            return build(new AdviceAdapterListener(adviceListener), null, BEFORE, RETURN, THROWS, IMMEDIATELY_RETURN, IMMEDIATELY_THROWS);
        }

        @Deprecated
        @Override
        public EventWatcher onWatch(final AdviceListener adviceListener, Event.Type... eventTypeArray) {
            return onWatch(adviceListener);
        }

        @Override
        public EventWatcher onWatch(EventListener eventListener, Event.Type... eventTypeArray) {
            return build(eventListener, null, eventTypeArray);
        }

    }

    private class BuildingForWatching implements IBuildingForWatching {

        private final Set<Event.Type> eventTypeSet = new HashSet<Event.Type>();
        private final List<Progress> progresses = new ArrayList<Progress>();

        @Override
        public IBuildingForWatching withProgress(Progress progress) {
            if (null != progress) {
                progresses.add(progress);
            }
            return this;
        }

        @Override
        public IBuildingForWatching withCall() {
            eventTypeSet.add(CALL_BEFORE);
            eventTypeSet.add(CALL_RETURN);
            eventTypeSet.add(CALL_THROWS);
            return this;
        }

        @Override
        public IBuildingForWatching withLine() {
            eventTypeSet.add(LINE);
            return this;
        }

        @Override
        public EventWatcher onWatch(AdviceListener adviceListener) {
            eventTypeSet.add(BEFORE);
            eventTypeSet.add(RETURN);
            eventTypeSet.add(THROWS);
            eventTypeSet.add(IMMEDIATELY_RETURN);
            eventTypeSet.add(IMMEDIATELY_THROWS);
            return build(
                    new AdviceAdapterListener(adviceListener),
                    toProgressGroup(progresses),
                    eventTypeSet.toArray(EMPTY)
            );
        }

        @Override
        public EventWatcher onWatch(EventListener eventListener, Event.Type... eventTypeArray) {
            return build(eventListener, toProgressGroup(progresses), eventTypeArray);
        }

    }

    private EventWatchCondition toEventWatchCondition() {
        final List<Filter> filters = new ArrayList<Filter>();
        for (final BuildingForClass bfClass : bfClasses) {
            final Filter filter = new Filter() {
                @Override
                public boolean doClassFilter(final int access,
                                             final String javaClassName,
                                             final String superClassTypeJavaClassName,
                                             final String[] interfaceTypeJavaClassNameArray,
                                             final String[] annotationTypeJavaClassNameArray) {
                    return (access & bfClass.withAccess) == bfClass.withAccess
                            && patternMatching(javaClassName, bfClass.pattern, patternType)
                            && bfClass.hasInterfaceTypes.patternHas(interfaceTypeJavaClassNameArray)
                            && bfClass.hasAnnotationTypes.patternHas(annotationTypeJavaClassNameArray);
                }

                @Override
                public boolean doMethodFilter(final int access,
                                              final String javaMethodName,
                                              final String[] parameterTypeJavaClassNameArray,
                                              final String[] throwsTypeJavaClassNameArray,
                                              final String[] annotationTypeJavaClassNameArray) {
                    // nothing to matching
                    if (bfClass.bfBehaviors.isEmpty()) {
                        return false;
                    }

                    // matching any behavior
                    for (final BuildingForBehavior bfBehavior : bfClass.bfBehaviors) {
                        if ((access & bfBehavior.withAccess) == bfBehavior.withAccess
                                && patternMatching(javaMethodName, bfBehavior.pattern, patternType)
                                && bfBehavior.withParameterTypes.patternWith(parameterTypeJavaClassNameArray)
                                && bfBehavior.hasExceptionTypes.patternHas(throwsTypeJavaClassNameArray)
                                && bfBehavior.hasAnnotationTypes.patternHas(annotationTypeJavaClassNameArray)) {
                            return true;
                        }//if
                    }//for

                    // non matched
                    return false;
                }
            };//filter

            filters.add(makeExtFilter(filter, bfClass));
        }
        return new EventWatchCondition() {
            @Override
            public Filter[] getOrFilterArray() {
                return filters.toArray(new Filter[0]);
            }
        };
    }

    private Filter makeExtFilter(final Filter filter,
                                 final BuildingForClass bfClass) {
        return ExtFilter.ExtFilterFactory.make(
                filter,
                bfClass.isIncludeSubClasses,
                bfClass.isIncludeBootstrap
        );
    }

    private ProgressGroup toProgressGroup(final List<Progress> progresses) {
        if (progresses.isEmpty()) {
            return null;
        }
        return new ProgressGroup(progresses);
    }

    private EventWatcher build(final EventListener listener,
                               final Progress progress,
                               final Event.Type... eventTypes) {

        final int watchId = moduleEventWatcher.watch(
                toEventWatchCondition(),
                listener,
                progress,
                eventTypes
        );

        return new EventWatcher() {

            final List<Progress> progresses = new ArrayList<Progress>();

            @Override
            public int getWatchId() {
                return watchId;
            }

            @Override
            public IBuildingForUnWatching withProgress(Progress progress) {
                if (null != progress) {
                    progresses.add(progress);
                }
                return this;
            }

            @Override
            public void onUnWatched() {
                moduleEventWatcher.delete(watchId, toProgressGroup(progresses));
            }

        };
    }

    /**
     * 观察进度组
     */
    private static class ProgressGroup implements Progress {

        private final List<Progress> progresses;

        ProgressGroup(List<Progress> progresses) {
            this.progresses = progresses;
        }

        @Override
        public void begin(int total) {
            for (final Progress progress : progresses) {
                progress.begin(total);
            }
        }

        @Override
        public void progressOnSuccess(Class<?> clazz, int index) {
            for (final Progress progress : progresses) {
                progress.progressOnSuccess(clazz, index);
            }
        }

        @Override
        public void progressOnFailed(Class<?> clazz, int index, Throwable cause) {
            for (final Progress progress : progresses) {
                progress.progressOnFailed(clazz, index, cause);
            }
        }

        @Override
        public void finish(int cCnt, int mCnt) {
            for (final Progress progress : progresses) {
                progress.finish(cCnt, mCnt);
            }
        }

    }

    /**
     * 模式匹配组列表
     */
    private class PatternGroupList {

        final List<Group> groups = new ArrayList<Group>();

        /*
         * 添加模式匹配组
         */
        void add(String... patternArray) {
            groups.add(new Group(patternArray));
        }

        /*
         * 模式匹配With
         */
        boolean patternWith(final String[] stringArray) {

            // 如果模式匹配组为空，说明不参与本次匹配
            if (groups.isEmpty()) {
                return true;
            }

            for (final Group group : groups) {
                if (group.matchingWith(stringArray)) {
                    return true;
                }
            }
            return false;
        }

        /*
         * 模式匹配Has
         */
        boolean patternHas(final String[] stringArray) {

            // 如果模式匹配组为空，说明不参与本次匹配
            if (groups.isEmpty()) {
                return true;
            }

            for (final Group group : groups) {
                if (group.matchingHas(stringArray)) {
                    return true;
                }
            }
            return false;
        }

    }

    /**
     * 模式匹配组
     */
    private class Group {

        final String[] patternArray;

        Group(String[] patternArray) {
            this.patternArray = GaArrayUtils.isEmpty(patternArray)
                    ? new String[0]
                    : patternArray;
        }

        /*
         * stringArray中任意字符串能匹配上匹配模式
         */
        boolean anyMatching(final String[] stringArray,
                            final String pattern) {
            if (GaArrayUtils.isEmpty(stringArray)) {
                return false;
            }
            for (final String string : stringArray) {
                if (patternMatching(string, pattern, patternType)) {
                    return true;
                }
            }
            return false;
        }

        /*
         * 匹配模式组中所有匹配模式都在目标中存在匹配通过的元素
         * 要求匹配组中每一个匹配项都在stringArray中存在匹配的字符串
         */
        boolean matchingHas(final String[] stringArray) {

            for (final String pattern : patternArray) {
                if (anyMatching(stringArray, pattern)) {
                    continue;
                }
                return false;
            }
            return true;
        }

        boolean matchingWith(final String[] stringArray) {

            // 长度不一样就不用不配了
            int length;
            if ((length = GaArrayUtils.getLength(stringArray)) != GaArrayUtils.getLength(patternArray)) {
                return false;
            }
            // 长度相同则逐个位置比较，只要有一个位置不符，则判定不通过
            for (int index = 0; index < length; index++) {
                if (!patternMatching(stringArray[index], patternArray[index], patternType)) {
                    return false;
                }
            }
            // 所有位置匹配通过，判定匹配成功
            return true;
        }

    }

}
