package com.jrasp.api.model;

public class ModuleInfo {
    private String id;
    private boolean isActivated;
    private boolean isLoaded;
    private int classCnt;
    private int methodCnt;
    private String version;
    private String author;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean isActivated() {
        return isActivated;
    }

    public void setActivated(boolean activated) {
        isActivated = activated;
    }

    public boolean isLoaded() {
        return isLoaded;
    }

    public void setLoaded(boolean loaded) {
        isLoaded = loaded;
    }

    public int getClassCnt() {
        return classCnt;
    }

    public void setClassCnt(int classCnt) {
        this.classCnt = classCnt;
    }

    public int getMethodCnt() {
        return methodCnt;
    }

    public void setMethodCnt(int methodCnt) {
        this.methodCnt = methodCnt;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public ModuleInfo() {
    }

    public ModuleInfo(String id, boolean isActivated, boolean isLoaded, int classCnt, int methodCnt, String version) {
        this.id = id;
        this.isActivated = isActivated;
        this.isLoaded = isLoaded;
        this.classCnt = classCnt;
        this.methodCnt = methodCnt;
        this.version = version;
    }

    public ModuleInfo(String id, boolean isActivated, boolean isLoaded, int classCnt, int methodCnt) {
        this.id = id;
        this.isActivated = isActivated;
        this.isLoaded = isLoaded;
        this.classCnt = classCnt;
        this.methodCnt = methodCnt;
    }
}
