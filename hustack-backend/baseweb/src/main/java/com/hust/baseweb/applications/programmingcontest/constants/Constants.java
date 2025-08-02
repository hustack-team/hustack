package com.hust.baseweb.applications.programmingcontest.constants;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
public class Constants {

    public static final String SPLIT_TEST_CASE = "testcasedone" + RandomStringUtils.randomAlphabetic(10);

    public static final String SOURCECODE_HEREDOC_DELIMITER = RandomStringUtils.randomAlphabetic(10);

    @Getter
    @RequiredArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    public enum RegistrationType {
        PENDING("PENDING"),
        SUCCESSFUL("SUCCESSFUL"),
        FAILED("FAILED");

        String value;
    }


    @Getter
    @RequiredArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    public enum RegisterCourseStatus {
        SUCCESSES("SUCCESSES"),
        FAILED("FAILED");

        String value;
    }

    @Getter
    @RequiredArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    public enum GetPointForRankingType {
        LATEST("LATEST"),
        HIGHEST("HIGHEST");

        String value;
    }

    @Getter
    @RequiredArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    public enum TestCaseSubmissionError {
        FILE_LIMIT("File size limit exceeded"),
        MEMORY_LIMIT("Segmentation fault"),
        TIME_LIMIT("Killed");

        String value;
    }

    @Getter
    @RequiredArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    public enum DockerImage {
        GCC("gcc:12.3"),
        JAVA("eclipse-temurin:17"),
        PYTHON3("python:3.7-bookworm");

        String value;
    }

    @Getter
    @RequiredArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    public enum DockerContainer {
        GCC("/gcc"),
        JAVA("/java"),
        PYTHON3("/python3");

        String value;
    }

    @Getter
    @RequiredArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    public enum ProblemResultEvaluationType {
        NORMAL("NORMAL_EVALUATION"),
        CUSTOM("CUSTOM_EVALUATION");

        String value;
    }

}
