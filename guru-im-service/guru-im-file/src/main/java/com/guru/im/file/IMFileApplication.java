package com.guru.im.file;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableDiscoveryClient
@MapperScan("com.guru.im.file.mapper")
@ComponentScan("com.guru.im")
public class IMFileApplication {
    public static void main(String[] args) {
        SpringApplication.run(IMFileApplication.class, args);
    }
}
