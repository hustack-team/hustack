package com.hust.baseweb.config;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@AllArgsConstructor
@ConfigurationProperties(prefix = "problem")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class IEProblemProperties {

    Export exportConf;
    Import importConf;

    @Getter
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    public static class Export {
        long maxSizeBytes;
    }

    @Getter
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    public static class Import {
        long maxSizeBytes;
        int fileCount;
    }
}
