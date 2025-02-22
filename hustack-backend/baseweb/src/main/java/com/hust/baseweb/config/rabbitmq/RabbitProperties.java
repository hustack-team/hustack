package com.hust.baseweb.config.rabbitmq;

import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Validated
@AllArgsConstructor
@ConfigurationProperties(prefix = "spring.rabbitmq.programming-contest")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RabbitProperties {

    QueueConfig quiz;

    QueueConfig notification;

    @PostConstruct
    private void postConstruct() {
        if (this.quiz == null) {
            this.quiz = new QueueConfig(1, 2, 1, 2, 30_000, true);
        }

        if (this.notification == null) {
            this.notification = new QueueConfig(1, 2, 1, 2, 30_000, true);
        }
    }

    @Getter
    @Validated
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    public static class QueueConfig {

        @Min(1)
//        @Max(15)
        int concurrentConsumers;

        @Min(1)
//        @Max(15)
        int maxConcurrentConsumers;

        @Min(1)
        @Max(2)
        int prefetchCount;

        @Min(1)
        int retryLimit;

        @Min(30000)
        int deadMessageTtl;

        boolean autoStartup;
    }

}

