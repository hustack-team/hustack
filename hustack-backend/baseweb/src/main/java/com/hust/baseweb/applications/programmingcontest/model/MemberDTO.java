package com.hust.baseweb.applications.programmingcontest.model;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MemberDTO {
    UUID groupId;
    String userId;
    String fullName;
    LocalDateTime addedTime;

}
