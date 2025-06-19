package com.hust.baseweb.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@AllArgsConstructor
@ConfigurationProperties(prefix = "spring.mail")
public class MailProperties {

    //    @NotBlank
    private String username;
}
