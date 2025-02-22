package com.hust.baseweb.consumer;

import com.hust.baseweb.applications.notifications.entity.NotificationType;
import com.hust.baseweb.applications.notifications.entity.Notifications;
import com.hust.baseweb.applications.notifications.service.NotificationsService;
import com.hust.baseweb.config.rabbitmq.RabbitConfig;
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

import static com.hust.baseweb.config.rabbitmq.RabbitConfig.NOTIFICATION_QUEUE;
import static com.hust.baseweb.config.rabbitmq.RabbitRoutingKey.NOTIFICATION_DL;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NotificationListener {

    NotificationsService notificationsService;

    RabbitProperties rabbitConfig;

    @RabbitListener(queues = NOTIFICATION_QUEUE, containerFactory = "notificationListenerContainerFactory")
    public void onMessage(
        Message message, Notifications messageBody, Channel channel,
        @Header(required = false, name = "x-delivery-count") Integer deliveryCount
    ) throws Exception {
        if (deliveryCount == null || deliveryCount < rabbitConfig.getNotification().getRetryLimit()) {
            retryMessage(message, messageBody, channel);
        } else {
            sendMessageToDeadLetterQueue(message, channel);
        }
    }

    protected void retryMessage(Message message, Notifications messageBody, Channel channel) throws IOException {
        try {
            if (messageBody.getType() == NotificationType.EPHEMERAL) {
                notificationsService.createEphemeralNotification(messageBody);
            } else if (messageBody.getType() == NotificationType.PERSISTENT) {
                notificationsService.create(messageBody);
            }

            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            e.printStackTrace();
            channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
        }
    }

    protected void sendMessageToDeadLetterQueue(Message message, Channel channel) throws IOException {
        channel.basicPublish(
            RabbitConfig.DEAD_LETTER_EXCHANGE,
            NOTIFICATION_DL,
            new AMQP.BasicProperties.Builder().deliveryMode(2).build(),
            message.getBody());
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }
}
