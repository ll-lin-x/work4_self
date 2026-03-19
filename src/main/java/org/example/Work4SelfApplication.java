package org.example;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@SpringBootApplication
@MapperScan("org.example.mapper")
@EnableAsync
@EnableScheduling
@EnableMethodSecurity
public class Work4SelfApplication {

    public static void main(String[] args) {
        SpringApplication.run(Work4SelfApplication.class, args);
    }

}
