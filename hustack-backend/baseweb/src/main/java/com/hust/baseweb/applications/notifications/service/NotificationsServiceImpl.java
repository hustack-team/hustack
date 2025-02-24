package com.hust.baseweb.applications.notifications.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hust.baseweb.applications.notifications.entity.Notifications;
import com.hust.baseweb.applications.notifications.model.NotificationDTO;
import com.hust.baseweb.applications.notifications.model.NotificationProjection;
import com.hust.baseweb.applications.notifications.repo.NotificationsRepo;
import jakarta.annotation.PostConstruct;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.hust.baseweb.applications.notifications.entity.Notifications.STATUS_CREATED;
import static com.hust.baseweb.applications.notifications.entity.Notifications.STATUS_READ;

@Slf4j
@AllArgsConstructor(onConstructor_ = @Autowired)
@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NotificationsServiceImpl implements NotificationsService {

    ObjectMapper mapper;

    ExecutorService executor = Executors.newSingleThreadExecutor();

    NotificationsRepo notificationsRepo;

    @PostConstruct
    public void init() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            executor.shutdown();
            try {
                executor.awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                log.error(e.toString());
            }
        }));
    }

    @Override
    public Page<NotificationProjection> getNotifications(String toUser, UUID fromId, int page, int size) {
        Pageable sortedByCreatedStampDsc =
            PageRequest.of(page, size, Sort.by("created_stamp").descending());
        Page<NotificationProjection> notifications = fromId == null
            ? notificationsRepo.findAllNotifications(
            toUser,
            sortedByCreatedStampDsc)
            : notificationsRepo.findNotificationsFromId(toUser, fromId, sortedByCreatedStampDsc);

        return notifications;
    }

    @Override
    public long countNumUnreadNotification(String toUser) {
        return notificationsRepo.countByToUserAndStatusId(toUser, STATUS_CREATED);
    }

    @Override
    public void create(String fromUser, String toUser, String content, String url) {
        Notifications notification = new Notifications();

        notification.setFromUser(fromUser);
        notification.setToUser(toUser);
        notification.setContent(content);
        notification.setUrl(url);
        notification.setStatusId(STATUS_CREATED);

        create(notification);
    }

    /**
     * @param notification
     */
    @Override
    public void create(Notifications notification) {
        notification = notificationsRepo.save(notification);
        NotificationDTO dto = mapper.convertValue(
            notificationsRepo.findNotificationById(notification.getId()),
            NotificationDTO.class);
        dispatchNotification(notification.getToUser(), dto);
    }

    /**
     * @param notification
     */
    @Override
    public void createEphemeralNotification(Notifications notification) {
        NotificationDTO dto = mapper.convertValue(notification, NotificationDTO.class);
        dispatchNotification(notification.getToUser(), dto);
    }

    private void dispatchNotification(String toUser, NotificationDTO dto) {
        List<SseEmitter> subscription = subscriptions.get(toUser);
        if (null != subscription) {
            send(
                subscription,
                SseEmitter.event()
                          .id(dto.getId())
                          .name(SSE_EVENT_NEW_NOTIFICATION)
                          .data(dto.toJson(), MediaType.TEXT_EVENT_STREAM)
                // TODO: discover reconnectTime() method
            );
        }
    }

    private void send(List<SseEmitter> subscriptions, SseEmitter.SseEventBuilder event) {
        executor.execute(() -> subscriptions.forEach(subscription -> {
                                                         try {
                                                             subscription.send(event);
                                                         } catch (Exception ignore) {
                                                             // This is normal behavior when a client disconnects.
                                                             // onError callback will be automatically fired.
//                                                             log.error(
//                                                                 "Failed to send event because of error: {}",
//                                                                 ignore.getMessage());
//                                                             try {
//                                                                 subscription.completeWithError(ignore);
//                                                             } catch (Exception completionException) {
//                                                                 log.error(
//                                                                     "Error occurred when attempting to mark SseEmitter as complete: {}",
//                                                                     completionException.getMessage());
//                                                             }
                                                         }
                                                     }
        ));
    }


    @Override
    public void updateStatus(UUID notificationId, String status) {
        Notifications notification = notificationsRepo.findById(notificationId).orElse(null);

        if (null != notification) {
            notification.setStatusId(STATUS_READ);
            notificationsRepo.save(notification);
        }
    }

    @Override
    public void updateMultipleNotificationsStatus(
        String userId,
        String status,
        Date beforeOrAt
    ) {
        List<Notifications> notifications = notificationsRepo.findByToUserAndStatusIdAndCreatedStampLessThanEqual(
            userId,
            STATUS_CREATED,
            beforeOrAt);

        // TODO: upgrade this method to check valid status according to notification status transition.
        if (!notifications.isEmpty()) {
            for (Notifications notification : notifications) {
                notification.setStatusId(status);
            }

            notificationsRepo.saveAll(notifications);
        }
    }
}
