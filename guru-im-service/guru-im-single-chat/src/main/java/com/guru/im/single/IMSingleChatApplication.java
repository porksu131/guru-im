package com.guru.im.single;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@MapperScan("com.guru.im.single.mapper")
@ComponentScan("com.guru.im")
@EnableDiscoveryClient
public class IMSingleChatApplication {
    public static void main(String[] args) {
        SpringApplication.run(IMSingleChatApplication.class, args);
    }
}
