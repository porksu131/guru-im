package com.guru.im.user;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@MapperScan("com.guru.im.user.mapper")
@ComponentScan("com.guru.im")
@EnableDiscoveryClient
public class IMUserApplication {
    public static void main(String[] args) {
        SpringApplication.run(IMUserApplication.class, args);
    }
}
