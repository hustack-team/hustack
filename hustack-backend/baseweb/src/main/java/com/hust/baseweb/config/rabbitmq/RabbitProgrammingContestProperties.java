package com.hust.baseweb.config.rabbitmq;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Validated
@AllArgsConstructor
@ConfigurationProperties(prefix = "spring.rabbitmq.programming-contest")
public class RabbitProgrammingContestProperties {

    @Min(1)
    private int concurrentConsumers;

    @Max(10)
    private int maxConcurrentConsumers;

    @Min(1)
    @Max(2)
    private int prefetchCount;

    @Min(1)
    private int retryLimit;

    @Min(60000)
    private int deadMessageTtl;

}

