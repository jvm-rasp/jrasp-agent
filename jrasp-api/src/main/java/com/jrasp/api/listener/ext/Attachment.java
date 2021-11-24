package com.jrasp.api.listener.ext;

public interface Attachment {

    void attach(Object attachment);

    <T> T attachment();

}
