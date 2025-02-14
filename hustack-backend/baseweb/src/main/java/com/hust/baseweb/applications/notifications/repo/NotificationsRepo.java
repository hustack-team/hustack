package com.hust.baseweb.applications.notifications.repo;

import com.hust.baseweb.applications.notifications.entity.Notifications;
import com.hust.baseweb.applications.notifications.model.NotificationDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Date;
import java.util.List;
import java.util.UUID;

public interface NotificationsRepo extends JpaRepository<Notifications, UUID> {

    long countByToUserAndStatusId(String toUser, String statusId);

    @Query(value = "select\n" +
                   "\tcast(id as varchar),\n" +
                   "\tcontent,\n" +
                   "\tfrom_user fromUser,\n" +
                   "\turl,\n" +
                   "\tfirst_name firstName,\n" +
                   "\tmiddle_name middleName,\n" +
                   "\tlast_name lastName,\n" +
                   "\tn.status_id statusId,\n" +
                   "\tn.created_stamp createdStamp\n" +
                   "from\n" +
                   "\tnotifications n\n" +
                   "left join user_register ur on\n" +
                   "\tn.from_user = ur.user_login_id where n.to_user = ?1",
           nativeQuery = true)
    Page<NotificationDTO> findAllNotifications(String toUser, Pageable pageable);

    @Query(value = "with cte as (\n" +
                   "select\n" +
                   "\tcreated_stamp\n" +
                   "from\n" +
                   "\tnotifications\n" +
                   "where\n" +
                   "\tid = ?2 )\n" +
                   "select\n" +
                   "\tcast(id as varchar),\n" +
                   "\tcontent,\n" +
                   "\tfrom_user fromUser,\n" +
                   "\turl,\n" +
                   "\tfirst_name firstName,\n" +
                   "\tmiddle_name middleName,\n" +
                   "\tlast_name lastName,\n" +
                   "\tn2.status_id statusId,\n" +
                   "\tn2.created_stamp createdStamp\n" +
                   "from\n" +
                   "\tnotifications n2\n" +
                   "left join user_register ur on\n" +
                   "\tn2.from_user = ur.user_login_id,\n" +
                   "\tcte\n" +
                   "where\n" +
                   "\tn2.to_user = ?1\n" +
                   "\tand n2.created_stamp < cte.created_stamp\n",
           nativeQuery = true,
           countQuery = "with cte as (\n" +
                        "select\n" +
                        "\tcreated_stamp\n" +
                        "from\n" +
                        "\tnotifications\n" +
                        "where\n" +
                        "\tid = ?2)\n" +
                        "select\n" +
                        "\tcount(n2.id)\n" +
                        "from\n" +
                        "\tnotifications n2\n" +
                        "left join user_register ur on\n" +
                        "\tn2.from_user = ur.user_login_id,\n" +
                        "\tcte\n" +
                        "where\n" +
                        "\tn2.to_user = ?1\n" +
                        "\tand n2.created_stamp < cte.created_stamp")
    Page<NotificationDTO> findNotificationsFromId(String toUser, UUID fromId, Pageable pageable);

    @Query(value = "select\n" +
                   "\tcast(id as varchar),\n" +
                   "\tcontent,\n" +
                   "\tfrom_user fromUser,\n" +
                   "\turl,\n" +
                   "\tfirst_name firstName,\n" +
                   "\tmiddle_name middleName,\n" +
                   "\tlast_name lastName,\n" +
                   "\tn.status_id statusId,\n" +
                   "\tn.created_stamp createdStamp\n" +
                   "from\n" +
                   "\tnotifications n\n" +
                   "left join user_register ur on\n" +
                   "\tn.from_user = ur.user_login_id where n.id = ?1",
           nativeQuery = true)
    NotificationDTO findNotificationById(UUID notificationId);

    List<Notifications> findByToUserAndStatusIdAndCreatedStampLessThanEqual(
        String toUser,
        String statusId,
        Date beforeOrAt
    );
}
