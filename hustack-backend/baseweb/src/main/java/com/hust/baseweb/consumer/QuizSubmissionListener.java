package com.hust.baseweb.consumer;

import com.hust.baseweb.applications.education.quiztest.service.QuizTestService;
import com.hust.baseweb.config.rabbitmq.RabbitProperties;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

import static com.hust.baseweb.config.rabbitmq.QuizRoutingKey.QUIZ_DL;
import static com.hust.baseweb.config.rabbitmq.RabbitConfig.QUIZ_DEAD_LETTER_EXCHANGE;
import static com.hust.baseweb.config.rabbitmq.RabbitConfig.QUIZ_QUEUE;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class QuizSubmissionListener extends BaseRabbitListener {

    QuizTestService quizTestService;

    RabbitProperties rabbitConfig;

    @Override
    @RabbitListener(queues = QUIZ_QUEUE, containerFactory = "quizListenerContainerFactory")
    public void onMessage(
        Message message, String messageBody, Channel channel,
        @Header(required = false, name = "x-delivery-count") Integer deliveryCount
    ) throws Exception {
        if (deliveryCount == null || deliveryCount < rabbitConfig.getQuiz().getRetryLimit()) {
            retryMessage(message, messageBody, channel);
        } else {
            sendMessageToDeadLetterQueue(message, channel);
        }
    }

    @Override
    protected void retryMessage(Message message, String messageBody, Channel channel) throws IOException {
        try {
            UUID quizId = UUID.fromString(messageBody);
            quizTestService.updateFromQuizTestExecutionSubmission(quizId);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            e.printStackTrace();
            channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
        }
    }

    @Override
    protected void sendMessageToDeadLetterQueue(Message message, Channel channel) throws IOException {
        channel.basicPublish(
            QUIZ_DEAD_LETTER_EXCHANGE,
            QUIZ_DL,
            new AMQP.BasicProperties.Builder().deliveryMode(2).build(),
            message.getBody());
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }
}
