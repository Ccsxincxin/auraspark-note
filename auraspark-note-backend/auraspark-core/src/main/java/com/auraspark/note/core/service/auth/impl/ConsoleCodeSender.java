package com.auraspark.note.core.service.auth.impl;

import com.auraspark.note.core.service.auth.CodeSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean(CodeSender.class)
public class ConsoleCodeSender implements CodeSender {

    private static final Logger log = LoggerFactory.getLogger(ConsoleCodeSender.class);

    @Override
    public void send(String target, String code, String type) {
        log.info("[VERIFICATION CODE] [{}] To: {} Code: {}", type, target, code);
    }
}


