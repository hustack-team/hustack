package com.hust.baseweb.applications.notifications.model;

import java.util.Date;

import static com.hust.baseweb.applications.notifications.entity.Notifications.STATUS_READ;

public interface NotificationProjection {

    String getId();

    String getContent();

    String getFromUser();

    String getFirstName();

//    @JsonIgnore
//    String getMiddleName();

    String getLastName();

    String getStatusId();

    String getUrl();

    default boolean getRead() {
        return getStatusId().equals(STATUS_READ);
    }

    Date getCreatedStamp();
}
