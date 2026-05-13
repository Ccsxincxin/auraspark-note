package com.auraspark.note.core.service.auth;

public interface CodeSender {
    void send(String target, String code, String type);
}

