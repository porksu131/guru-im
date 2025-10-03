package com.guru.im.offline;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableDiscoveryClient
@MapperScan("com.guru.im.offline.mapper")
@ComponentScan("com.guru.im")
public class IMOfflineApplication {
    public static void main(String[] args) {
        SpringApplication.run(IMOfflineApplication.class, args);
    }
}
