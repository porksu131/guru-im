package com.guru.im.signal;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@MapperScan("com.guru.im.signal.mapper")
@ComponentScan("com.guru.im")
@EnableDiscoveryClient
public class IMSignalApplication {
    public static void main(String[] args) {
        SpringApplication.run(IMSignalApplication.class, args);
    }
}

