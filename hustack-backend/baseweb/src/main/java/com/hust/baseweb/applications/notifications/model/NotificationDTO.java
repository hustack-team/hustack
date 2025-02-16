package com.hust.baseweb.applications.notifications.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static com.hust.baseweb.applications.notifications.entity.Notifications.STATUS_READ;
import static com.hust.baseweb.utils.DateTimeUtils.ISO_8601_DATE_FORMAT;

/**
 * @author Le Anh Tuan
 */
public interface NotificationDTO {

    String getId();

    String getContent();

    @JsonIgnore
    String getFromUser();

    @JsonIgnore
    String getFirstName();

    @JsonIgnore
    String getMiddleName();

    @JsonIgnore
    String getLastName();

    @JsonIgnore
    String getStatusId();

    String getUrl();

    default boolean getRead() {
        return getStatusId().equals(STATUS_READ);
    }

    Date getCreatedStamp();

    default String getAvatar() {
        String firstName = getFirstName();
        String lastName = getLastName();

        String s1 = StringUtils.isBlank(firstName) ? "" : StringUtils.trim(firstName).substring(0, 1);
        String avatar = s1 + (StringUtils.isBlank(lastName) ? "" : StringUtils.trim(lastName).substring(0, 1));

        return avatar.equals("") ? null : avatar;
    }

    @JsonIgnore
    default String toJson() {
        Map<String, Object> jsonMap = new HashMap<>();

        jsonMap.put("id", getId());
        jsonMap.put("content", getContent());
        jsonMap.put("url", getUrl());
        jsonMap.put("avatar", getAvatar());
        jsonMap.put("read", getRead());
        jsonMap.put("createdStamp", ISO_8601_DATE_FORMAT.format(getCreatedStamp()));

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writeValueAsString(jsonMap);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error converting object to JSON", e);
        }
    }
}
