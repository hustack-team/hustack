package com.hust.baseweb.config;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@AllArgsConstructor
@ConfigurationProperties(prefix = "content.fs")
public class FileSystemStorageProperties {

    @NotBlank
    private String filesystemRoot;

    @NotBlank
    private String videoPath;

    @NotBlank
    private String classManagementDataPath;

    @NotBlank
    private String backlogDataPath;
}
