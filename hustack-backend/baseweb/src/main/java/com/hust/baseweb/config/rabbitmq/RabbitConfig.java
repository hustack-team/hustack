package com.hust.baseweb.config.rabbitmq;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.util.ISO8601DateFormat;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "programming_contest_exchange";

    public static final String QUIZ_EXCHANGE = "quiz_exchange";

    public static final String DEAD_LETTER_EXCHANGE = "programming_contest_dead_letter_exchange";

    public static final String QUIZ_DEAD_LETTER_EXCHANGE = "quiz_dead_letter_exchange";

    public static final String JUDGE_PROBLEM_QUEUE = "judge_problem_queue";

    public static final String JUDGE_PROBLEM_DEAD_LETTER_QUEUE = "judge_problem_dead_letter_queue";

    public static final String JUDGE_CUSTOM_PROBLEM_QUEUE = "judge_custom_problem_queue";

    public static final String JUDGE_CUSTOM_PROBLEM_DEAD_LETTER_QUEUE = "judge_custom_problem_dead_letter_queue";

    public static final String QUIZ_QUEUE = "quiz_queue";

    public static final String QUIZ_DEAD_LETTER_QUEUE = "quiz_dead_letter_queue";

    public static final String NOTIFICATION_QUEUE = "notification_queue";

    public static final String NOTIFICATION_DEAD_LETTER_QUEUE = "notification_dead_letter_queue";

    @Autowired
    private RabbitProperties rabbitConfig;

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);

        return rabbitTemplate;
    }

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }


    @Bean
    public MessageConverter jsonMessageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
        objectMapper.setDateFormat(new ISO8601DateFormat());

        return new Jackson2JsonMessageConverter(objectMapper);
    }

    // Configuration setting: https://docs.spring.io/spring-amqp/docs/current/reference/html/#containerAttributes
    @Bean
    public SimpleRabbitListenerContainerFactory quizListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();

        factory.setAutoStartup(rabbitConfig.getQuiz().isAutoStartup());
        factory.setConnectionFactory(connectionFactory);
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setMessageConverter(jsonMessageConverter());
        factory.setConcurrentConsumers(rabbitConfig.getQuiz().getConcurrentConsumers());
        factory.setMaxConcurrentConsumers(rabbitConfig.getQuiz().getMaxConcurrentConsumers());
        factory.setPrefetchCount(rabbitConfig.getQuiz().getPrefetchCount());
        // factory.setChannelTransacted(true); //try if there are faults, but this will
        // slow down the process

        return factory;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory notificationListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();

        factory.setAutoStartup(rabbitConfig.getNotification().isAutoStartup());
        factory.setConnectionFactory(connectionFactory);
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setMessageConverter(jsonMessageConverter());
        factory.setConcurrentConsumers(rabbitConfig.getNotification().getConcurrentConsumers());
        factory.setMaxConcurrentConsumers(rabbitConfig.getNotification().getMaxConcurrentConsumers());
        factory.setPrefetchCount(rabbitConfig.getNotification().getPrefetchCount());
        // factory.setChannelTransacted(true); //try if there are faults, but this will
        // slow down the process

        return factory;
    }

    @Bean
    public DirectExchange exchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue judgeProblemQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-queue-type", "quorum");
        args.put("x-overflow", "reject-publish");

        return new Queue(JUDGE_PROBLEM_QUEUE, true, false, false, args);
    }

    @Bean
    public Binding judgeProblemBinding() {
        return BindingBuilder
            .bind(judgeProblemQueue())
            .to(exchange())
            .with(RabbitRoutingKey.JUDGE_PROBLEM);
    }

    @Bean
    public Queue judgeCustomProblemQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-queue-type", "quorum");
        args.put("x-overflow", "reject-publish");

        return new Queue(JUDGE_CUSTOM_PROBLEM_QUEUE, true, false, false, args);
    }

    @Bean
    public Binding judgeCustomProblemBinding() {
        return BindingBuilder
            .bind(judgeCustomProblemQueue())
            .to(exchange())
            .with(RabbitRoutingKey.JUDGE_CUSTOM_PROBLEM);
    }

    @Bean
    public Queue notificationQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-queue-type", "quorum");
        args.put("x-overflow", "reject-publish");

        return new Queue(NOTIFICATION_QUEUE, true, false, false, args);
    }

    @Bean
    public Binding notificationBinding() {
        return BindingBuilder
            .bind(notificationQueue())
            .to(exchange())
            .with(RabbitRoutingKey.NOTIFICATION);
    }

    // DeadLetterExchange & DeadLetterQueue
    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DEAD_LETTER_EXCHANGE, true, false);
    }

    @Bean
    public Queue judgeProblemDeadLetterQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-queue-type", "quorum");
        args.put("x-dead-letter-exchange", EXCHANGE);
        args.put("x-dead-letter-routing-key", RabbitRoutingKey.JUDGE_PROBLEM);
        args.put("x-message-ttl", rabbitConfig.getQuiz().getDeadMessageTtl());

        return new Queue(JUDGE_PROBLEM_DEAD_LETTER_QUEUE, true, false, false, args);
    }

    @Bean
    public Binding judgeProblemDeadLetterBinding() {
        return BindingBuilder
            .bind(judgeProblemDeadLetterQueue())
            .to(deadLetterExchange())
            .with(RabbitRoutingKey.JUDGE_PROBLEM_DL);
    }

    @Bean
    public Queue judgeCustomProblemDeadLetterQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-queue-type", "quorum");
        args.put("x-dead-letter-exchange", EXCHANGE);
        args.put("x-dead-letter-routing-key", RabbitRoutingKey.JUDGE_CUSTOM_PROBLEM);
        args.put("x-message-ttl", rabbitConfig.getQuiz().getDeadMessageTtl());

        return new Queue(JUDGE_CUSTOM_PROBLEM_DEAD_LETTER_QUEUE, true, false, false, args);
    }

    @Bean
    public Binding judgeCustomProblemDeadLetterBinding() {
        return BindingBuilder
            .bind(judgeCustomProblemDeadLetterQueue())
            .to(deadLetterExchange())
            .with(RabbitRoutingKey.JUDGE_CUSTOM_PROBLEM_DL);
    }

    @Bean
    public Queue notificationDeadLetterQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-queue-type", "quorum");
        args.put("x-dead-letter-exchange", EXCHANGE);
        args.put("x-dead-letter-routing-key", RabbitRoutingKey.NOTIFICATION);
        args.put("x-message-ttl", rabbitConfig.getNotification().getDeadMessageTtl());

        return new Queue(NOTIFICATION_DEAD_LETTER_QUEUE, true, false, false, args);
    }

    @Bean
    public Binding notificationDeadLetterBinding() {
        return BindingBuilder
            .bind(notificationQueue())
            .to(deadLetterExchange())
            .with(RabbitRoutingKey.NOTIFICATION_DL);
    }

    @Bean
    public Queue quizQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-queue-type", "quorum");
        args.put("x-overflow", "reject-publish");

        return new Queue(QUIZ_QUEUE, true, false, false, args);
    }

    @Bean
    public DirectExchange quizExchange() {
        return new DirectExchange(QUIZ_EXCHANGE, true, false);
    }

    @Bean
    public Binding judgeQuizBinding() {
        return BindingBuilder
            .bind(quizQueue())
            .to(quizExchange())
            .with(QuizRoutingKey.QUIZ);
    }

    @Bean
    public Queue judgeQuizDeadLetterQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-queue-type", "quorum");
        args.put("x-dead-letter-exchange", EXCHANGE);
        args.put("x-dead-letter-routing-key", QuizRoutingKey.QUIZ_DL);
        args.put("x-message-ttl", rabbitConfig.getQuiz().getDeadMessageTtl());

        return new Queue(QUIZ_DEAD_LETTER_QUEUE, true, false, false, args);
    }

    @Bean
    public DirectExchange quizDeadLetterExchange() {
        return new DirectExchange(QUIZ_DEAD_LETTER_EXCHANGE, true, false);
    }

    @Bean
    public Binding judgeQuizDeadLetterBinding() {
        return BindingBuilder
            .bind(judgeQuizDeadLetterQueue())
            .to(quizDeadLetterExchange())
            .with(QuizRoutingKey.QUIZ_DL);
    }
}
