package com.hust.baseweb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@ConfigurationPropertiesScan
@EnableAsync
@EnableScheduling
@EnableCaching
@EnableMethodSecurity
@SpringBootApplication(scanBasePackages = {"com.hust.baseweb", "vn.edu.hust.soict.judge0client", "ai.soict.hustack.authzresourceserver"})
public class HustackApplication {

    public static void main(String[] args) {
        SpringApplication.run(HustackApplication.class, args);
    }
}
