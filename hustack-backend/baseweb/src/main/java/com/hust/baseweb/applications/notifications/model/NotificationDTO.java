package com.hust.baseweb.applications.notifications.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hust.baseweb.applications.notifications.entity.NotificationType;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.apache.commons.lang3.StringUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static com.hust.baseweb.applications.notifications.entity.Notifications.STATUS_READ;
import static com.hust.baseweb.utils.DateTimeUtils.ISO_8601_DATE_FORMAT;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NotificationDTO {

    String id;

    NotificationType type;

    String content;

    @JsonIgnore
    String fromUser;

    @JsonIgnore
    String firstName;

//    @JsonIgnore
//    String middleName;

    @JsonIgnore
    String lastName;

    @JsonIgnore
    String statusId;

    String url;

    boolean read;

    public String getAvatar() {
        String firstName = getFirstName();
        String lastName = getLastName();

        String s1 = StringUtils.isBlank(firstName) ? "" : StringUtils.trim(firstName).substring(0, 1);
        String avatar = s1 + (StringUtils.isBlank(lastName) ? "" : StringUtils.trim(lastName).substring(0, 1));

        return avatar.isEmpty() ? null : avatar;
    }

    Date createdStamp;

    String avatar;

    public boolean isRead() {
        return STATUS_READ.equals(getStatusId());
    }

    @JsonIgnore
    public String toJson() {
        Map<String, Object> jsonMap = new HashMap<>();

        jsonMap.put("id", getId());
        jsonMap.put("type", getType());
        jsonMap.put("content", getContent());
        jsonMap.put("url", getUrl());
        jsonMap.put("avatar", getAvatar());
        jsonMap.put("read", isRead());
        jsonMap.put("createdStamp", ISO_8601_DATE_FORMAT.format(getCreatedStamp()));

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writeValueAsString(jsonMap);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error converting object to JSON", e);
        }
    }
}
