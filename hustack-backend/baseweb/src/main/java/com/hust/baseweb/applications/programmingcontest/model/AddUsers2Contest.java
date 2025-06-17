package com.hust.baseweb.applications.programmingcontest.model;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AddUsers2Contest {

    List<String> userIds;

    List<UUID> groupIds;

    String role;
}
