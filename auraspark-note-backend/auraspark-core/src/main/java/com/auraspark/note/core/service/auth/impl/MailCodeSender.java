package com.auraspark.note.core.service.auth.impl;

import com.auraspark.note.core.service.auth.CodeSender;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
@ConditionalOnProperty("spring.mail.host")
public class MailCodeSender implements CodeSender {

    private static final Logger log = LoggerFactory.getLogger(MailCodeSender.class);

    private static final Map<String, String> TYPE_LABELS = Map.of(
            "REGISTER", "注册",
            "LOGIN", "登录",
            "RESET_PASSWORD", "重置密码"
    );

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final String template;

    public MailCodeSender(JavaMailSender mailSender,
                          @Value("${spring.mail.username}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
        this.template = loadTemplate();
    }

    @Override
    public void send(String target, String code, String type) {
        try {
            String label = TYPE_LABELS.getOrDefault(type, type);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(target);
            helper.setSubject("【Auraspark 微光】" + label + "验证码");
            helper.setText(template
                    .replace("{code}", code)
                    .replace("{type}", label), true);
            mailSender.send(message);
            log.info("Verification code [{}] sent to {}", type, target);
        } catch (MessagingException e) {
            log.error("Failed to send verification code to {}: {}", target, e.getMessage());
            throw new RuntimeException("Failed to send email", e);
        }
    }

    private static String loadTemplate() {
        try {
            return new String(
                    new ClassPathResource("templates/mail-code.html").getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load mail template", e);
        }
    }
}
