package com.hust.baseweb.applications.education.config;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@AllArgsConstructor
@ConfigurationProperties(prefix = "")
public class EducationConfigProperties {

    @NotBlank
    private String url_root;
}
