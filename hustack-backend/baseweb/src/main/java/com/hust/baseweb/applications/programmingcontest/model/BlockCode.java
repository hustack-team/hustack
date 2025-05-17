package com.hust.baseweb.applications.programmingcontest.model;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@ToString
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BlockCode {
    String id;
    String code;
    boolean forStudent;
    int seq;
    String language;
}
