package com.hust.baseweb.applications.programmingcontest.model;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class MemberDTO {
    private UUID groupId;
    private String userId;
    private String role;
}
