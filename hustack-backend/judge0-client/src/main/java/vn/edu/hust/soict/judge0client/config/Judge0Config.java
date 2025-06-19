package vn.edu.hust.soict.judge0client.config;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

import java.util.Set;

@Getter
@AllArgsConstructor
@ConstructorBinding
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@ConfigurationProperties(prefix = "judge0.servers")
public class Judge0Config {

    ServerConfig singleThreaded;

    ServerConfig multiThreaded;

    @Getter
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    public static class ServerConfig {

        String uri;

        Auth authn;

        Auth authz;

        Submission submission;

        Set<String> multiThreadedLibs;
    }

    @Getter
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    public static class Auth {

        String header;

        String token;
    }

    @Getter
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    public static class Submission {

        Float maxCpuTimeLimit;

        Float maxCpuExtraTime;

        Float maxWallTimeLimit;

        Integer maxMemoryLimit;

        Integer maxStackLimit;

        Integer maxMaxFileSize;

        Integer javaMaxProcessesAndOrThreads;

        String javaCommandLineArguments;
    }
}
