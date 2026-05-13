package com.auraspark.note.launcher;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.auraspark.note")
@MapperScan({"com.auraspark.note.core.mapper", "com.auraspark.note.ai.mapper"})
public class AurasparkLauncherApplication {
    public static void main(String[] args) {
        SpringApplication.run(AurasparkLauncherApplication.class, args);
    }
}
