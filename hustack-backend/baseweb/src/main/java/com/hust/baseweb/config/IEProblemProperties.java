package com.hust.baseweb.config;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@AllArgsConstructor
@ConfigurationProperties(prefix = "problem")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class IEProblemProperties {

    Export exportConfig;
    Import importConfig;

    @Getter
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Export {
        long sizeWarningThreshold;
    }

    @Getter
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Import {
        long maxFileSize;
        int maxFileCount;
    }
}
