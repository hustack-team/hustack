package com.hust.baseweb.applications.programmingcontest.model;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "problem")
@Getter
@Setter
public class IEProblemProperties {

    private Export exportConf;
    private Import importConf;

    @Getter
    @Setter
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Export {
        long maxSizeBytes;
    }

    @Getter
    @Setter
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Import {
        long maxSizeBytes;
        int fileCount;
        long maxSizeUnzip;
    }
}
