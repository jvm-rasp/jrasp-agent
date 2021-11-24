package com.jrasp.api.listener.ext;

import com.jrasp.api.event.Event;
import com.jrasp.api.util.LazyGet;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Advice implements Attachment {

    private final int processId;
    private final int invokeId;

    private final ClassLoader loader;
    private final LazyGet<Behavior> behaviorLazyGet;
    private final Object[] parameterArray;
    private final Object target;

    private Object returnObj;
    private Throwable throwable;

    private Object attachment;
    private Set<String> marks = new HashSet<String>();

    private Advice top = this;
    private Advice parent = this;
    private Event.Type state = Event.Type.BEFORE;

    Advice(final int processId,
           final int invokeId,
           final LazyGet<Behavior> behaviorLazyGet,
           final ClassLoader loader,
           final Object[] parameterArray,
           final Object target) {
        this.processId = processId;
        this.invokeId = invokeId;
        this.behaviorLazyGet = behaviorLazyGet;
        this.loader = loader;
        this.parameterArray = parameterArray;
        this.target = target;
    }

    Advice applyBefore(final Advice top,
                       final Advice parent) {
        this.top = top;
        this.parent = parent;
        return this;
    }

    Advice applyReturn(final Object returnObj) {
        this.returnObj = returnObj;
        this.state = Event.Type.RETURN;
        return this;
    }

    Advice applyThrows(final Throwable throwable) {
        this.throwable = throwable;
        this.state = Event.Type.THROWS;
        return this;
    }

    public boolean isReturn() {
        return this.state == Event.Type.RETURN;
    }

    public boolean isThrows() {
        return this.state == Event.Type.THROWS;
    }

    public Advice changeParameter(final int index,
                                  final Object changeValue) {
        parameterArray[index] = changeValue;
        return this;
    }

    public int getProcessId() {
        return processId;
    }

    public int getInvokeId() {
        return invokeId;
    }

    public Behavior getBehavior() {
        return behaviorLazyGet.get();
    }

    public ClassLoader getLoader() {
        return loader;
    }

    public Object[] getParameterArray() {
        return parameterArray;
    }

    public Object getTarget() {
        return target;
    }

    public Object getReturnObj() {
        return returnObj;
    }

    public Throwable getThrowable() {
        return throwable;
    }

    @Override
    public void attach(final Object attachment) {
        this.attachment = attachment;
    }

    @Override
    public <E> E attachment() {
        return (E) attachment;
    }

    @Override
    public int hashCode() {
        return processId + invokeId;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Advice) {
            final Advice advice = (Advice) obj;
            return processId == advice.processId
                    && invokeId == advice.invokeId;
        } else {
            return false;
        }
    }

    public void mark(final String mark) {
        marks.add(mark);
    }

    public boolean hasMark(final String exceptMark) {
        return marks.contains(exceptMark);
    }

    public boolean unMark(final String mark) {
        return marks.remove(mark);
    }

    public void attach(final Object attachment,
                       final String mark) {
        attach(attachment);
        mark(mark);
    }

    public boolean isProcessTop() {
        return parent == this;
    }

    public Advice getProcessTop() {
        return top;
    }

    public List<Advice> listHasMarkOnChain(final String exceptMark) {
        final List<Advice> advices = new ArrayList<Advice>();
        if (hasMark(exceptMark)) {
            advices.add(this);
        }
        if (!isProcessTop()) {
            advices.addAll(parent.listHasMarkOnChain(exceptMark));
        }
        return advices;
    }

}

