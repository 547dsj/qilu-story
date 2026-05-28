package com.qilu.story;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 歧路·互动小说平台 - 主应用类
 */
@EnableAsync
@SpringBootApplication
public class QiluStoryApplication {

    public static void main(String[] args) {
        SpringApplication.run(QiluStoryApplication.class, args);
    }
}