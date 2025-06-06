package com.hust.baseweb.applications.notifications.repo;

import com.hust.baseweb.applications.notifications.entity.Notifications;
import com.hust.baseweb.applications.notifications.model.NotificationProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Date;
import java.util.List;
import java.util.UUID;

public interface NotificationsRepo extends JpaRepository<Notifications, UUID> {

    long countByToUserAndStatusId(String toUser, String statusId);

    @Query(value = "select " +
                   "    cast(id as varchar), " +
                   "    content, " +
                   "    from_user fromUser, " +
                   "    url, " +
                   "    first_name firstName, " +
                   "    middle_name middleName, " +
                   "    last_name lastName, " +
                   "    n.status_id statusId, " +
                   "    n.created_stamp createdStamp " +
                   "from " +
                   "    notifications n " +
                   "left join user_register ur on " +
                   "    n.from_user = ur.user_login_id where n.to_user = ?1",
           nativeQuery = true)
    Page<NotificationProjection> findAllNotifications(String toUser, Pageable pageable);

    @Query(value = "with cte as ( " +
                   "select " +
                   "    created_stamp " +
                   "from " +
                   "    notifications " +
                   "where " +
                   "    id = ?2 ) " +
                   "select " +
                   "    cast(id as varchar), " +
                   "    content, " +
                   "    from_user fromUser, " +
                   "    url, " +
                   "    first_name firstName, " +
                   "    middle_name middleName, " +
                   "    last_name lastName, " +
                   "    n2.status_id statusId, " +
                   "    n2.created_stamp createdStamp " +
                   "from " +
                   "    notifications n2 " +
                   "left join user_register ur on " +
                   "    n2.from_user = ur.user_login_id, " +
                   "    cte " +
                   "where " +
                   "    n2.to_user = ?1 " +
                   "    and n2.created_stamp < cte.created_stamp ",
           nativeQuery = true,
           countQuery = "with cte as ( " +
                        "select " +
                        "    created_stamp " +
                        "from " +
                        "    notifications " +
                        "where " +
                        "    id = ?2) " +
                        "select " +
                        "    count(n2.id) " +
                        "from " +
                        "    notifications n2 " +
                        "left join user_register ur on " +
                        "    n2.from_user = ur.user_login_id, " +
                        "    cte " +
                        "where " +
                        "    n2.to_user = ?1 " +
                        "    and n2.created_stamp < cte.created_stamp")
    Page<NotificationProjection> findNotificationsFromId(String toUser, UUID fromId, Pageable pageable);

    @Query(value = "select " +
                   "    cast(id as varchar), " +
                   "    content, " +
                   "    from_user fromUser, " +
                   "    url, " +
                   "    first_name firstName, " +
                   "    last_name lastName, " +
                   "    n.status_id statusId, " +
                   "    n.created_stamp createdStamp " +
                   "from " +
                   "    notifications n " +
                   "left join user_login ul on " +
                   "    n.from_user = ul.user_login_id where n.id = ?1",
           nativeQuery = true)
    NotificationProjection findNotificationById(UUID notificationId);

    List<Notifications> findByToUserAndStatusIdAndCreatedStampLessThanEqual(
        String toUser,
        String statusId,
        Date beforeOrAt
    );
}
