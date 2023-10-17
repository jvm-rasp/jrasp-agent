package com.jrasp.agent.core.env;

public enum ContainerEnum {

    NOT("not int container"),
    DOCKER("in docker"),
    K8S("in k8s"),
    UNKNOWN("unknown container");

    String name;

    ContainerEnum(String name) {
        this.name = name;
    }


}